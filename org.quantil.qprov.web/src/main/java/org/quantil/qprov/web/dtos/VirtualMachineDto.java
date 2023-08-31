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

package org.quantil.qprov.web.dtos;

import java.util.UUID;

import org.quantil.qprov.core.model.agents.VirtualMachine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for VirtualMachines ({@link org.quantil.qprov.core.model.agents.VirtualMachine}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class VirtualMachineDto {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    private String name;

    private String cpu;

    private int cpuCores;

    private int ramSize;

    private int diskSize;

    public static VirtualMachineDto createDTO(VirtualMachine virtualMachine) {
        return new VirtualMachineDto(virtualMachine.getDatabaseId(), virtualMachine.getName(), virtualMachine.getCpu(),
                virtualMachine.getCpuCores(), virtualMachine.getRamSize(), virtualMachine.getDiskSize());
    }
}
