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

package org.quantil.qprov.core.model.agents;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.model.entities.*;
import org.quantil.qprov.core.utils.Utils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class QPU extends org.openprovenance.prov.xml.Agent implements ProvExtension<QPU> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private String name;

    private String version;

    private Date lastUpdated;

    private Date lastCalibrated;

    private int maxShots;

    private int queueSize;

    private boolean isSimulator;

    @OneToMany(mappedBy = "qpu",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Gate> gateSet = new HashSet<>();

    @OneToMany(mappedBy = "qpu",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Qubit> qubits = new HashSet<>();

    @OneToMany(mappedBy = "qpu",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<CalibrationMatrix> calibrationMatrices = new HashSet<>();

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Provider provider;

    /**
     * Return the average T1 time from all qubits of the last calibration or null if no calibration data is available
     *
     * @return the average T1 time of all qubits, or 0 if no calibration data is available
     */
    public BigDecimal getAvgT1Time() {
        return BigDecimal.valueOf(qubits.stream().map(qubit -> qubit.getQubitCharacteristics().stream()
                        .min(Comparator.comparing(QubitCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(qubitCharacteristics -> qubitCharacteristics.getT1Time().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the average T2 time from all qubits of the last calibration or null if no calibration data is available
     *
     * @return the average T2 time of all qubits, or 0 if no calibration data is available
     */
    public BigDecimal getAvgT2Time() {
        return BigDecimal.valueOf(qubits.stream().map(qubit -> qubit.getQubitCharacteristics().stream()
                        .min(Comparator.comparing(QubitCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(qubitCharacteristics -> qubitCharacteristics.getT2Time().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the average readout error from all qubits of the last calibration or null if no calibration data is available
     *
     * @return the average readout error of all qubits, or 0 if no calibration data is available
     */
    public BigDecimal getAvgReadoutError() {
        return BigDecimal.valueOf(qubits.stream().map(qubit -> qubit.getQubitCharacteristics().stream()
                        .min(Comparator.comparing(QubitCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(qubitCharacteristics -> qubitCharacteristics.getReadoutError().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the average multi qubit gate error from all multi qubit gates of the last calibration or null if no calibration data is available
     *
     * @return the average multi qubit gate error of all multi qubit gates, or 0 if no calibration data is available
     */
    public BigDecimal getAvgMultiQubitGateError() {
        return BigDecimal.valueOf(gateSet.stream().filter(gate -> gate.getOperatingQubits().size() > 1)
                .map(gate -> gate.getGateCharacteristics().stream()
                        .min(Comparator.comparing(GateCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(gateCharacteristics -> gateCharacteristics.getGateErrorRate().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the average single qubit gate error from all single qubit gates of the last calibration or null if no calibration data is available
     *
     * @return the average single qubit gate error of all single qubit gates, or 0 if no calibration data is available
     */
    public BigDecimal getAvgSingleQubitGateError() {
        return BigDecimal.valueOf(gateSet.stream().filter(gate -> gate.getOperatingQubits().size() == 1)
                .map(gate -> gate.getGateCharacteristics().stream()
                        .min(Comparator.comparing(GateCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(gateCharacteristics -> gateCharacteristics.getGateErrorRate().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the average multi qubit gate time from all multi qubit gates of the last calibration or null if no calibration data is available
     *
     * @return the average multi qubit gate time of all multi qubit gates, or 0 if no calibration data is available
     */
    public BigDecimal getAvgMultiQubitGateTime() {
        return BigDecimal.valueOf(gateSet.stream().filter(gate -> gate.getOperatingQubits().size() > 1)
                .map(gate -> gate.getGateCharacteristics().stream()
                        .min(Comparator.comparing(GateCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(gateCharacteristics -> gateCharacteristics.getGateTime().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the average single qubit gate time from all single qubit gates of the last calibration or null if no calibration data is available
     *
     * @return the average single qubit gate time of all single qubit gates, or 0 if no calibration data is available
     */
    public BigDecimal getAvgSingleQubitGateTime() {
        return BigDecimal.valueOf(gateSet.stream().filter(gate -> gate.getOperatingQubits().size() == 1)
                .map(gate -> gate.getGateCharacteristics().stream()
                        .min(Comparator.comparing(GateCharacteristics::getCalibrationTime)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(gateCharacteristics -> gateCharacteristics.getGateTime().doubleValue())
                .average()
                .orElse(0));
    }

    /**
     * Return the maximum gate time from all gates of the last calibration or null if no calibration data is available
     *
     * @return the maximum gate time of all gates on all qubits, or <code>null</code> if no calibration data is available
     */
    public BigDecimal getMaximumGateTime() {
        BigDecimal maxGateTime = BigDecimal.valueOf(0);
        for (Gate gate : gateSet) {
            final Optional<GateCharacteristics> latestCharacteristicsOptional =
                    gate.getGateCharacteristics().stream().min(Comparator.comparing(GateCharacteristics::getCalibrationTime));
            if (latestCharacteristicsOptional.isPresent()) {
                final GateCharacteristics latestCharacteristics = latestCharacteristicsOptional.get();
                if (latestCharacteristics.getGateTime().compareTo(maxGateTime) > 0) {
                    maxGateTime = latestCharacteristics.getGateTime();
                }
            }
        }
        return maxGateTime;
    }

    @Override
    public Set<Statement> toStandardCompliantProv(QPU extensionStatement) {
        final org.openprovenance.prov.xml.Agent agent = new org.openprovenance.prov.xml.Agent();

        // add QPU specific attributes
        agent.setId(Utils.generateQualifiedName(databaseId.toString(), null));
        agent.getType().add(Utils.createTypeElement(Constants.QPROV_TYPE_QPU));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_NAME, name,
                        Constants.QPROV_TYPE_QPU_NAME + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_UPDATE, lastUpdated.toString(),
                        Constants.QPROV_TYPE_QPU_UPDATE + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_CALIBRATION, lastCalibrated.toString(),
                        Constants.QPROV_TYPE_QPU_CALIBRATION + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_QUEUE, String.valueOf(queueSize),
                        Constants.QPROV_TYPE_QPU_QUEUE + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_MAX_SHOTS, String.valueOf(maxShots),
                        Constants.QPROV_TYPE_QPU_MAX_SHOTS + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_VERSION, version,
                        Constants.QPROV_TYPE_QPU_VERSION + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QPU_SIMULATOR, String.valueOf(isSimulator),
                        Constants.QPROV_TYPE_QPU_SIMULATOR + Constants.QPROV_TYPE_SUFFIX));

        // add data about contained qubits
        final Set<Statement> statements = qubits.stream().flatMap(qubit -> qubit.toStandardCompliantProv(qubit).stream()).collect(Collectors.toSet());
        statements.add(agent);

        return statements;
    }
}
