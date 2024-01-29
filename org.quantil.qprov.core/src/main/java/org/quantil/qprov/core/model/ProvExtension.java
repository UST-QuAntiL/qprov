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

package org.quantil.qprov.core.model;

import java.util.Set;

import org.openprovenance.prov.model.Statement;

/**
 * Interface to define PROV extensions that can be transformed into standard-compliant PROV
 */
public interface ProvExtension<T> {

    /**
     * Transform the given PROV extension statement to a set of standard-compliant PROV statements to enable the export of standard-compliant PROV
     * graphs
     *
     * @param extensionStatement the PROV statement using extensions
     * @return a set of standard-compliant PROV statements
     */
    Set<Statement> toStandardCompliantProv(T extensionStatement);
}
