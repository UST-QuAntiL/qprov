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

package org.quantil.qprov.core;

/**
 * Constants for the QProv classes.
 */
public final class Constants {

    /**** General constants ****/
    public static final int BIG_DECIMAL_PRECISION = 30;

    public static final int BIG_DECIMAL_SCALE = 15;

    /**** Namespaces and prefixes ****/
    public static final String DEFAULT_NAMESPACE = "http://quantil.org/qprov";

    public static final String DEFAULT_NAMESPACE_PREFIX = "qprov";

    public static final String DATA_TYPE_QNAME = "QName";

    public static final String NAMESPACE_XSD_PREFIX = "xsd";

    /**** QProv type names ****/
    public static final String QPROV_TYPE_PROVIDER = "provider";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT = "quantumCircuit";

    public static final String QPROV_TYPE_CLASSICAL_DATA = "classicalData";

    public static final String QPROV_TYPE_COMPILE = "compileActivity";

    public static final String QPROV_TYPE_EXECUTE = "executeActivity";

    public static final String QPROV_TYPE_PREPARE_DATA = "prepareDataActivity";

    public static final String QPROV_TYPE_COMPILER = "compiler";

    public static final String QPROV_TYPE_PREPARATION_SERVICE = "dataPreparationService";

    public static final String QPROV_TYPE_QPU = "qpu";

    public static final String QPROV_TYPE_QUBIT = "qubit";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE = "virtualMachine";

    /**** QProv type attributes ****/
    public static final String QPROV_TYPE_SUFFIX = "Type";

    public static final String QPROV_TYPE_PROVIDER_NAME = "providerName";

    public static final String QPROV_TYPE_PROVIDER_URL = "offeringUrl";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_NAME = "circuitName";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_DEPTH = "circuitDepth";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_WIDTH = "circuitWidth";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_SIZE = "circuitSize";

    public static final String QPROV_TYPE_QUANTUM_CIRCUIT_URL = "circuitCodeUrl";

    public static final String QPROV_TYPE_COMPILATION_TIME = "compilationTime";

    public static final String QPROV_TYPE_COMPILATION_OPTIMIZATION = "optimizationGoal";

    public static final String QPROV_TYPE_COMPILATION_RANDOM_SEED = "randomSeed";

    public static final String QPROV_TYPE_EXECUTION_TIME = "executionTime";

    public static final String QPROV_TYPE_EXECUTION_SHOTS = "numberOfShots";

    public static final String QPROV_TYPE_EXECUTION_MITIGATION = "appliedMitigationTechnique";

    public static final String QPROV_TYPE_PREPARATION_ENCODING = "appliedEncoding";

    public static final String QPROV_TYPE_COMPILER_NAME = "compilerName";

    public static final String QPROV_TYPE_COMPILER_PROVIDER_NAME = "compilerProviderName";

    public static final String QPROV_TYPE_COMPILER_VERSION = "compilerVersion";

    public static final String QPROV_TYPE_PREPARATION_SERVICE_NAME = "dataPreparationServiceName";

    public static final String QPROV_TYPE_PREPARATION_SERVICE_PROVIDER_NAME = "dataPreparationServiceProviderName";

    public static final String QPROV_TYPE_PREPARATION_SERVICE_VERSION = "dataPreparationServiceVersion";

    public static final String QPROV_TYPE_QPU_NAME = "qpuName";

    public static final String QPROV_TYPE_QPU_UPDATE = "lastUpdate";

    public static final String QPROV_TYPE_QPU_CALIBRATION = "lastCalibration";

    public static final String QPROV_TYPE_QPU_QUEUE = "queueSize";

    public static final String QPROV_TYPE_QPU_MAX_SHOTS = "maxShots";

    public static final String QPROV_TYPE_QPU_VERSION = "qpuVersion";

    public static final String QPROV_TYPE_QPU_SIMULATOR = "isSimulator";

    public static final String QPROV_TYPE_QUBIT_NAME = "qubitName";

    public static final String QPROV_TYPE_QUBIT_CONNECTED_QUBITS = "connectedQubits";

    public static final String QPROV_TYPE_QUBIT_T1 = "t1";

    public static final String QPROV_TYPE_QUBIT_T2 = "t2";

    public static final String QPROV_TYPE_QUBIT_READOUT_ERROR = "readoutError";

    public static final String QPROV_TYPE_QUBIT_CALIBRATION_TIME = "calibrationTime";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_NAME = "virtualMachineName";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_CPU_NAME = "virtualMachineCpuName";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_CPU_CORES_COUNT = "virtualMachineCpuCoresCount";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_RAM_SIZE = "virtualMachineRamSize";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_DISK_SIZE = "virtualMachineDiskSize";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_RECORDING_TIME = "recordingTime";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_CPU_USAGE = "cpuUsage";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_CLOCK_SPEED = "clockSpeed";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_RAM_USAGE = "ramUsage";

    public static final String QPROV_TYPE_VIRTUAL_MACHINE_DISK_USAGE = "diskUsage";

    /**** Default values ****/

    public static final String QPROV_CHARACTERISTICS_NO_DATA = "no data";

    private Constants() {
    }
}
