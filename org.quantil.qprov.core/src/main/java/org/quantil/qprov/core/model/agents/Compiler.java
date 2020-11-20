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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.utils.Utils;
import org.quantil.qprov.core.model.ProvExtension;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Compiler extends org.openprovenance.prov.xml.Agent implements ProvExtension<Compiler> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private String name;

    private String providerName;

    private String version;

    @Override
    public Set<Statement> toStandardCompliantProv(Compiler extensionStatement) {
        final org.openprovenance.prov.xml.Agent agent = new org.openprovenance.prov.xml.Agent();
        agent.setId(Utils.generateQualifiedName(databaseId.toString(), null));
        agent.getType().add(Utils.createTypeElement(Constants.QPROV_TYPE_COMPILER));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_COMPILER_NAME, name,
                        Constants.QPROV_TYPE_COMPILER_NAME + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_COMPILER_PROVIDER_NAME, providerName,
                        Constants.QPROV_TYPE_COMPILER_PROVIDER_NAME + Constants.QPROV_TYPE_SUFFIX));
        agent.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_COMPILER_VERSION, version,
                        Constants.QPROV_TYPE_COMPILER_VERSION + Constants.QPROV_TYPE_SUFFIX));
        return Stream.of(agent).collect(Collectors.toCollection(HashSet::new));
    }
}
