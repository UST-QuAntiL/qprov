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

package org.quantil.qprov.core.model.entities;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.model.agents.QPU;
import org.quantil.qprov.core.utils.Utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Qubit extends org.openprovenance.prov.xml.Entity implements ProvExtension<Qubit> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private String name;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private QPU qpu;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "qubit_connectivity",
            joinColumns = @JoinColumn(name = "qubit1"),
            inverseJoinColumns = @JoinColumn(name = "qubit2"))
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Qubit> connectedQubits = new HashSet<>();

    @OneToMany(mappedBy = "qubit",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<QubitCharacteristics> qubitCharacteristics = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "qubits_gates",
            joinColumns = @JoinColumn(name = "qubit_id"),
            inverseJoinColumns = @JoinColumn(name = "gate_id")
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Gate> supportedGates = new HashSet<>();

    @Override
    public Set<Statement> toStandardCompliantProv(Qubit qubit) {
        final org.openprovenance.prov.xml.Entity entity = new org.openprovenance.prov.xml.Entity();

        // add Qubit specific attributes
        entity.setId(Utils.generateQualifiedName(databaseId.toString(), null));
        entity.getType().add(Utils.createTypeElement(Constants.QPROV_TYPE_QUBIT));
        entity.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_QUBIT_NAME, name,
                Constants.QPROV_TYPE_QUBIT_NAME + Constants.QPROV_TYPE_SUFFIX));

        // add set of names from connected qubits
        entity.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_QUBIT_CONNECTED_QUBITS,
                connectedQubits.stream().map(Qubit::getName).collect(Collectors.joining(",")),
                Constants.QPROV_TYPE_QUBIT_CONNECTED_QUBITS + Constants.QPROV_TYPE_SUFFIX));

        // add latest calibration data
        String calibrationTime = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String t1 = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String t2 = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        String readoutError = Constants.QPROV_CHARACTERISTICS_NO_DATA;
        final Optional<QubitCharacteristics> currentCharacteristicsOptional = qubit.getQubitCharacteristics().stream()
                .min(Comparator.comparing(QubitCharacteristics::getCalibrationTime));
        if (currentCharacteristicsOptional.isPresent()) {
            // update with latest calibration data
            final QubitCharacteristics currentCharacteristics = currentCharacteristicsOptional.get();
            calibrationTime = currentCharacteristics.getCalibrationTime().toString();
            t1 = currentCharacteristics.getT1Time().toString();
            t2 = currentCharacteristics.getT2Time().toString();
            readoutError = currentCharacteristics.getReadoutError().toString();
        }
        entity.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_QUBIT_CALIBRATION_TIME,
                calibrationTime, Constants.QPROV_TYPE_QUBIT_CALIBRATION_TIME + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_QUBIT_T1,
                t1, Constants.QPROV_TYPE_QUBIT_T1 + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_QUBIT_T2,
                t2, Constants.QPROV_TYPE_QUBIT_T2 + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils.createOtherElement(Constants.QPROV_TYPE_QUBIT_READOUT_ERROR,
                readoutError, Constants.QPROV_TYPE_QUBIT_READOUT_ERROR + Constants.QPROV_TYPE_SUFFIX));

        // add data about gates on the qubit
        final Set<Statement> statements =
                supportedGates.stream().flatMap(gate -> gate.toStandardCompliantProv(gate).stream()).collect(Collectors.toSet());
        statements.add(entity);

        return statements;
    }

    public void addSupportedGate(@NonNull Gate gate) {
        if (supportedGates.contains(gate)) {
            return;
        }
        supportedGates.add(gate);
        gate.addOperatingQubit(this);
    }

    public void removeSupportedGate(@NonNull Gate gate) {
        if (!supportedGates.contains(gate)) {
            return;
        }
        supportedGates.remove(gate);
        gate.removeOperatingQubit(this);
    }
}
