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

import java.util.Date;
import java.util.HashSet;
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
import org.quantil.qprov.core.Utils;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.Qubit;

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

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Provider provider;

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

        // add data about contained qubits
        final Set<Statement> statements = qubits.stream().flatMap(qubit -> qubit.toStandardCompliantProv(qubit).stream()).collect(Collectors.toSet());
        statements.add(agent);

        return statements;
    }
}
