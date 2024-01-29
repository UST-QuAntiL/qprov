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

package org.quantil.qprov.web.dtos;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.quantil.qprov.core.model.entities.Gate;
import org.quantil.qprov.core.model.entities.Qubit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for Gates ({@link org.quantil.qprov.core.model.entities.Gate}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class GateDto {

    private UUID id;

    private String name;

    private boolean multiQubitGate;

    private List<UUID> operatingQubits;

    public static GateDto createDTO(Gate gate) {
        return new GateDto(gate.getDatabaseId(), gate.getName(), gate.getOperatingQubits().size() > 1,
                gate.getOperatingQubits().stream().map(Qubit::getDatabaseId).collect(Collectors.toList()));
    }
}
