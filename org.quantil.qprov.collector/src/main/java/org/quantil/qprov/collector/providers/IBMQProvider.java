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

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.quantil.qprov.collector.IProvider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.ibmq.client.ApiClient;
import org.quantil.qprov.ibmq.client.ApiException;
import org.quantil.qprov.ibmq.client.Configuration;
import org.quantil.qprov.ibmq.client.api.GetBackendInformationApi;
import org.quantil.qprov.ibmq.client.api.GetProvidersInformationApi;
import org.quantil.qprov.ibmq.client.api.LoginApi;
import org.quantil.qprov.ibmq.client.auth.ApiKeyAuth;
import org.quantil.qprov.ibmq.client.model.AccessToken;
import org.quantil.qprov.ibmq.client.model.ApiToken;
import org.quantil.qprov.ibmq.client.model.Device;
import org.quantil.qprov.ibmq.client.model.DeviceProperties;
import org.quantil.qprov.ibmq.client.model.Hub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class IBMQProvider implements IProvider {

    public static final String PROVIDER_ID = "ibmq";

    public static final String IBMQ_DEFAULT_HUB = "ibm-q";

    public static final String IBMQ_DEFAULT_GROUP = "open";

    public static final String IBMQ_DEFAULT_PROJECT = "main";

    private static final Logger logger = LoggerFactory.getLogger(IBMQProvider.class);

    private final String envApiToken;

    private ApiClient defaultClient;

    public IBMQProvider(Environment env) {
        this.envApiToken = env.getProperty("QPROV_IBMQ_TOKEN");
    }

    public boolean authenticate(String token) {

        final String userApiToken;

        // search for the token...
        if (envApiToken != null) {
            userApiToken = envApiToken;
        } else if (!token.isEmpty()) {
            userApiToken = token;
        } else {
            return false;
        }

        // get base client for ibmq api
        this.defaultClient = Configuration.getDefaultApiClient();
        this.defaultClient.setBasePath("https://api.quantum-computing.ibm.com/v2");

        // authenticate
        logger.info("IBMQProvider() try to get an accessToken via supplied apiToken: {}", userApiToken);
        final ApiToken apiToken = new ApiToken();
        apiToken.setApiToken(userApiToken);

        try {
            // get a short-lived access token with the api token
            final AccessToken accessToken = new LoginApi(this.defaultClient).loginLoginWithApiToken(apiToken);

            logger.info("IBMQProvider() successfully got an accessToken: {}", accessToken);

            // configure "API Token" authorization with obtained accessToken
            final ApiKeyAuth apiKeyAuth = (ApiKeyAuth) this.defaultClient.getAuthentication("API Token");
            apiKeyAuth.setApiKey(accessToken.getId());

            return true;
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean preAuthenticationNeeded() {
        return true;
    }

    @Override
    public boolean collect() {
        final List<Hub> hubs = collectHubs();
        hubs.forEach((Hub hub) -> logger.info(hub.toString()));

        final List<QPU> qpus = collectQPUs();
        qpus.forEach((QPU qpu) -> logger.info(qpu.toString()));

        return true;
    }

    public List<Hub> collectHubs() {
        try {
            final GetProvidersInformationApi providersInformationApi = new GetProvidersInformationApi(this.defaultClient);
            return providersInformationApi.getProvidersInformationAllHubsAsCollaborator();
        } catch (ApiException e) {
            System.err.println("Exception when calling GetProvidersInformationApi#getProvidersInformationAllHubsAsCollaborator");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<QPU> collectQPUs() {
        final List<Device> devices;
        final List<QPU> qpus = new ArrayList<>();

        try {
            final GetBackendInformationApi backendInformationApi = new GetBackendInformationApi(defaultClient);
            devices = backendInformationApi
                    .getBackendInformationGetProjectDevicesWithVersion(IBMQ_DEFAULT_HUB, IBMQ_DEFAULT_GROUP, IBMQ_DEFAULT_PROJECT);

            devices.forEach((Device device) -> {

                try {
                    final DeviceProperties deviceProperties = backendInformationApi
                            .getBackendInformationGetDeviceProperties(IBMQ_DEFAULT_HUB, IBMQ_DEFAULT_GROUP, IBMQ_DEFAULT_PROJECT,
                                    device.getBackendName(), null, null);

                    if (deviceProperties.getBackendName() != null) {

                        final ModelMapper modelMapper = new ModelMapper();

                        final QPU qpu = modelMapper.map(device, QPU.class);
                        // TODO: parse into new model
                        qpus.add(qpu);
                    }
                } catch (ApiException e) {
                    System.err.println("Exception when calling GetBackendInformationApi#getBackendInformationGetProjectDevicesWithVersion");
                    System.err.println("Status code: " + e.getCode());
                    System.err.println("Reason: " + e.getResponseBody());
                    System.err.println("Response headers: " + e.getResponseHeaders());
                    e.printStackTrace();
                }
            });
        } catch (ApiException e) {
            System.err.println("Exception when calling GetBackendInformationApi#getBackendInformationGetProjectDevicesWithVersion");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }

        return qpus;
    }
}
