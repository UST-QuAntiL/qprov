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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.utils.Utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class QuantumCircuit extends org.openprovenance.prov.xml.Entity implements ProvExtension<QuantumCircuit> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private String name;

    private int depth;

    private int width;

    private int size;

    private URL codeUrl;

    @Override
    public Set<Statement> toStandardCompliantProv(QuantumCircuit extensionStatement) {
        final org.openprovenance.prov.xml.Entity entity = new org.openprovenance.prov.xml.Entity();
        entity.setId(Utils.generateQualifiedName(name, null));
        entity.getType().add(Utils.createTypeElement(Constants.QPROV_TYPE_QUANTUM_CIRCUIT));
        entity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QUANTUM_CIRCUIT_NAME, name,
                        Constants.QPROV_TYPE_QUANTUM_CIRCUIT_NAME + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QUANTUM_CIRCUIT_DEPTH, name,
                        Constants.QPROV_TYPE_QUANTUM_CIRCUIT_DEPTH + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QUANTUM_CIRCUIT_WIDTH, name,
                        Constants.QPROV_TYPE_QUANTUM_CIRCUIT_WIDTH + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QUANTUM_CIRCUIT_SIZE, name,
                        Constants.QPROV_TYPE_QUANTUM_CIRCUIT_SIZE + Constants.QPROV_TYPE_SUFFIX));
        entity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_QUANTUM_CIRCUIT_URL, name,
                        Constants.QPROV_TYPE_QUANTUM_CIRCUIT_URL + Constants.QPROV_TYPE_SUFFIX));
        return Stream.of(entity).collect(Collectors.toCollection(HashSet::new));
    }
}
