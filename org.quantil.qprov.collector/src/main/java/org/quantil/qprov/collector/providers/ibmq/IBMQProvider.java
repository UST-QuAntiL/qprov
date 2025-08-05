/*******************************************************************************
 * Copyright (c) 2024 the QProv contributors.
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

package org.quantil.qprov.collector.providers.ibmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quantil.qprov.collector.Constants;
import org.quantil.qprov.collector.IProvider;
import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.GateCharacteristics;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.model.entities.QubitCharacteristics;
import org.quantil.qprov.core.repositories.*;
import org.quantil.qprov.ibm.iam.client.api.TokenRetrievalApi;
import org.quantil.qprov.ibm.quantum.client.ApiException;
import org.quantil.qprov.ibm.quantum.client.api.BackendsApi;
import org.quantil.qprov.ibm.quantum.client.auth.ApiKeyAuth;
import org.quantil.qprov.ibm.quantum.client.auth.HttpBearerAuth;
import org.quantil.qprov.ibm.quantum.client.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class IBMQProvider implements IProvider {

    protected static final Logger logger = LogManager.getLogger();

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    private final QubitCharacteristicsRepository qubitCharacteristicsRepository;

    private final GateCharacteristicsRepository gateCharacteristicsRepository;

    private final GateRepository gateRepository;

    private final IBMQCircuitExecutor ibmqCircuitExecutor;

    private final Boolean executeCalibrationCircuits;

    private org.quantil.qprov.ibm.iam.client.ApiClient iamClient;
    private org.quantil.qprov.ibm.quantum.client.ApiClient ibmClient;

    @Value("${qprov.ibmq.apikey}")
    private String ibmApiKey;
    @Value("${qprov.ibmq.service-crn}")
    private String ibmqServiceCrn;

    public IBMQProvider(ProviderRepository providerRepository, QPURepository qpuRepository,
                        QubitRepository qubitRepository,
                        QubitCharacteristicsRepository qubitCharacteristicsRepository,
                        GateCharacteristicsRepository gateCharacteristicsRepository,
                        GateRepository gateRepository,
                        IBMQCircuitExecutor ibmqCircuitExecutor,
                        @Value("${qprov.ibmq.execute-calibration}") Boolean executeCalibrationCircuits,
                        @Value("${qprov.ibmq.auto-collect}") Boolean autoCollect,
                        @Value("${qprov.ibmq.auto-collect-interval}") Integer autoCollectInterval,
                        @Value("${qprov.ibmq.auto-collect-interval-circuits}") Integer autoCollectIntervalCircuits) {
        this.providerRepository = providerRepository;
        this.qpuRepository = qpuRepository;
        this.qubitRepository = qubitRepository;
        this.qubitCharacteristicsRepository = qubitCharacteristicsRepository;
        this.gateCharacteristicsRepository = gateCharacteristicsRepository;
        this.gateRepository = gateRepository;
        this.executeCalibrationCircuits = executeCalibrationCircuits;
        this.ibmqCircuitExecutor = ibmqCircuitExecutor;

        this.iamClient = org.quantil.qprov.ibm.iam.client.Configuration.getDefaultApiClient();
        this.iamClient.setBasePath("https://iam.cloud.ibm.com");
        this.ibmClient = org.quantil.qprov.ibm.quantum.client.Configuration.getDefaultApiClient();
        this.ibmClient.setBasePath("https://quantum.cloud.ibm.com/api/v1");

        logger.debug("Started IBMQ Provider with auto collect: {}", autoCollect);

        // periodically collect data if activated in properties/environment variables
        if (autoCollect) {
            logger.debug("Auto collection activated with interval: {} min", autoCollectInterval);
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
            scheduler.scheduleAtFixedRate(new IBMQRunnableApi(this),
                    Constants.DEFAULT_COLLECTION_STARTUP_TIME, autoCollectInterval, TimeUnit.MINUTES);

            if (executeCalibrationCircuits) {
                logger.debug("Auto collection by circuit execution activated with interval: {} min", autoCollectIntervalCircuits);

                // circuit execution is delayed as it relies on the set of identified QPUs from the API collection
                scheduler.scheduleAtFixedRate(new IBMQRunnableCircuits(this),
                        Constants.DEFAULT_COLLECTION_STARTUP_TIME_CIRCUITS, autoCollectIntervalCircuits, TimeUnit.MINUTES);
            }
        }
    }

    /**
     * Authenticate at IBM IAM using the token provided through the environment variables
     *
     * @return <code>true</code> if authentication is successful, <code>false</code> otherwise
     */
    private boolean authenticate() {

        // abort authentication if token is not provided
        if (Objects.isNull(this.ibmApiKey)) {
            logger.error("No api key provided!");
            return false;
        }

        try {
            final var tokenRetrievalApi = new TokenRetrievalApi(this.iamClient);
            final var token = tokenRetrievalApi.getTokenApiKey("urn:ibm:params:oauth:grant-type:apikey", this.ibmApiKey, null);
            final var bearerAuth = (HttpBearerAuth) this.ibmClient.getAuthentication("IBMCloudAuth");
            bearerAuth.setBearerToken(token.getAccessToken());
            final var serviceCRN = (ApiKeyAuth) this.ibmClient.getAuthentication("ServiceCRN");
            serviceCRN.setApiKey(this.ibmqServiceCrn);

            return true;
        } catch (org.quantil.qprov.ibm.iam.client.ApiException e) {
            logger.error("Error while authenticating at IBM IAM: {}", e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Check if IBMQ provider already exists in the database and return it or otherwise create it
     *
     * @return the retrieved or created IBMQ provider object
     */
    private Provider addProviderToDatabase() {
        final Optional<Provider> providerOptional = providerRepository.findByName(IBMQConstants.PROVIDER_ID);
        if (providerOptional.isPresent()) {
            logger.debug("Provider already present, skipping creation.");
            return providerOptional.get();
        }

        // create a new Provider object representing the IBMQ provider that is handled by this collector
        final Provider provider = new Provider();
        provider.setName(IBMQConstants.PROVIDER_ID);
        try {
            provider.setOfferingURL(new URL(IBMQConstants.PROVIDER_URL));
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
    private QPU addQPUToDatabase(Provider provider, BackendsResponseV2DevicesInner device, Map<String, Object> deviceProperties, Map<String, Object> deviceConfiguration) {
        final Optional<QPU> qpuOptional = qpuRepository.findByName(device.getName());
        if (qpuOptional.isPresent()) {
            logger.debug("QPU already present, updating information.");
            QPU qpu = qpuOptional.get();
            qpu.setVersion((String) deviceProperties.get("backend_version"));
            qpu.setMaxShots((int) deviceConfiguration.get("max_shots"));
            qpu = qpuRepository.save(qpu);
            return qpu;
        }

        // create a new QPU object representing the retrieved device
        QPU qpu = new QPU();
        qpu.setName(device.getName());
        qpu.setProvider(provider);
        qpu.setVersion((String) deviceProperties.get("backend_version"));
        qpu.setMaxShots((int) deviceConfiguration.get("max_shots"));
        qpu.setSimulator(Objects.nonNull(device.getIsSimulator()) && device.getIsSimulator());
        qpu = qpuRepository.save(qpu);

        // add qubits
        final Map<String, Qubit> qubits = new HashMap<>();
        if (Objects.nonNull(deviceConfiguration.get("coupling_map"))) {
            for (List<Integer> coupling : (List<List<Integer>>) deviceConfiguration.get("coupling_map")) {
                final List<Qubit> alreadyAdded = new ArrayList<>();
                for (Integer qubitId : coupling) {
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
            for (int i = 0; i < device.getQubits(); i++) {
                Qubit qubit = new Qubit();
                qubit.setQpu(qpu);
                qubit.setName(String.valueOf(i));
                qpu.getQubits().add(qubit);
                qubit = qubitRepository.save(qubit);

                qubits.put(qubit.getName(), qubit);
            }
        }

        // add gates to the qubits on which they can be executed
        if (Objects.nonNull(device.getIsSimulator()) && !device.getIsSimulator() && Objects.nonNull(deviceConfiguration.get("gates"))) {
            for (var ibmGate : (List<Map<String, Object>>) deviceConfiguration.get("gates")) {
                addGateFromDevice(ibmGate, qpu);
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
     */
    public void addGateFromDevice(Map<String, Object> ibmGate, QPU qpu) {

        // remove duplicates in coupling map
        final List<List<Integer>> distinctList =
                ((List<List<Integer>>) ibmGate.get("coupling_map")).stream().map(listToSort -> listToSort.stream().sorted().collect(Collectors.toList())).distinct()
                        .collect(Collectors.toList());

        // each gate is instantiated for each coupling map, as the gate on different qubits has different characteristics
        for (List<Integer> coupling : distinctList) {
            Gate gate = new Gate();
            gate.setName((String) ibmGate.get("name"));
            gate.setQpu(qpu);
            gate = gateRepository.save(gate);

            // add gate to each qubit in the coupling if it operates on multiple qubits
            final Set<Qubit> operatingQubits = new HashSet<>();
            for (Integer qubitId : coupling) {
                final Qubit qubit = qubitRepository.findByQpuAndName(qpu, qubitId.toString()).orElse(null);
                if (Objects.nonNull(qubit)) {
                    qubit.addSupportedGate(gate);
                    qubitRepository.save(qubit);
                }
            }
            gate.setOperatingQubits(operatingQubits);
            gateRepository.save(gate);
        }
    }

    /**
     * Update the qubit characteristics of the given QPU with the latest calibration data and add to the database
     *
     * @param qpu              the QPU to update the qubit characteristics for
     * @param deviceProperties the device properties retrieved from the IBM API
     * @param calibrationTime  the time of the calibration the given device properties were retrieved from
     */
    private void updateQubitCharacteristicsOfQPU(QPU qpu, Map<String, Object> deviceProperties, Date calibrationTime) {
        var qubits = (List<List<Map<String, Object>>>) deviceProperties.get("qubits");

        if (qubits.size() != qpu.getQubits().size()) {
            logger.error("Number of qubits in the device properties ({}) does not equal number of qubits from the QPU ({})!",
                    qubits.size(), qpu.getQubits().size());
            return;
        }

        // iterate through all properties and update corresponding Qubit
        for (int i = 0; i < qubits.size(); i++) {

            // get properties and Qubit which belong together (based on the order)
            final List<Map<String, Object>> propertiesOfQubitList = qubits.get(i);
            final Qubit currentQubit = qubitRepository.findByQpuAndName(qpu, String.valueOf(i)).orElse(null);

            if (Objects.isNull(currentQubit)) {
                logger.warn("Unable to retrieve related qubit with name {} for QPU {}", i, qpu.getName());
                continue;
            }

            // skip update if latest characteristics have the same time stamp then current calibration data
            final QubitCharacteristics latestCharacteristics =
                    qubitCharacteristicsRepository.findByQubitOrderByCalibrationTimeDesc(currentQubit).stream().findFirst().orElse(null);
            if (Objects.nonNull(latestCharacteristics) && !calibrationTime.after(latestCharacteristics.getCalibrationTime())) {
                logger.trace("Stored characteristics are up-to-date. No update needed!");
                continue;
            }

            // create new characteristics object with the current characteristics
            final QubitCharacteristics qubitCharacteristics = new QubitCharacteristics();
            qubitCharacteristics.setQubit(currentQubit);
            qubitCharacteristics.setCalibrationTime(calibrationTime);

            // retrieve T1, T2, and readout error - NOTE: T1 and T2 are in micro seconds (us)
            for (var propertiesOfQubit : propertiesOfQubitList) {
                switch ((String) propertiesOfQubit.get("name")) {
                    case "T1":
                        qubitCharacteristics.setT1Time(new BigDecimal((Integer) propertiesOfQubit.get("value")));
                        break;
                    case "T2":
                        qubitCharacteristics.setT2Time(new BigDecimal((Integer) propertiesOfQubit.get("value")));
                        break;
                    case "readout_error":
                        qubitCharacteristics.setReadoutError(new BigDecimal((Integer) propertiesOfQubit.get("value")));
                        break;
                    default:
                }
            }

            // update qubit object with new characteristics object
            currentQubit.getQubitCharacteristics().add(qubitCharacteristics);
            qubitRepository.save(currentQubit);
        }
    }

    /**
     * Update the gate characteristics of the given QPU with the latest calibration data and add to the database
     *
     * @param qpuId            the Id of the QPU to update the gate characteristics for
     * @param deviceProperties the device properties retrieved from the IBM API
     * @param calibrationTime  the time of the calibration the given device properties were retrieved from
     */
    private void updateGateCharacteristicsOfQPU(UUID qpuId, Map<String, Object> deviceProperties, Date calibrationTime) {

        final QPU qpu = qpuRepository.findById(qpuId).orElse(null);
        if (Objects.isNull(qpu)) {
            logger.error("Unable to retrieve QPU with Id: {}", qpuId);
            return;
        }

        final List<Gate> gates =
                qpu.getQubits().stream().flatMap(qubit -> qubit.getSupportedGates().stream()).distinct().collect(Collectors.toList());
        logger.debug("Updating characteristics for {} gates of QPU: {}", gates.size(), qpu.getName());

        for (Gate gate : gates) {

            // skip update if latest characteristics have the same time stamp then current calibration data
            final GateCharacteristics latestCharacteristics =
                    gateCharacteristicsRepository.findByGateOrderByCalibrationTimeDesc(gate).stream().findFirst().orElse(null);
            if (Objects.nonNull(latestCharacteristics) && !calibrationTime.after(latestCharacteristics.getCalibrationTime())) {
                logger.trace("Stored gate characteristics are up-to-date. No update needed!");
                continue;
            }

            // get the DevicePropsGate that belongs to the gate that should be updated with the characteristics
            final var matchingGateOptional =
                    ((List<Map<String, Object>>) deviceProperties.get("gates")).stream()
                            .filter(ibmGate -> ibmGate.get("gate").equals(gate.getName()))
                            .filter(ibmGate -> IBMQUtility.operatesOnSameQubits(ibmGate, gate))
                            .findFirst();

            if (matchingGateOptional.isEmpty()) {
                logger.warn("No properties found for gate {} on QPU: {}", gate.getName(), qpu.getName());
                continue;
            }
            final var matchingGate = matchingGateOptional.get();

            if (Objects.isNull(matchingGate.get("parameters"))) {
                logger.warn("Parameters for matching gate properties are null!");
                continue;
            }

            // create new characteristics object with the current characteristics
            final GateCharacteristics gateCharacteristics = new GateCharacteristics();
            gateCharacteristics.setGate(gate);
            gateCharacteristics.setCalibrationTime(calibrationTime);

            // retrieve gate time and error rate - NOTE: gate times are in nano seconds (ns)
            for (Map<String, Object> characteristicsOfGate : (List<Map<String, Object>>) matchingGate.get("parameters")) {

                switch ((String) characteristicsOfGate.get("Name")) {
                    case "gate_error":
                        gateCharacteristics.setGateErrorRate(BigDecimal.valueOf((Float) characteristicsOfGate.get("Value")));
                        break;
                    case "gate_length":
                        gateCharacteristics.setGateTime(BigDecimal.valueOf((Integer) characteristicsOfGate.get("Value")));
                        break;
                    default:
                }
            }

            // update gate object with new characteristics object
            gate.getGateCharacteristics().add(gateCharacteristics);
            gateRepository.save(gate);
        }
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
            final BackendsApi backendsApi = new BackendsApi(this.ibmClient);
            final var devices = backendsApi.listBackends("2025-05-01").getDevices();

            // get details for each retrieved QPU
            boolean status = true;
            for (var device : devices) {
                final var deviceProperties = backendsApi.getBackendProperties(device.getName(), "2025-05-01", null);
                final var deviceConfiguration = backendsApi.getBackendConfiguration(device.getName(), "2025-05-01");

                // create QPU in database if not already existing
                logger.debug("Found QPU with name '{}'. Adding to database!", device.getName());
                final QPU qpu = addQPUToDatabase(provider, device, deviceProperties, deviceConfiguration);

                logger.debug("Getting detailed information for the QPU...");

                final int queueSize = device.getQueueLength();
                qpu.setQueueSize(queueSize);
                logger.debug("Current queue size: {}", queueSize);

                // skip simulators in further analysis as they do not provide calibration data
                if (Objects.isNull(device.getIsSimulator()) || device.getIsSimulator()) {
                    logger.debug("Device is simulator. Skipping data retrieval!");
                    qpuRepository.save(qpu);
                    continue;
                }

                // retrieve details about qubits, gates, calibration, and queue size
                final var lastCalibratedString = (String) deviceProperties.get("last_update_date");

                DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
                Instant instant = Instant.from(formatter.parse(lastCalibratedString));
                Date lastCalibrated = Date.from(instant);

                // update QPU object with last calibration and update time
                qpu.setLastCalibrated(lastCalibrated);
                qpu.setLastUpdated(new Date(System.currentTimeMillis()));
                qpuRepository.save(qpu);

                // add new qubit and gate characteristics if a new calibration was done since the last retrieval
                updateQubitCharacteristicsOfQPU(qpu, deviceProperties, lastCalibrated);
                updateGateCharacteristicsOfQPU(qpu.getDatabaseId(), deviceProperties, lastCalibrated);
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
        return IBMQConstants.PROVIDER_ID;
    }

    @Override
    public boolean collectFromApi() {
        logger.debug("Collection by IBMQProvider started...");

        if (!authenticate()) {
            logger.warn("Authentication failed. Aborting retrieval from IBMQProvider. Please check the provided access token!");
            return false;
        }
        logger.debug("Successfully authenticated. Starting retrieval of QPUs...");
        final Provider ibmqProvider = addProviderToDatabase();

        final boolean qpuRetrievalSuccess = collectQPUs(ibmqProvider);
        logger.debug("Retrieval of QPUs returned success: {}", qpuRetrievalSuccess);
        return qpuRetrievalSuccess;
    }

    @Override
    public boolean collectThroughCircuits() {
        throw new UnsupportedOperationException("Not updated yet.");

//        if (!executeCalibrationCircuits) {
//            logger.warn("Execution of calibration circuits deactivated in the properties. Please activate for this functionality!");
//            return false;
//        }
//
//        logger.debug("Triggering execution of circuits to determine calibration data for QPUs from IBMQ!");
//        return ibmqCircuitExecutor.collectDataByCircuitExecutions(ibmApiKey);
    }
}
