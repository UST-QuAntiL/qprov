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

package org.quantil.qprov.collector.providers.ibmq;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.ibmq.client.model.DevicePropsGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IBMQUtility {

    private static final Logger logger = LoggerFactory.getLogger(IBMQUtility.class);

    /**
     * Parse the objects returned by IBM to a Java Map
     *
     * @param propertiesList the propertiesList as provided by the IBM API
     * @return the Map containing the data from the provided properties list
     */
    public static Map<String, String> transformIbmPropertiesToMap(Object propertiesList) {
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
     * Check whether the given gate from the QProv data model operates on the same set of qubits than the gate for which the characteristics were
     * retrieved from IBM
     *
     * @param ibmGateProperties the properties of the gate retrieved from IBM
     * @param gate              the gate from the QPov data model
     * @return <code>true</code> if the two gates operate on the same set of qubits, <code>false</code> otherwise
     */
    public static boolean operatesOnSameQubits(DevicePropsGate ibmGateProperties, Gate gate) {

        if (Objects.isNull(ibmGateProperties.getQubits())) {
            logger.warn("Qubits in IBM gate properties are null for gate with name: {}!", gate.getName());
            return false;
        }

        if (ibmGateProperties.getQubits().size() != gate.getOperatingQubits().size()) {
            logger.debug("Gates operate on different qubits!");
            return false;
        }

        // check if the stored gate and the gate for which the information was retrieved operate on the same qubit
        for (Qubit operatingQubit : gate.getOperatingQubits()) {
            boolean foundMatchingQubit = false;
            for (BigDecimal ibmOperatingQubit : ibmGateProperties.getQubits()) {
                if (ibmOperatingQubit.toString().equals(operatingQubit.getName())) {
                    foundMatchingQubit = true;
                }
            }
            if (!foundMatchingQubit) {
                return false;
            }
        }

        return true;
    }
}
