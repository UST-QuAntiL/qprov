/*******************************************************************************
 * Copyright (c) 2020 the QProv contributors.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.quantil.qprov.collector.providers;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.quantil.qprov.collector.IProvider;
import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.model.entities.QubitCharacteristics;
import org.quantil.qprov.core.repositories.GateRepository;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.quantil.qprov.core.repositories.QubitCharacteristicsRepository;
import org.quantil.qprov.core.repositories.QubitRepository;
import org.quantil.qprov.ibmq.client.ApiClient;
import org.quantil.qprov.ibmq.client.ApiException;
import org.quantil.qprov.ibmq.client.Configuration;
import org.quantil.qprov.ibmq.client.api.GetBackendInformationApi;
import org.quantil.qprov.ibmq.client.api.LoginApi;
import org.quantil.qprov.ibmq.client.auth.ApiKeyAuth;
import org.quantil.qprov.ibmq.client.model.AccessToken;
import org.quantil.qprov.ibmq.client.model.ApiToken;
import org.quantil.qprov.ibmq.client.model.Device;
import org.quantil.qprov.ibmq.client.model.DeviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class IBMQProvider implements IProvider {

    public static final String PROVIDER_ID = "ibmq";

    public static final String PROVIDER_URL = "https://quantum-computing.ibm.com/";

    public static final String IBMQ_DEFAULT_HUB = "ibm-q";

    public static final String IBMQ_DEFAULT_GROUP = "open";

    public static final String IBMQ_DEFAULT_PROJECT = "main";

    private static final Logger logger = LoggerFactory.getLogger(IBMQProvider.class);

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    private final QubitCharacteristicsRepository qubitCharacteristicsRepository;

    private final GateRepository gateRepository;

    private final String ibmqToken;

    private ApiClient defaultClient;

    public IBMQProvider(ProviderRepository providerRepository, QPURepository qpuRepository,
                        QubitRepository qubitRepository,
                        QubitCharacteristicsRepository qubitCharacteristicsRepository,
                        GateRepository gateRepository, Environment env) {
        this.providerRepository = providerRepository;
        this.qpuRepository = qpuRepository;
        this.qubitRepository = qubitRepository;
        this.qubitCharacteristicsRepository = qubitCharacteristicsRepository;
        this.gateRepository = gateRepository;
        this.ibmqToken = env.getProperty("QPROV_IBMQ_TOKEN");
        this.defaultClient = Configuration.getDefaultApiClient();
        this.defaultClient.setBasePath("https://api.quantum-computing.ibm.com/v2");
    }

    /**
     * Authenticate at IBMQ using the token provided through the environment variables
     *
     * @return <code>true</code> if authentication is successful, <code>false</code> otherwise
     */
    private boolean authenticate() {

        // abort authentication if token is not provided
        if (Objects.isNull(ibmqToken)) {
            logger.error("No api token provided!");
            return false;
        }

        final ApiToken apiToken = new ApiToken();
        apiToken.setApiToken(ibmqToken);

        try {
            // get a short-lived access token with the api token
            logger.debug("IBMQProvider try to get an accessToken via supplied apiToken!");
            final AccessToken accessToken = new LoginApi(this.defaultClient).loginLoginWithApiToken(apiToken);
            logger.debug("IBMQProvider successfully got an accessToken!");

            // configure "API Token" authorization with obtained accessToken
            final ApiKeyAuth apiKeyAuth = (ApiKeyAuth) this.defaultClient.getAuthentication("API Token");
            apiKeyAuth.setApiKey(accessToken.getId());
            return true;
        } catch (ApiException e) {
            logger.error("Error while authenticating at IBMQ: {}", e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Check if IBMQ provider already exists in the database and return it or otherwise create it
     *
     * @return the retrieved or created IBMQ provider object
     */
    private Provider addProviderToDatabase() {
        final Optional<Provider> providerOptional = providerRepository.findByName(PROVIDER_ID);
        if (providerOptional.isPresent()) {
            logger.debug("Provider already present, skipping creation.");
            return providerOptional.get();
        }

        // create a new Provider object representing the IBMQ provider that is handled by this collector
        final Provider provider = new Provider();
        provider.setName(PROVIDER_ID);
        try {
            provider.setOfferingURL(new URL(PROVIDER_URL));
        } catch (MalformedURLException e) {
            logger.error("Unable to add provider URL due to MalformedURLException!");
        }
        providerRepository.save(provider);

        return provider;
    }

    /**
     * Add the given device as a QPU to the database or update the information of the already stored QPU object
     *
     * @param provider the provider the QPU belongs to
     * @param device   the IBMQ device to store or update the QPu object for
     * @return the newly created or updated QPU object
     */
    private QPU addQPUToDatabase(Provider provider, Device device) {
        final Optional<QPU> qpuOptional = qpuRepository.findByName(device.getBackendName());
        if (qpuOptional.isPresent()) {
            logger.debug("QPU already present, updating information.");
            QPU qpu = qpuOptional.get();
            qpu.setVersion(device.getBackendVersion());
            qpu.setMaxShots(device.getMaxShots().intValue());
            qpu = qpuRepository.save(qpu);
            return qpu;
        }

        // create a new QPU object representing the retrieved device
        QPU qpu = new QPU();
        qpu.setName(device.getBackendName());
        qpu.setProvider(provider);
        qpu.setVersion(device.getBackendVersion());
        qpu.setMaxShots(device.getMaxShots().intValue());
        qpu.setSimulator(Objects.nonNull(device.getSimulator()) && device.getSimulator());
        qpu = qpuRepository.save(qpu);

        // add qubits
        final Map<String, Qubit> qubits = new HashMap<>();
        if (Objects.nonNull(device.getCouplingMap())) {
            for (List<BigDecimal> coupling : device.getCouplingMap()) {
                final List<Qubit> alreadyAdded = new ArrayList<>();
                for (BigDecimal qubitId : coupling) {
                    final String qubitName = qubitId.toString();

                    // create new qubit if not already done
                    Qubit qubit = qubits.get(qubitName);
                    if (Objects.isNull(qubit)) {
                        qubit = new Qubit();
                        qubit.setQpu(qpu);
                        qubit.setName(qubitName);
                        qubit = qubitRepository.save(qubit);

                        qubits.put(qubitName, qubit);
                    }

                    // connect qubits within the coupling
                    for (Qubit toConnect : alreadyAdded) {
                        toConnect.getConnectedQubits().add(qubit);
                        qubit.getConnectedQubits().add(toConnect);
                    }
                    alreadyAdded.add(qubit);
                }
            }
            qpu.getQubits().addAll(qubits.values());
        } else {
            // for simulators and QPUs with one qubit no coupling map exists, therefore just add the qubits
            for (int i = 0; i < device.getnQubits().intValue(); i++) {
                final Qubit qubit = new Qubit();
                qubit.setQpu(qpu);
                qubit.setName(String.valueOf(i));
                qpu.getQubits().add(qubit);

                qubits.put(qubit.getName(), qubit);
            }
        }

        // add gates to the qubits on which they can be executed
        if (Objects.nonNull(device.getSimulator()) && !device.getSimulator() && Objects.nonNull(device.getGates())) {
            for (org.quantil.qprov.ibmq.client.model.Gate ibmGate : device.getGates()) {
                addGateFromDevice(ibmGate, qpu, qubits);
            }
        }

        qpu = qpuRepository.save(qpu);
        return qpu;
    }

    /**
     * Add the gate of the device to the corresponding qubits
     *
     * @param ibmGate the gate to add to the different qubits that can execute it
     * @param qpu     the qpu to which the qubits belong
     * @param qubits  a mapping from qubit names to the corresponding qubit objects
     */
    private void addGateFromDevice(org.quantil.qprov.ibmq.client.model.Gate ibmGate, QPU qpu, Map<String, Qubit> qubits) {

        // remove duplicates in coupling map
        final List<List<BigDecimal>> distinctList =
                ibmGate.getCouplingMap().stream().map(listToSort -> listToSort.stream().sorted().collect(Collectors.toList())).distinct()
                        .collect(Collectors.toList());

        // each gate is instantiated for each coupling map, as the gate on different qubits has different characteristics
        for (List<BigDecimal> coupling : distinctList) {
            final Gate gate = new Gate();
            gate.setName(ibmGate.getName());
            gate.setQpu(qpu);
            gateRepository.save(gate);

            // add gate to each qubit in the coupling if it operates on multiple qubits
            final Set<Qubit> operatingQubits = new HashSet<>();
            for (BigDecimal qubitId : coupling) {
                final Qubit qubit = qubits.get(qubitId.toString());
                qubit.getSupportedGates().add(gate);
                operatingQubits.add(qubit);
            }
            gate.setOperatingQubits(operatingQubits);
            gateRepository.save(gate);
        }
    }

    /**
     * Parse the objects returned by IBM to a Java Map
     *
     * @param propertiesList the propertiesList as provided by the IBM API
     * @return the Map containing the data from the provided properties list
     */
    private Map<String, String> transformIbmPropertiesToMap(Object propertiesList) {
        final String[] propertiesArray = propertiesList.toString()
                .replaceAll("\\s+", "")
                .replaceAll("\\{", "")
                .replaceAll("}", "")
                .split(",");

        final Map<String, String> map = new HashMap<>();
        for (String property : propertiesArray) {
            final String[] propertyParts = property.split("=");
            if (propertyParts.length != 2) {
                continue;
            }
            map.put(propertyParts[0], propertyParts[1]);
        }
        return map;
    }

    /**
     * Update the qubit characteristics of the given QPU with the latest calibration data and add to the database
     *
     * @param qpu              the QPU to update the qubit characteristics for
     * @param deviceProperties the device properties retrieved from the IBM API
     * @param calibrationTime  the time of the calibration the given device properties were retrieved from
     * @return the updated QPU object
     */
    private QPU updateQubitCharacteristicsOfQPU(QPU qpu, DeviceProperties deviceProperties, Date calibrationTime) {

        if (deviceProperties.getQubits().size() != qpu.getQubits().size()) {
            logger.error("Number of qubits in the device properties ({}) does not equal number of qubits from the QPU ({})!",
                    deviceProperties.getQubits().size(), qpu.getQubits().size());
            return qpu;
        }

        // Sort list of qubits after their name. This has to be done as the API only provides qubit properties in the order of the qubit
        // names (intergers) and does not provide a reference from the properties to the qubit
        final List<Qubit> sortedQubits = new ArrayList<>(qpu.getQubits());
        sortedQubits.sort(Comparator.comparing(a -> Integer.valueOf(a.getName())));

        // iterate through all properties and update corresonding Qubit
        for (int i = 0; i < deviceProperties.getQubits().size(); i++) {

            // get properties and Qubit which belong together (based on the order)
            final List<Object> propertiesOfQubitList = deviceProperties.getQubits().get(i);
            final Qubit currentQubit = sortedQubits.get(i);

            // skip update if latest characteristics have the same time stamp then current calibration data
            final QubitCharacteristics latestCharacteristics =
                    qubitCharacteristicsRepository.findByQubitOrderByCalibrationTimeDesc(currentQubit).stream().findFirst().orElse(null);
            if (Objects.nonNull(latestCharacteristics) && !calibrationTime.after(latestCharacteristics.getCalibrationTime())) {
                logger.debug("Stored characteristics are up-to-date. No update needed!");
                continue;
            }

            // create new characteristics object with the current characteristics
            final QubitCharacteristics qubitCharacteristics = new QubitCharacteristics();
            qubitCharacteristics.setQubit(currentQubit);
            qubitCharacteristics.setCalibrationTime(calibrationTime);

            // retrieve T1, T2, and readout error
            for (Object propertiesOfQubit : propertiesOfQubitList) {
                final Map<String, String> propertiesMap = transformIbmPropertiesToMap(propertiesOfQubit);

                switch (propertiesMap.get("name")) {
                    case "T1":
                        qubitCharacteristics.setT1Time(new BigDecimal(propertiesMap.get("value")));
                        break;
                    case "T2":
                        qubitCharacteristics.setT2Time(new BigDecimal(propertiesMap.get("value")));
                        break;
                    case "readout_error":
                        qubitCharacteristics.setReadoutError(new BigDecimal(propertiesMap.get("value")));
                        break;
                    default:
                }
            }

            // update qubit object with new characteristics object
            currentQubit.getQubitCharacteristics().add(qubitCharacteristics);
            qubitRepository.save(currentQubit);
        }

        return qpu;
    }

    /**
     * Collect the data about the QPUs from IBMQ and add or update existing database entries
     *
     * @param provider the provider object to connect the QPU objects to
     * @return <code>true</code> if collection of QPU data is successful, <code>false</code> otherwise
     */
    private boolean collectQPUs(Provider provider) {

        try {
            // get all available QPUs
            final GetBackendInformationApi backendInformationApi = new GetBackendInformationApi(defaultClient);
            final List<Device> devices = backendInformationApi
                    .getBackendInformationGetProjectDevicesWithVersion(IBMQ_DEFAULT_HUB, IBMQ_DEFAULT_GROUP, IBMQ_DEFAULT_PROJECT);

            // get details for each retrieved QPU
            boolean status = true;
            for (Device device : devices) {

                // create QPU in database if not already existing
                logger.debug("Found QPU with name '{}'. Adding to database!", device.getBackendName());
                QPU qpu = addQPUToDatabase(provider, device);

                try {
                    logger.debug("Getting detailed information for the QPU...");

                    // skip simulators in further analysis as they do not provide calibration data
                    if (Objects.isNull(device.getSimulator()) || device.getSimulator()) {
                        logger.debug("Device is simulator. Skipping data retrieval!");
                        continue;
                    }

                    // retrieve details about qubits, gates, calibration, and queue size
                    final DeviceProperties deviceProperties = backendInformationApi
                            .getBackendInformationGetDeviceProperties(IBMQ_DEFAULT_HUB, IBMQ_DEFAULT_GROUP, IBMQ_DEFAULT_PROJECT,
                                    device.getBackendName(), null, null);

                    // update QPU object with last calibration and update time
                    final Date lastCalibrated = new Date(deviceProperties.getLastUpdateDate().toInstant().toEpochMilli());
                    qpu.setLastCalibrated(lastCalibrated);
                    qpu.setLastUpdated(new Date(System.currentTimeMillis()));
                    qpuRepository.save(qpu);

                    // add new qubit characteristics if a new calibration was done since the last retrieval
                    updateQubitCharacteristicsOfQPU(qpu, deviceProperties, lastCalibrated);

                    // TODO: retrieve data about gates
                } catch (ApiException e) {
                    logger.error("Exception while getting details about QPU with name '{}': {}", device.getBackendName(),
                            e.getLocalizedMessage());
                    status = false;
                }
            }

            return status;
        } catch (ApiException e) {
            logger.error("Exception while retrieving all available QPUs: {}",
                    e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean collect() {
        logger.debug("Collection by IBMQProvider started...");

        if (!authenticate()) {
            logger.warn("Authentication failed. Aborting retrieval from IBMQProvider. Please check the provided access token!");
            return false;
        }
        logger.debug("Successfully authenticated. Starting retrieval of QPUs...");

        // add the IBMQ provider as object to the database if it was not created in a previous collection and otherwise retrieve it
        final Provider ibmqProvider = addProviderToDatabase();

        final boolean qpuRetrievalSuccess = collectQPUs(ibmqProvider);
        logger.debug("Retrieval of QPUs returned success: {}", qpuRetrievalSuccess);
        return qpuRetrievalSuccess;
    }
}
