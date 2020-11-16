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

import java.util.Objects;

import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IBMQCircuitExecutor {

    private static final Logger logger = LoggerFactory.getLogger(IBMQCircuitExecutor.class);

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    public IBMQCircuitExecutor(ProviderRepository providerRepository, QPURepository qpuRepository) {
        this.providerRepository = providerRepository;
        this.qpuRepository = qpuRepository;
    }

    /**
     * Collect the different kinds of provenance data by executing corresponding calibration circuits
     *
     * @param ibmqToken the IBMQ access token to execute quantum circuits on QPUs from IBMQ
     * @return <code>true</code> if collection was successful, <code>false</code> if an error occured
     */
    public boolean collectDataByCircuitExecutions(String ibmqToken) {
        final Provider provider = providerRepository.findByName(IBMQConstants.PROVIDER_ID).orElse(null);

        if (Objects.isNull(provider)) {
            logger.error("Unable to retrieve IBMQ provider from database. Run API collection first and retry then!");
            return false;
        }

        boolean success = true;
        for (QPU qpu : qpuRepository.findByProvider(provider)) {
            logger.debug("Determining data on QPU: {}", qpu.getName());

            // We currently collect the calibration matrices by running circuits. The collection of other kind of data can be added here
            success &= collectCalibrationMatrix(qpu, ibmqToken);
        }

        return success;
    }

    /**
     * Execute calibration circuits to calculate the calibration matrix on the given QPU
     *
     * @param qpu       the QPU to calculate the calibration matrix for
     * @param ibmqToken the IBMQ access token to execute quantum circuits on QPUs from IBMQ
     * @return <code>true</code> if the calculation was successful, otherwise <code>false</code>
     */
    private boolean collectCalibrationMatrix(QPU qpu, String ibmqToken) {
        // TODO
        return false;
    }
}
