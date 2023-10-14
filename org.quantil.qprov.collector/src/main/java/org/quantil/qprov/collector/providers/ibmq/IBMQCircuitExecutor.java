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

import org.quantil.qprov.collector.Constants;
import org.quantil.qprov.collector.providers.qiskit.service.QiskitServiceRequest;
import org.quantil.qprov.collector.providers.qiskit.service.QiskitServiceResult;
import org.quantil.qprov.core.model.agents.Provider;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.model.entities.CalibrationMatrix;
import org.quantil.qprov.core.repositories.ProviderRepository;
import org.quantil.qprov.core.repositories.QPURepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.Vector;

@Component
public class IBMQCircuitExecutor {

    private static final Logger logger = LoggerFactory.getLogger(IBMQCircuitExecutor.class);

    private final ProviderRepository providerRepository;

    private final QPURepository qpuRepository;

    private URI createCalibrationMatrixApiEndpoint;

    public IBMQCircuitExecutor(ProviderRepository providerRepository, QPURepository qpuRepository,
                               @Value("${qprov.qiskit-service.hostname}") String hostname,
                               @Value("${qprov.qiskit-service.port}") int port,
                               @Value("${qprov.qiskit-service.version}") String version) {
        this.providerRepository = providerRepository;
        this.qpuRepository = qpuRepository;

        createCalibrationMatrixApiEndpoint =
                URI.create(String.format("http://%s:%d/qiskit-service/api/%s/calculate-calibration-matrix", hostname, port, version));
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

            if (qpu.isSimulator()) {
                logger.debug("Skipping simulator {} for calibration matrix calculation!", qpu.getName());
                continue;
            }

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

        // Prepare the request to the Qiskit service
        final RestTemplate restTemplate = new RestTemplate();
        final QiskitServiceRequest request = new QiskitServiceRequest(qpu.getName(), ibmqToken);

        try {
            // make the execution request
            final URI resultLocation = restTemplate.postForLocation(createCalibrationMatrixApiEndpoint, request);

            boolean retrievedResult = false;
            QiskitServiceResult result = restTemplate.getForObject(resultLocation, QiskitServiceResult.class);
            while (!retrievedResult) {
                try {
                    // poll for result of calibration matrix calculation
                    result = restTemplate.getForObject(resultLocation, QiskitServiceResult.class);
                    retrievedResult = result.isComplete();

                    // wait for next poll
                    Thread.sleep(Constants.CALIBRATION_MATRIX_CALCULATION_POLLING_INTERVAL);
                } catch (InterruptedException e) {
                    // pass
                } catch (RestClientException e) {
                    logger.error("Unable to retrieve result from URL: {}", resultLocation);
                    retrievedResult = true;
                }
            }

            if (Objects.isNull(result) || !result.isComplete()) {
                logger.debug("Retrieval of calibration matrix not successful!");
                return false;
            }

            // get the calibration matrix from the result
            final Object qiskitResult = result.getResult().get(IBMQConstants.QISKIT_SERVICE_RESULT_VARIABLE);

            final Vector<Vector<Double>> parsedCalibrationMatrix = new Vector<Vector<Double>>();
            final String[] rows = qiskitResult.toString().split("],\\[");
            for (String row : rows) {
                final String cleanedRow = row.replaceAll("\\[", "").replaceAll("]", "");
                final Vector<Double> rowVector = new Vector<Double>();
                for (String entry : cleanedRow.split(",")) {
                    rowVector.add(Double.parseDouble(entry));
                }
                parsedCalibrationMatrix.add(rowVector);
            }

            // add new calibration matrix to the database and update corresponding QPU
            final CalibrationMatrix calibrationMatrix = new CalibrationMatrix();
            calibrationMatrix.setQpu(qpu);
            calibrationMatrix.setCalibrationTime(new Date(System.currentTimeMillis()));
            calibrationMatrix.setCalibrationMatrix(parsedCalibrationMatrix);
            qpu.getCalibrationMatrices().add(calibrationMatrix);
            qpuRepository.save(qpu);

            return true;
        } catch (RestClientException e) {
            logger.error("Unable to access Qiskit service at URL: {}", createCalibrationMatrixApiEndpoint);
            return false;
        }
    }
}
