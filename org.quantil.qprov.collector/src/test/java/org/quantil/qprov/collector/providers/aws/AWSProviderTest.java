/*******************************************************************************
 * Copyright (c) 2023 the QProv contributors.
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AWSProviderTest {
    String accessToken = "";
    String secretAccessToken = "";

    @Test
    @DisplayName("Test IonQ retrieval")
    public void testIonQDevice() throws IllegalAccessException {
        AWSDevice device = retrieveDevice("ionq", "us-east-1", "Aria 1");
        Assert.assertArrayEquals(new String[]{"x", "y", "z", "rx", "ry", "rz", "h", "cnot", "s", "si", "t", "ti", "v", "vi", "xx", "yy", "zz", "swap"}, device.getGates().toArray());
        Assert.assertEquals(25, device.getNumberQubits().intValue());
    }

    @Test
    @DisplayName("Test Rigetti retrieval")
    public void testRigettiDevice() throws IllegalAccessException {
        AWSDevice device = retrieveDevice("rigetti", "us-west-1", "Aspen-M-1");
        Assert.assertArrayEquals(
                new String[]{"cz", "xy", "ccnot", "cnot", "cphaseshift", "cphaseshift00", "cphaseshift01", "cphaseshift10", "cswap", "h", "i", "iswap", "phaseshift", "pswap", "rx", "ry", "rz", "s", "si", "swap", "t", "ti", "x", "y", "z"},
                device.getGates().toArray());
        Assert.assertEquals(80, device.getNumberQubits().intValue());
    }

    @Test
    @DisplayName("Test simulator retrieval")
    public void testSimulators() throws IllegalAccessException {
        List<AWSDevice> simulators = retrieveSimulators();
        for (AWSDevice simulator : simulators) {
            Assert.assertEquals("SIMULATOR", simulator.getDeviceType());
        }
        AWSDevice sv1 = simulators.stream().filter(device -> device.getDeviceName().equals("SV1")).findFirst().get();
        Assert.assertEquals(34, sv1.getNumberQubits().intValue());
    }

    private List<AWSDevice> retrieveSimulators() {
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
        final List<AWSDevice>[] devices = new List[1];
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
                    devices[0] = Arrays.asList(mapper.treeToValue(node, AWSDevice[].class));
                    devices[0] = devices[0].stream()
                            .filter(awsDevice -> awsDevice.getDeviceType().equals("SIMULATOR"))
                            // .filter(awsDevice -> !awsDevice.getDeviceStatus().equals("RETIRED"))
                            .collect(Collectors.toList());
                    for (AWSDevice awsDevice : devices[0]) {
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
        return devices[0];
    }

    AWSDevice retrieveDevice(String provider, String region, String name) {
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
        final AWSDevice[] device = new AWSDevice[1];
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
                    device[0] = devices.stream()
                            .filter(awsDevice -> awsDevice.getDeviceType().equals("QPU"))
                            .filter(awsDevice -> awsDevice.getProviderName().toLowerCase().equals(provider))
                            // .filter(awsDevice -> !awsDevice.getDeviceStatus().equals("RETIRED"))
                            .filter(device -> device.getDeviceName().equals(name))
                            .collect(Collectors.toList()).get(0);
                    device[0].recoverPropertiesFromDeviceCapabilities();
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
        return device[0];
    }
}
