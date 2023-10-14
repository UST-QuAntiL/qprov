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
import com.amazonaws.services.braket.model.SearchDevicesRequest;
import com.amazonaws.services.braket.model.SearchDevicesResult;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AWSProviderTest {
    String accessToken = "";
    String secretAccessToken = "";
    Map<String, AWSBraket> clientsPerRegion = new HashMap<>();
    List<AWSDevice> devices;

    @Test
    @DisplayName("General test")
    public void testBraketClientSDKIonQ() throws IllegalAccessException {
        testBraket("ionq");
    }

    @Test
    @DisplayName("General test")
    public void testBraketClientSDKRigetti() throws IllegalAccessException {
        testBraket("rigetti");
    }

    private void testBraket(String provider) throws IllegalAccessException {
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
        if (provider.equals("ionq")) {
            handleIonQ();
        } else if (provider.equals("rigetti")) {
            handleRigetti();

        } else {
            throw new IllegalAccessException("Provider is not supported!");
        }
    }

    private void handleRigetti() {
        throw new NotImplementedException("Rigetti is more complex and will be supported after IonQ");
    }

    private void handleIonQ() {
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
                    System.out.printf("Received the following payload: %s%n", IOUtils.toString(httpResponse.getContent()));
                    JsonNode node = mapper.readTree(IOUtils.toString(httpResponse.getContent())).get("devices");
                    devices = Arrays.asList(mapper.treeToValue(node, AWSDevice[].class));
                    devices = devices.stream()
                            .filter(awsDevice -> awsDevice.getDeviceType().equals("QPU"))
                            .filter(awsDevice -> awsDevice.getProviderName().toLowerCase().equals("ionq"))
                            .filter(awsDevice -> !awsDevice.getDeviceStatus().equals("RETIRED"))
                            .collect(Collectors.toList());
                    System.out.printf("Got a total of %d devices.%n", devices.size());
                    // This works for ionq
                    for (AWSDevice device : devices) {
                        JsonNode providerNode = mapper.readTree(device.getDeviceCapabilities()).get("provider");
                        if (providerNode == null) {
                            System.out.printf("Provider node for %s is null.%n", device.getDeviceName());
                            continue;
                        }
                        double fidelity1Q = providerNode.get("fidelity").get("1Q").get("mean").asDouble();
                        double fidelity2Q = providerNode.get("fidelity").get("2Q").get("mean").asDouble();
                        double fidelitySpam = providerNode.get("fidelity").get("spam").get("mean").asDouble();
                        double timingT1 = providerNode.get("timing").get("T1").asDouble();
                        double timingT2 = providerNode.get("timing").get("T2").asDouble();
                        double timing1Q = providerNode.get("timing").get("1Q").asDouble();
                        double timing2Q = providerNode.get("timing").get("2Q").asDouble();
                        double timingReadout = providerNode.get("timing").get("readout").asDouble();
                        double timingReset = providerNode.get("timing").get("reset").asDouble();
                    }
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
        SearchDevicesResult searchDevicesResult = client.searchDevices(request);
    }
}
