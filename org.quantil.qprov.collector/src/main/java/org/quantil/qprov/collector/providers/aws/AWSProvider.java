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
import com.amazonaws.services.braket.AWSBraket;
import com.amazonaws.services.braket.AWSBraketClient;
import com.amazonaws.services.braket.AWSBraketClientBuilder;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quantil.qprov.collector.Constants;
import org.quantil.qprov.collector.IProvider;
import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class AWSProvider implements IProvider {

    private static final Logger logger = LoggerFactory.getLogger(AWSProvider.class);

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private final QubitRepository qubitRepository;

    private final QubitCharacteristicsRepository qubitCharacteristicsRepository;

    private final GateCharacteristicsRepository gateCharacteristicsRepository;

    private final GateRepository gateRepository;

    private final Boolean executeCalibrationCircuits;

    // Checkout https://docs.aws.amazon.com/braket/latest/developerguide/braket-regions.html for regions
    private final List<String> regions = List.of(
            "us-east-1",    // IonQ and QuEra
            "us-west-1",        // Rigetti
            "eu-west-2");                // OQC

    private final Map<String, AWSBraket> clientsPerRegion = new HashMap<>();
    private final Map<String, List<AWSDevice>> devicesPerRegion = new HashMap<>();

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
        this.executeCalibrationCircuits = executeCalibrationCircuits;

        regions.forEach(this::constructAWSBraketClient);

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

    private void constructAWSBraketClient(String region) {
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
        builder.setRegion("us-east-1");
        builder.setRequestHandlers(new RequestHandler2() {
            @Override
            public HttpResponse beforeUnmarshalling(Request<?> request, HttpResponse httpResponse) {
                try {
                    // Get array of devices
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(IOUtils.toString(httpResponse.getContent())).get("devices");
                    List<AWSDevice> devices = Arrays.asList(mapper.treeToValue(node, AWSDevice[].class));
                    devicesPerRegion.put(region, devices.stream().filter(awsDevice -> awsDevice.getDeviceType().equals("QPU")).collect(Collectors.toList()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return super.beforeUnmarshalling(request, httpResponse);
            }
        });
        clientsPerRegion.put(region, builder.build());
    }

    /**
     * Check if IBMQ provider already exists in the database and return it or otherwise create it
     *
     * @return the retrieved or created IBMQ provider object
     */
    private Provider addProviderToDatabase() {
        final Optional<Provider> providerOptional = providerRepository.findByName(AWSConstants.PROVIDER_ID);
        if (providerOptional.isPresent()) {
            logger.debug("Provider already present, skipping creation.");
            return providerOptional.get();
        }

        // create a new Provider object representing the IBMQ provider that is handled by this collector
        final Provider provider = new Provider();
        provider.setName(AWSConstants.PROVIDER_ID);
        try {
            provider.setOfferingURL(new URL(AWSConstants.PROVIDER_URL));
        } catch (MalformedURLException e) {
            logger.error("Unable to add provider URL due to MalformedURLException!");
        }
        providerRepository.save(provider);

        return provider;
    }

    @Override
    public String getProviderId() {
        return AWSConstants.PROVIDER_ID;
    }

    @Override
    public boolean collectFromApi() {
        return false;
    }

    @Override
    public boolean collectThroughCircuits() {
        return false;
    }
}
