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

package org.quantil.qprov.core.model.agents;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.model.entities.CalibrationMatrix;
import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.GateCharacteristics;
import org.quantil.qprov.core.model.entities.Qubit;
import org.quantil.qprov.core.model.entities.QubitCharacteristics;
import org.quantil.qprov.core.utils.Utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
     * Return the minimum T1 time from all qubits of the last calibration or null if no calibration data is available
     *
     * @return the minimum T1 time of all qubits, or <code>null</code> if no calibration data is available
     */
    public BigDecimal getMinT1Time() {
        BigDecimal minT1Time = null;
        for (Qubit qubit : qubits) {
            final Optional<QubitCharacteristics> latestCharacteristicsOptional =
                    qubit.getQubitCharacteristics().stream().min(Comparator.comparing(QubitCharacteristics::getCalibrationTime));
            if (latestCharacteristicsOptional.isPresent()) {
                final QubitCharacteristics latestCharacteristics = latestCharacteristicsOptional.get();
                if (Objects.isNull(minT1Time) || latestCharacteristics.getT1Time().compareTo(minT1Time) < 0) {
                    minT1Time = latestCharacteristics.getT1Time();
                }
            }
        }
        return minT1Time;
    }

    /**
     * Return the maximum gate time from all gates of the last calibration or null if no calibration data is available
     *
     * @return the maximum gate time of all gates on all qubits, or <code>null</code> if no calibration data is available
     */
    public BigDecimal getMaximumGateTime() {
        BigDecimal maxGateTime = null;
        for (Gate gate : gateSet) {
            final Optional<GateCharacteristics> latestCharacteristicsOptional =
                    gate.getGateCharacteristics().stream().min(Comparator.comparing(GateCharacteristics::getCalibrationTime));
            if (latestCharacteristicsOptional.isPresent()) {
                final GateCharacteristics latestCharacteristics = latestCharacteristicsOptional.get();
                if (Objects.isNull(maxGateTime) || latestCharacteristics.getGateTime().compareTo(maxGateTime) > 0) {
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
