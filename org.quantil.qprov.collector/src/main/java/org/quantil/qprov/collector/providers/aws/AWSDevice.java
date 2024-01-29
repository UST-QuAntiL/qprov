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

package org.quantil.qprov.collector.providers.aws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
public class AWSDevice {
    private static final Logger logger = LoggerFactory.getLogger(AWSDevice.class);

    private String deviceArn;
    private String deviceName;
    private String providerName;
    private String deviceType;
    private String deviceStatus;
    @ToString.Exclude
    private String deviceCapabilities;

    // These values cannot be directly deserialized with jackson as they are contained within a more complex json object
    @JsonIgnore
    private List<String> gates;
    @JsonIgnore
    private BigDecimal numberQubits;
    @JsonIgnore
    private BigDecimal maxShots;
    @JsonIgnore
    private Date calibrationTime;
    @JsonIgnore
    private Map<Integer, List<Integer>> connectivityMap;

    public void recoverPropertiesFromDeviceCapabilities() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode deviceCapabilitiesNode = mapper.readTree(deviceCapabilities);
        if (!deviceCapabilitiesNode.elements().hasNext()) {
            logger.warn("For QPU {} of provider {} the device capabilities are empty!", deviceName, providerName);
            return;
        }
        recoverBasicProperties(deviceCapabilitiesNode);
        recoverGates(deviceCapabilitiesNode);
        recoverConnectivityMap(deviceCapabilitiesNode);
    }

    private void recoverConnectivityMap(JsonNode deviceCapabilitiesNode) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode connectivityNode = deviceCapabilitiesNode.get("paradigm").get("connectivity");
        if (connectivityNode == null) {
            logger.warn("For device {} of provider {} no connectivity node was returned.", deviceName, providerName);
            return;
        }
        boolean fullyConnected = connectivityNode.get("fullyConnected").asBoolean();
        int numberQubits = getNumberQubits().intValue();
        this.connectivityMap = new HashMap<>(numberQubits);
        if (fullyConnected) {
            // Assuming we only have integer capacity here
            // Assuming for fully connected (ionq) the qubits are named sequentially
            for (int i = 0; i < numberQubits; i++) {
                connectivityMap.put(i, new ArrayList<>(numberQubits - 1));
                // No coupling between i-th and i-th qubit; Doing it in two loops allows us to remain comparison free
                for (int j = 0; j < i; j++) {
                    connectivityMap.get(i).add(j);
                }
                for (int j = i + 1; j < numberQubits; j++) {
                    connectivityMap.get(i).add(j);
                }
            }
            return;
        }
        // NOTICE: At the time of writing this code, this only applies to rigetti QPUs (we only target ionq and rigetti for now). Thus the code is adapted to the json aws provides for rigetti qpus
        JsonNode connectivityGraphNode = deviceCapabilitiesNode.get("paradigm").get("connectivity").get("connectivityGraph");
        Iterator<Map.Entry<String, JsonNode>> connectivityPerNode = connectivityGraphNode.fields();
        while (connectivityPerNode.hasNext()) {
            Map.Entry<String, JsonNode> entry = connectivityPerNode.next();
            List<Integer> connections = Arrays.asList(mapper.treeToValue(entry.getValue(), Integer[].class));
            connectivityMap.put(Integer.parseInt(entry.getKey()), connections);
        }
    }

    private void recoverGates(JsonNode deviceCapabilitiesNode) throws IOException {
        if (deviceCapabilitiesNode.get("action").get("braket.ir.openqasm.program") == null) {
            logger.warn("For device {} of provider {} the gates for openqasm are not available", deviceName, providerName);
            return;
        }
        ArrayNode gatesNode = (ArrayNode) deviceCapabilitiesNode.get("action")
                .get("braket.ir.openqasm.program")
                .get("supportedOperations");
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(new TypeReference<List<String>>() {
        });
        gates = reader.readValue(gatesNode);
    }


    private void recoverBasicProperties(JsonNode deviceCapabilitiesNode) throws IOException {
        numberQubits = new BigDecimal(deviceCapabilitiesNode.get("paradigm").get("qubitCount").asInt());
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(new TypeReference<List<Integer>>() {
        });
        List<Integer> shotRange = reader.readValue(deviceCapabilitiesNode.get("service").get("shotsRange"));
        maxShots = new BigDecimal(shotRange.stream().max(Integer::compareTo).orElse(-1));
        // TODO: It appears that this is currently not the correct meaning as even simulators have an updatedAt property -> Cannot map to calibration time
        if (deviceCapabilitiesNode.get("service").get("updatedAt") == null) {
            logger.warn("Device {} has no calibration time property.", deviceName);
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            String dateString = deviceCapabilitiesNode.get("service").get("updatedAt").asText().replace('T', ' ');
            calibrationTime = formatter.parse(dateString);
        } catch (ParseException e) {
            logger.error("Could not parse the calibration date");
        }
    }
}
