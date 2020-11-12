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

package org.quantil.qprov.core;

/**
 * Constants for the QProv classes.
 */
public final class Constants {

    /**** Namespaces and prefixes ****/
    public static final String DEFAULT_NAMESPACE = "http://quantil.org/qprov";

    public static final String DEFAULT_NAMESPACE_PREFIX = "qprov";

    public static final String DATA_TYPE_QNAME = "QName";

    public static final String NAMESPACE_XSD_PREFIX = "xsd";

    /**** QProv type names ****/
    public static final String QPROV_TYPE_PROVIDER = "provider";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT = "quantumCircuit";

    /**** QProv type attributes ****/
    public static final String QPROV_TYPE_SUFFIX = "Type";

    public static final String QPROV_TYPE_PROVIDER_NAME = "providerName";

    public static final String QPROV_TYPE_PROVIDER_URL = "offeringUrl";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_NAME = "circuitName";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_DEPTH = "circuitDepth";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_WIDTH = "circuitWidth";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_SIZE = "circuitSize";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_URL = "circuitCodeUrl";

    private Constants() {
    }
}
