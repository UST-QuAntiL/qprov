package org.quantil.qprov.collector.providers;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.quantil.qprov.collector.IProvider;
import org.quantil.qprov.core.entities.QPU;
import org.quantil.qprov.core.entities.QPUProperties;
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

    private static final Logger logger = LoggerFactory.getLogger(IBMQProvider.class);

    public static final String PROVIDER_ID = "ibmq";

    public static final String IBMQ_DEFAULT_HUB = "ibm-q";
    public static final String IBMQ_DEFAULT_GROUP = "open";
    public static final String IBMQ_DEFAULT_PROJECT = "main";

    private final String envApiToken;

    public ApiClient defaultClient;

    public IBMQProvider(Environment env) {
        this.envApiToken = env.getProperty("QPROV_IBMQ_TOKEN");
    }

    public boolean authenticate(String token) {

        String userApiToken;

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
        ApiToken apiToken = new ApiToken();
        apiToken.setApiToken(userApiToken);

        try {
            // get a short-lived access token with the api token
            AccessToken accessToken;
            accessToken = new LoginApi(this.defaultClient).loginLoginWithApiToken(apiToken);

            logger.info("IBMQProvider() successfully got an accessToken: {}", accessToken);

            // configure "API Token" authorization with obtained accessToken
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) this.defaultClient.getAuthentication("API Token");
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
    public boolean collect(String token) {
        this.authenticate(token);
        return collect();
    }
    @Override
    public boolean collect() {
        List<Hub> hubs = collectHubs();
        hubs.forEach((Hub hub) -> logger.info(hub.toString()));

        List<QPU> qpus = collectQPUs();
        qpus.forEach((QPU qpu) -> logger.info(qpu.toString()));

        return true;
    }

    public List<Hub> collectHubs() {
        try {
            GetProvidersInformationApi providersInformationApi = new GetProvidersInformationApi(this.defaultClient);
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
        List<Device> devices;
        List<QPU> qpus = new ArrayList<>();

        try {
            GetBackendInformationApi backendInformationApi = new GetBackendInformationApi(defaultClient);
            devices = backendInformationApi.getBackendInformationGetProjectDevicesWithVersion(IBMQ_DEFAULT_HUB, IBMQ_DEFAULT_GROUP, IBMQ_DEFAULT_PROJECT);

            devices.forEach((Device device) -> {

                try {

                    DeviceProperties deviceProperties = backendInformationApi.getBackendInformationGetDeviceProperties(IBMQ_DEFAULT_HUB, IBMQ_DEFAULT_GROUP, IBMQ_DEFAULT_PROJECT, device.getBackendName(), null, null);

                    if (deviceProperties.getBackendName() != null) {

                        ModelMapper modelMapper = new ModelMapper();

                        QPU qpu = modelMapper.map(device, QPU.class);
                        QPUProperties qpuProperties = modelMapper.map(deviceProperties, QPUProperties.class);

                        qpu.setProvider(PROVIDER_ID);
                        qpu.setProperties(qpuProperties);
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
