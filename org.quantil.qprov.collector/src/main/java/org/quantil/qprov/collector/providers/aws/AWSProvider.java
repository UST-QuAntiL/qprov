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

package org.quantil.qprov.collector.providers.aws;

import com.amazonaws.Request;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.braket.AWSBraketClient;
import com.amazonaws.services.braket.AWSBraketClientBuilder;
import com.amazonaws.services.braket.model.SearchDevicesRequest;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.quantil.qprov.collector.Constants;
import org.quantil.qprov.collector.IProvider;
import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.GateCharacteristics;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.model.entities.QubitCharacteristics;
import org.quantil.qprov.core.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class AWSProvider implements IProvider {

    private static final Logger logger = LoggerFactory.getLogger(AWSProvider.class);
    private static final Map<String, Integer> qubitsPerGateQasm = ImmutableMap.<String, Integer>builder()
            .put("x", Integer.valueOf(1))
            .put("y", Integer.valueOf(1))
            .put("z", Integer.valueOf(1))
            .put("rx", Integer.valueOf(1))
            .put("ry", Integer.valueOf(1))
            .put("rz", Integer.valueOf(1))
            .put("h", Integer.valueOf(1))
            .put("s", Integer.valueOf(1))
            .put("si", Integer.valueOf(1))
            .put("t", Integer.valueOf(1))
            .put("ti", Integer.valueOf(1))
            .put("v", Integer.valueOf(1))
            .put("vi", Integer.valueOf(1))
            .put("cnot", Integer.valueOf(2))
            .put("xx", Integer.valueOf(2))
            .put("yy", Integer.valueOf(2))
            .put("zz", Integer.valueOf(2))
            .put("swap", Integer.valueOf(2))
            .build();

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    private final QubitCharacteristicsRepository qubitCharacteristicsRepository;

    private final GateCharacteristicsRepository gateCharacteristicsRepository;

    private final GateRepository gateRepository;


    // Checkout https://docs.aws.amazon.com/braket/latest/developerguide/braket-regions.html for regions
    private final Map<String, String> providersAndRegions = Map.of(
            "ionq", "us-east-1",
            "rigetti", "us-west-1",
            "aws", "us-east-1");

    private final Map<String, String> offeringURLs = Map.of(
            "ionq", "https://ionq.com/",
            "rigetti", "https://www.rigetti.com/",
            "aws", "https://aws.amazon.com/braket/"
    );

    private final Map<String, List<AWSDevice>> devicesPerProvider = new HashMap<>();
    private List<AWSDevice> simulators;

    @Value("${qprov.aws.token}")
    private String accessToken;
    @Value("${qprov.aws.secret-token}")
    private String secretAccessToken;

    public AWSProvider(ProviderRepository providerRepository, QPURepository qpuRepository,
                       QubitRepository qubitRepository,
                       QubitCharacteristicsRepository qubitCharacteristicsRepository,
                       GateCharacteristicsRepository gateCharacteristicsRepository,
                       GateRepository gateRepository,
                       @Value("${qprov.aws.execute-calibration}") Boolean executeCalibrationCircuits,
                       @Value("${qprov.aws.auto-collect}") Boolean autoCollect,
                       @Value("${qprov.aws.auto-collect-interval}") Integer autoCollectInterval,
                       @Value("${qprov.aws.auto-collect-interval-circuits}") Integer autoCollectIntervalCircuits) {
        this.providerRepository = providerRepository;
        this.qpuRepository = qpuRepository;
        this.qubitRepository = qubitRepository;
        this.qubitCharacteristicsRepository = qubitCharacteristicsRepository;
        this.gateCharacteristicsRepository = gateCharacteristicsRepository;
        this.gateRepository = gateRepository;

        // periodically collect data if activated in properties/environment variables
        if (autoCollect) {
            logger.debug("Auto collection activated with interval: {} min", autoCollectInterval);
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
            scheduler.scheduleAtFixedRate(new AWSRunnableApi(this),
                    Constants.DEFAULT_COLLECTION_STARTUP_TIME, autoCollectInterval, TimeUnit.MINUTES);

            if (executeCalibrationCircuits) {
                logger.debug("Auto collection by circuit execution activated with interval: {} min", autoCollectIntervalCircuits);

                // circuit execution is delayed as it relies on the set of identified QPUs from the API collection
                scheduler.scheduleAtFixedRate(new AWSRunnableCircuits(this),
                        Constants.DEFAULT_COLLECTION_STARTUP_TIME_CIRCUITS, autoCollectIntervalCircuits, TimeUnit.MINUTES);
            }
        }
    }

    @Override
    public String getProviderId() {
        return AWSConstants.PROVIDER_ID;
    }

    @Override
    public boolean collectFromApi() {
        for (Map.Entry<String, String> providerAndRegion : providersAndRegions.entrySet()) {
            try {
                getDevices(providerAndRegion.getKey(), providerAndRegion.getValue());
            } catch (RuntimeException runtimeException) {
                logger.error("Encountered an error when retrieving devices for provider {}", providerAndRegion.getKey());
                logger.error(runtimeException.getMessage());
                logger.error("Access key: {}", accessToken);
            }
        }
        for (String provider : providersAndRegions.keySet()) {
            Provider providerObj = addProviderToDatabase(provider);
            if (devicesPerProvider.get(provider) == null) {
                logger.error("Devices for provider {} could not be retrieved.", provider);
                continue;
            }
            for (AWSDevice device : devicesPerProvider.get(provider)) {
                logger.debug("Adding QPU {} of provider {} to database", device.getDeviceName(), device.getProviderName());
                final QPU qpu = addQPUToDatabase(providerObj, device);
                // Not entirely sure whether the updatedAt property is the calibrationDate
                Date lastCalibrated = new Date();
                if (device.getCalibrationTime() == null) {
                    logger.error("Device {} of provider {} does not have a valid calibration time.", device, provider);
                } else {
                    lastCalibrated = new Date(device.getCalibrationTime().toInstant().toEpochMilli());
                    qpu.setLastCalibrated(lastCalibrated);
                }
                qpu.setLastUpdated(new Date(System.currentTimeMillis()));
                qpuRepository.save(qpu);
                // add new qubit and gate characteristics if a new calibration was done since the last retrieval
                logger.debug("Updating qubit characteristics...");
                updateQubitCharacteristicsOfQPU(qpu, device, lastCalibrated);
                logger.debug("Updating gate characteristics...");
                updateGateCharacteristicsOfQPU(qpu.getDatabaseId(), device, lastCalibrated);
            }
        }
        if (simulators == null) {
            logger.error("No simulators from AWS retrieved.");
            return false;
        }
        for (AWSDevice simulator : simulators) {
            Provider providerObj = addProviderToDatabase("aws");
            addQPUToDatabase(providerObj, simulator);
        }
        return true;
    }

    private void getDevices(String provider, String region) {
        getQPUs(provider, region);
        getSimulators();
    }

    private void getSimulators() {
        AWSBraketClientBuilder builder = AWSBraketClient.builder();
        builder.setCredentials(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new BasicAWSCredentials(accessToken, secretAccessToken);
            }

            @Override
            public void refresh() {

            }
        });
        builder.setRegion("us-west-1");
        simulators = new ArrayList<>();
        builder.setRequestHandlers(new RequestHandler2() {
            @Override
            public HttpResponse beforeUnmarshalling(Request<?> request, HttpResponse httpResponse) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = IOUtils.toString(httpResponse.getContent());
                    JsonNode node = mapper.readTree(json).get("devices");
                    if (node == null) {
                        System.out.println("JSON response does not contain a devices property.");
                        return super.beforeUnmarshalling(request, httpResponse);
                    }
                    // Get array of devices
                    simulators = Arrays.asList(mapper.treeToValue(node, AWSDevice[].class));
                    simulators = simulators.stream()
                            .filter(awsDevice -> awsDevice.getDeviceType().equals("SIMULATOR"))
                            .collect(Collectors.toList());
                    for (AWSDevice awsDevice : simulators) {
                        awsDevice.recoverPropertiesFromDeviceCapabilities();
                    }
                    super.beforeUnmarshalling(request, httpResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return super.beforeUnmarshalling(request, httpResponse);
            }
        });
        AWSBraketClient client = (AWSBraketClient) builder.build();
        SearchDevicesRequest request = new SearchDevicesRequest();
        request.setFilters(new ArrayList<>());
        // The handling is done within the beforeUnmarshalling handler as the SDK discards the deviceCapabilities.
        client.searchDevices(request);
    }

    private void getQPUs(String provider, String region) {
        AWSBraketClientBuilder builder = AWSBraketClient.builder();
        builder.setCredentials(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new BasicAWSCredentials(accessToken, secretAccessToken);
            }

            @Override
            public void refresh() {

            }
        });
        builder.setRegion(region);
        builder.setRequestHandlers(new RequestHandler2() {
            @Override
            public HttpResponse beforeUnmarshalling(Request<?> request, HttpResponse httpResponse) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = IOUtils.toString(httpResponse.getContent());
                    JsonNode node = mapper.readTree(json).get("devices");
                    if (node == null) {
                        System.out.println("JSON response does not contain a devices property.");
                        return super.beforeUnmarshalling(request, httpResponse);
                    }
                    // Get array of devices
                    List<AWSDevice> devices = Arrays.asList(mapper.treeToValue(node, AWSDevice[].class));
                    devices = devices.stream()
                            .filter(awsDevice -> awsDevice.getDeviceType().equals("QPU"))
                            .filter(awsDevice -> awsDevice.getProviderName().toLowerCase().equals(provider))
                            .filter(awsDevice -> !awsDevice.getDeviceStatus().equals("RETIRED"))
                            .collect(Collectors.toList());
                    for (AWSDevice device : devices) {
                        device.recoverPropertiesFromDeviceCapabilities();
                    }
                    devicesPerProvider.put(provider, devices);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return super.beforeUnmarshalling(request, httpResponse);
            }
        });
        AWSBraketClient client = (AWSBraketClient) builder.build();
        SearchDevicesRequest request = new SearchDevicesRequest();
        request.setFilters(new ArrayList<>());
        // The handling is done within the beforeUnmarshalling handler as the SDK discards the deviceCapabilities.
        client.searchDevices(request);
    }


    /**
     * Check if provider already exists in the database and return it or otherwise create it
     *
     * @return the retrieved or created IBMQ provider object
     */
    private Provider addProviderToDatabase(String provider) {
        final Optional<Provider> providerOptional = providerRepository.findByName(provider);
        if (providerOptional.isPresent()) {
            logger.debug("Provider already present, skipping creation.");
            return providerOptional.get();
        }

        // create a new Provider object representing the IBMQ provider that is handled by this collector
        final Provider providerObj = new Provider();
        providerObj.setName(provider);
        try {
            providerObj.setOfferingURL(new URL(offeringURLs.get(provider)));
        } catch (MalformedURLException e) {
            logger.error("Unable to add provider URL due to MalformedURLException!");
        }
        providerRepository.save(providerObj);

        return providerObj;
    }

    private QPU addQPUToDatabase(Provider provider, AWSDevice device) {
        final Optional<QPU> qpuOptional = qpuRepository.findByName(device.getDeviceName());
        if (qpuOptional.isPresent()) {
            logger.debug("QPU already present, updating information.");
            QPU qpu = qpuOptional.get();
            if (device.getMaxShots() == null) {
                logger.error("For device {} of provider {} the max shots property is null.", device.getDeviceName(), provider);
            } else {
                qpu.setMaxShots(device.getMaxShots().intValue());
            }
            qpu = qpuRepository.save(qpu);
            return qpu;
        }

        // create a new QPU object representing the retrieved device
        QPU qpu = new QPU();
        qpu.setName(device.getDeviceName());

        qpu.setProvider(provider);
        if (device.getMaxShots() == null) {
            logger.error("For device {} of provider {} the max shots property is null.", device.getDeviceName(), provider);
        } else {
            qpu.setMaxShots(device.getMaxShots().intValue());
        }
        qpu = qpuRepository.save(qpu);

        addQubits(provider, device, qpu);

        // add gates to the qubits on which they can be executed
        if (device.getGates() == null) {
            logger.error("Device {} of provider {} has no gates in the model.", device, provider);
        } else {
            logger.debug("QPU {} has gates {}", device.getDeviceName(), device.getGates());
            logger.debug("Adding gates to device model...");
            addGatesFromDevice(device.getGates(), qpu, device);
        }

        qpu = qpuRepository.save(qpu);
        return qpu;
    }

    private void addQubits(Provider provider, AWSDevice device, QPU qpu) {
        // TODO: Adapted this, is this correct -> Benjamin
        // add qubits
        final Map<String, Qubit> qubits = new HashMap<>();
        if (Objects.nonNull(device.getConnectivityMap())) {
            // Preconstruct the qubits based on the connectivity map keys
            device.getConnectivityMap().keySet().stream().map(String::valueOf)
                    .map(qubitName -> constructQubit(qpu, qubitName))
                    .forEach(qubit -> {
                        qubits.put(qubit.getName(), qubit);
                        qpu.getQubits().add(qubit);
                    });

            // for each qubit, find all connected qubits
            for (Integer qubitId : device.getConnectivityMap().keySet()) {
                final String qubitName = qubitId.toString();
                // create new qubit if not already done
                Qubit qubit = qubits.get(qubitName);
                Set<Qubit> connectedQubits = device.getConnectivityMap().keySet().stream().map(String::valueOf).map(qubits::get).collect(Collectors.toSet());
                qubit.setConnectedQubits(connectedQubits);
            }
        } else {
            if (device.getNumberQubits() == null) {
                logger.error("For device {} of provider {} the number of qubits property is null.", device.getDeviceName(), provider);
                return;
            }
            // for simulators and QPUs with one qubit no coupling map exists, therefore just add the qubits
            for (int i = 0; i < device.getNumberQubits().intValue(); i++) {
                Qubit qubit = new Qubit();
                qubit.setQpu(qpu);
                qubit.setName(String.valueOf(i));
                qubit = qubitRepository.save(qubit);

                qubits.put(qubit.getName(), qubit);
                qpu.getQubits().add(qubit);
            }
        }
    }

    private Qubit constructQubit(QPU qpu, String qubitName) {
        Qubit qubit = new Qubit();
        qubit.setQpu(qpu);
        qubit.setName(qubitName);
        return qubitRepository.save(qubit);
    }

    public void addGatesFromDevice(List<String> gateNames, QPU qpu, AWSDevice device) {
        // add gate to each qubit in the coupling if it operates on multiple qubits
        List<String> oneQubitGates = gateNames.parallelStream().filter(this::is1QubitGateQasm).collect(Collectors.toList());
        logger.debug("1 Qubit gate list {}", oneQubitGates);
        List<String> twoQubitGates = gateNames.parallelStream().filter(Predicate.not(this::is1QubitGateQasm)).collect(Collectors.toList());
        logger.debug("2 Qubit gate list {}", twoQubitGates);
        HashMap<Integer, Qubit> qubits = new HashMap<>();
        for (Integer qubitId : device.getConnectivityMap().keySet()) {
            Optional<Qubit> optQubit = qubitRepository.findByQpuAndName(qpu, String.valueOf(qubitId));
            if (optQubit.isEmpty()) {
                logger.error("Qubit {} is null for device {}", qubitId, device.getDeviceName());
                continue;
            }
            qubits.put(qubitId, optQubit.get());
        }
        // Collect all distinct qubit ids
        if (device.getConnectivityMap() != null) {
            for (Integer sourceQubitId : device.getConnectivityMap().keySet()) {
                Qubit sourceQubit = qubits.get(sourceQubitId);
                for (String gateName : oneQubitGates) {
                    Gate gate = new Gate();
                    gate.setName(gateName);
                    gate.setQpu(qpu);
                    // Add gate to supported gates of the qubit it operates on
                    sourceQubit.getSupportedGates().add(gate);
                    gate.addOperatingQubit(sourceQubit);
                    qubitRepository.save(sourceQubit);
                    // TODO: This leads to an illegal state representation and it appears to work anyway. Not sure if we need it? (i am not familiar enough with JPA)
                    // gateRepository.save(gate);
                }
                // Use sublist to ensure no double counting e.g. 0-1 and 1-0 (assumes the list are sorted
                // Assuming sorted list (is the case for ionq)
                List<Integer> targetQubitIds = device.getConnectivityMap().get(sourceQubitId);
                // Assumes that the sourceQubitIds are numbered from 0 and the keyset is sorted:
                targetQubitIds = targetQubitIds.subList(sourceQubitId.intValue(), targetQubitIds.size());
                for (Integer targetQubitId : targetQubitIds) {
                    Qubit targetQubit = qubits.get(targetQubitId);
                    for (String gateName : twoQubitGates) {
                        Gate gate = new Gate();
                        gate.setName(gateName);
                        gate.setQpu(qpu);
                        // Add gate to supported gates of the qubits it operates on; This sets the operating qubits of the gate automatically
                        sourceQubit.getSupportedGates().add(gate);
                        targetQubit.getSupportedGates().add(gate);
                        gate.addOperatingQubit(sourceQubit);
                        gate.addOperatingQubit(targetQubit);
                        qubitRepository.save(sourceQubit);
                        qubitRepository.save(targetQubit);
                        // TODO: This leads to an illegal state representation and it appears to work anyway. Not sure if we need it? (i am not familiar enough with JPA)
                        // gateRepository.save(gate);
                    }
                }
            }
        }
    }

    /**
     * Update the qubit characteristics of the given QPU with the latest calibration data and add to the database
     *
     * @param qpu the QPU to update the qubit characteristics for
     */
    private void updateQubitCharacteristicsOfQPU(QPU qpu, AWSDevice device, Date calibrationTime) {
        // iterate through all properties and update corresponding Qubit
        if (device.getConnectivityMap() != null) {
            // We do this in case the qubits are not numbered/named sequentially
            for (Integer qubitId : device.getConnectivityMap().keySet()) {
                updateQubitCharacteristicsOfQPU(qubitId.toString(), qpu, device, calibrationTime);
            }
        } else {
            for (int i = 0; i < qpu.getQubits().size(); i++) {
                updateQubitCharacteristicsOfQPU(String.valueOf(i), qpu, device, calibrationTime);
            }
        }
    }

    private void updateQubitCharacteristicsOfQPU(String qubitId, QPU qpu, AWSDevice device, Date calibrationTime) {
        final Qubit currentQubit = qubitRepository.findByQpuAndName(qpu, qubitId).orElse(null);

        if (Objects.isNull(currentQubit)) {
            logger.warn("Unable to retrieve related qubit with name {} for QPU {}", qubitId, qpu.getName());
            return;
        }

        // skip update if latest characteristics have the same time stamp then current calibration data
        final QubitCharacteristics latestCharacteristics =
                qubitCharacteristicsRepository.findByQubitOrderByCalibrationTimeDesc(currentQubit).stream().findFirst().orElse(null);
        if (Objects.nonNull(latestCharacteristics) && !calibrationTime.after(latestCharacteristics.getCalibrationTime())) {
            logger.debug("Stored characteristics are up-to-date. No update needed!");
            return;
        }

        // create new characteristics object with the current characteristics
        final QubitCharacteristics qubitCharacteristics = new QubitCharacteristics();
        qubitCharacteristics.setQubit(currentQubit);
        qubitCharacteristics.setCalibrationTime(calibrationTime);

        // retrieve T1, T2, and readout error
        switch (device.getProviderName().toLowerCase()) {
            case "ionq":
                handleIonqQubitProperties(qubitCharacteristics, device);
                break;
            case "rigetti":
                handleRigettiQubitProperties(qubitCharacteristics, device);
                break;
            default:
                logger.warn("For device {} of provider {} no qubit handler is available. Qubit properties will be null.", device.getDeviceName(), device.getProviderName());
        }

        // update qubit object with new characteristics object
        currentQubit.getQubitCharacteristics().add(qubitCharacteristics);
        qubitRepository.save(currentQubit);
    }

    private void handleRigettiQubitProperties(QubitCharacteristics qubitCharacteristics, AWSDevice device) {
        logger.warn("Retrieving Qubit properties for Rigetti devices not yet supported!");
        // TODO: Implement me
    }

    private void handleIonqQubitProperties(QubitCharacteristics qubitCharacteristics, AWSDevice device) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode providerNode = mapper.readTree(device.getDeviceCapabilities()).get("provider");
            if (providerNode == null) {
                logger.warn("The IONQ device json has not the expected format. Cannot find provider node.");
                return;
            }
            // Fidelities
            JsonNode fidelityNode = providerNode.get("fidelity");
            if (fidelityNode != null) {
                JsonNode readoutError = fidelityNode.get("spam");
                if (readoutError == null) {
                    logger.warn("The IONQ device json has not the expected format. Cannot find spam/readout error node.");
                } else {
                    qubitCharacteristics.setReadoutError(BigDecimal.valueOf(1.0).subtract(new BigDecimal(readoutError.get("mean").asText())));
                }
            } else {
                logger.warn("The IONQ device json has not the expected format. Cannot find fidelity node.");
            }
            JsonNode timingNode = providerNode.get("timing");
            if (timingNode != null) {
                JsonNode t1 = timingNode.get("T1");
                JsonNode t2 = timingNode.get("T2");
                if (t1 != null) {
                    // in seconds
                    qubitCharacteristics.setT1Time(BigDecimal.valueOf(t1.asDouble()));
                } else {
                    logger.warn("The IONQ device json has not the expected format. Cannot find t1 timing node.");
                }
                if (t2 != null) {
                    // in seconds
                    qubitCharacteristics.setT2Time(BigDecimal.valueOf(t2.asDouble()));
                } else {
                    logger.warn("The IONQ device json has not the expected format. Cannot find t2 timing node.");
                }
            } else {
                logger.warn("The IONQ device json has not the expected format. Cannot find timing node.");
            }
        } catch (JsonProcessingException ex) {
            logger.error("Error processing qubit properties for QPU {} of provider {}:", device.getDeviceName(), device.getProviderName());
            logger.error(ex.getMessage());
        }

    }

    /**
     * Update the gate characteristics of the given QPU with the latest calibration data and add to the database
     *
     * @param qpuId           the Id of the QPU to update the gate characteristics for
     * @param calibrationTime the time of the calibration the given device properties were retrieved from
     */
    private void updateGateCharacteristicsOfQPU(UUID qpuId, AWSDevice device, Date calibrationTime) {
        final QPU qpu = qpuRepository.findById(qpuId).orElse(null);
        if (Objects.isNull(qpu)) {
            logger.error("Unable to retrieve QPU with Id: {}", qpuId);
            return;
        }

        logger.debug("QPU {} has {} qubits", qpu.getName(), qpu.getQubits().size());
        final List<Gate> gates =
                qpu.getQubits().stream().flatMap(qubit -> qubit.getSupportedGates().stream()).distinct().collect(Collectors.toList());
        logger.debug("Updating characteristics for {} gates of QPU: {}", gates.size(), qpu.getName());

        for (Gate gate : gates) {
            // skip update if latest characteristics have the same time stamp then current calibration data
            final GateCharacteristics latestCharacteristics =
                    gateCharacteristicsRepository.findByGateOrderByCalibrationTimeDesc(gate).stream().findFirst().orElse(null);
            if (Objects.nonNull(latestCharacteristics) && !calibrationTime.after(latestCharacteristics.getCalibrationTime())) {
                logger.debug("Stored gate characteristics are up-to-date. No update needed!");
                continue;
            }

            final GateCharacteristics gateCharacteristics = new GateCharacteristics();
            gateCharacteristics.setGate(gate);
            gateCharacteristics.setCalibrationTime(calibrationTime);

            switch (device.getProviderName().toLowerCase()) {
                case "ionq":
                    handleIonqGateProperties(gateCharacteristics, device);
                    break;
                case "rigetti":
                    handleRigettiGateProperties(gateCharacteristics, device);
                    break;
                default:
                    logger.warn("For device {} of provider {} no qubit handler is available. Qubit properties will be null.", device.getDeviceName(), device.getProviderName());
            }

            // update gate object with new characteristics object
            gate.getGateCharacteristics().add(gateCharacteristics);
            gateRepository.save(gate);
        }
    }

    private void handleIonqGateProperties(GateCharacteristics gateCharacteristics, AWSDevice device) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode providerNode = mapper.readTree(device.getDeviceCapabilities()).get("provider");
            if (providerNode == null) {
                logger.warn("The IONQ device json has not the expected format. Cannot find provider node.");
                return;
            }
            JsonNode fidelityNode = providerNode.get("fidelity");
            JsonNode timingNode = providerNode.get("timing");
            // If not updated (in case json is incomplete) just set them null
            BigDecimal gateErrorRate;
            BigDecimal gateTime;
            if (is1QubitGateQasm(gateCharacteristics.getGate().getName())) { // 1 Qubit gate
                gateErrorRate = retrieveGateErrorRate("1Q", fidelityNode);
                gateTime = retrieveTiming("1Q", timingNode);

            } else { // 2 Qubit gate
                gateErrorRate = retrieveGateErrorRate("2Q", fidelityNode);
                gateTime = retrieveTiming("2Q", timingNode);
            }
            // in seconds
            gateCharacteristics.setGateTime(gateTime);
            gateCharacteristics.setGateErrorRate(gateErrorRate);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    // Gate type is 1Q or 2Q for 1 or 2 qubit gates
    private BigDecimal retrieveTiming(String gateType, JsonNode timingNode) {
        boolean timingAvailable = timingNode != null;
        if (!timingAvailable) {
            logger.warn("The IONQ device json has not the expected format. Cannot find timing node.");
        }
        if (timingAvailable) {
            timingNode = timingNode.get(gateType);
            if (timingNode == null) {
                logger.warn("The IONQ device json has not the expected format. Cannot find timing node for 1 Qubit gates.");
            } else {
                return new BigDecimal(timingNode.asText());
            }
        }
        return null;

    }

    // Gate type is 1Q or 2Q for 1 or 2 qubit gates
    private BigDecimal retrieveGateErrorRate(String gateType, JsonNode fidelityNode) {
        boolean fidelityAvailable = fidelityNode != null;
        if (!fidelityAvailable) {
            logger.warn("The IONQ device json has not the expected format. Cannot find fidelity node.");
        }
        if (fidelityAvailable) {
            fidelityNode = fidelityNode.get(gateType);
            if (fidelityNode == null) {
                logger.warn("The IONQ device json has not the expected format. Cannot find fidelity node for 1 Qubit gates.");
            } else {
                fidelityNode = fidelityNode.get("mean");
                return BigDecimal.valueOf(1.0).subtract(new BigDecimal(fidelityNode.asText()));
            }
        }
        return null;
    }

    private boolean is1QubitGateQasm(String name) {
        if (!qubitsPerGateQasm.keySet().contains(name)) {
            logger.error("Unknown number of qubits for gate {} defaulting to 1", name);
        }
        return qubitsPerGateQasm.getOrDefault(name, 1) == 1;
    }

    private void handleRigettiGateProperties(GateCharacteristics gateCharacteristics, AWSDevice device) {
        logger.warn("Retrieving Gate properties for Rigetti devices not yet supported!");
        //TODO: Implement
    }

    @Override
    public boolean collectThroughCircuits() {
        logger.warn("Collect through circuit not implemented");
        return false;
    }
}
