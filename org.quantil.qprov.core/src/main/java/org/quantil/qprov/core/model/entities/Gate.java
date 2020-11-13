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

package org.quantil.qprov.core.model.entities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.model.agents.QPU;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Gate extends org.openprovenance.prov.xml.Entity implements ProvExtension<Gate> {

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

    @OneToMany(mappedBy = "gate",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GateCharacteristics> gateCharacteristics = new HashSet<>();

    @ManyToMany(mappedBy = "supportedGates",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @EqualsAndHashCode.Exclude
    private Set<Qubit> operatingQubits = new HashSet<>();

    @Override
    public Set<Statement> toStandardCompliantProv(Gate extensionStatement) {
        //TODO
        return null;
    }

    public void addOperatingQubit(@NonNull Qubit qubit) {
        if (operatingQubits.contains(qubit)) {
            return;
        }
        operatingQubits.add(qubit);
        qubit.addSupportedGate(this);
    }

    public void removeOperatingQubit(@NonNull Qubit qubit) {
        if (!operatingQubits.contains(qubit)) {
            return;
        }
        operatingQubits.remove(qubit);
        qubit.removeSupportedGate(this);
    }
}
