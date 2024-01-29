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

package org.quantil.qprov.web;

/**
 * Constants for the QProv Web module
 */
public final class Constants {

    /**** Swagger Tags ****/
    public static final String TAG_ROOT = "root";

    public static final String TAG_PROVIDER = "provider";

    public static final String TAG_SQL = "sql";

    public static final String TAG_VIRTUAL_MACHINE = "virtual-machine";

    public static final String TAG_PROV = "provenance-document";

    public static final String TAG_PROV_TEMPLATE = "provenance-template";

    /**** API paths ****/
    public static final String PATH_PROV = "provenance-documents";

    public static final String PATH_PROV_TEMPLATE = "provenance-templates";

    public static final String PATH_PROV_ENTITIES = "entities";

    public static final String PATH_PROV_AGENTS = "agents";

    public static final String PATH_PROV_ACTIVITIES = "activities";

    public static final String PATH_PROV_XML = "xml-document";

    public static final String PATH_PROV_JPEG = "jpeg";

    public static final String PATH_PROV_PDF = "pdf";

    public static final String PATH_PROV_NAMESPACE = "namespace";

    public static final String PATH_PROV_PARAMETERS = "parameters";

    public static final String PATH_PROVIDERS = "providers";

    public static final String PATH_SQL = "sql";

    public static final String PATH_VIRTUAL_MACHINES = "virtual-machines";

    public static final String PATH_QPUS = "qpus";

    public static final String PATH_QUBITS = "qubits";

    public static final String PATH_GATES = "gates";

    public static final String PATH_QUBITS_CONNECTED = "connected-qubit-";

    public static final String PATH_QUBITS_OPERATING = "operating-qubit-";

    public static final String PATH_AGGREGATED_DATA = "aggregated-data";

    public static final String PATH_CALIBRATION_MATRIX = "calibration-matrix";

    public static final String PATH_CHARACTERISTICS = "characteristics";

    private Constants() {
    }
}
