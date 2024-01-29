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

package org.quantil.qprov.core.model.activities;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvExtension;
import org.quantil.qprov.core.utils.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class CompileActivity extends org.openprovenance.prov.xml.Activity implements ProvExtension<CompileActivity> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private BigDecimal compilationTime;

    private String optimizationGoal;

    private String randomSeed;

    @Override
    public Set<Statement> toStandardCompliantProv(CompileActivity extensionStatement) {
        final org.openprovenance.prov.xml.Activity activity = new org.openprovenance.prov.xml.Activity();
        activity.setId(Utils.generateQualifiedName(databaseId.toString(), null));
        activity.getType().add(Utils.createTypeElement(Constants.QPROV_TYPE_COMPILE));
        activity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_COMPILATION_TIME, compilationTime.toString(),
                        Constants.QPROV_TYPE_COMPILATION_TIME + Constants.QPROV_TYPE_SUFFIX));
        activity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_COMPILATION_OPTIMIZATION, optimizationGoal,
                        Constants.QPROV_TYPE_COMPILATION_OPTIMIZATION + Constants.QPROV_TYPE_SUFFIX));
        activity.getOther().add(Utils
                .createOtherElement(Constants.QPROV_TYPE_COMPILATION_RANDOM_SEED, randomSeed,
                        Constants.QPROV_TYPE_COMPILATION_RANDOM_SEED + Constants.QPROV_TYPE_SUFFIX));
        return Stream.of(activity).collect(Collectors.toCollection(HashSet::new));
    }
}
