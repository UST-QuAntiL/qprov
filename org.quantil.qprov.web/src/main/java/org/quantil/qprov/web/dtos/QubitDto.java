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

package org.quantil.qprov.web.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quantil.qprov.core.model.entities.Qubit;

import java.util.UUID;

/**
 * Data transfer object for Qubits ({@link org.quantil.qprov.core.model.entities.Qubit}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class QubitDto {

    private UUID id;

    private String name;

    public static QubitDto createDTO(Qubit qubit) {
        return new QubitDto(qubit.getDatabaseId(), qubit.getName());
    }
}
