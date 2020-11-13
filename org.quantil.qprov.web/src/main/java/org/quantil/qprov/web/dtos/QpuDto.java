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

import java.util.Date;

import org.quantil.qprov.core.model.agents.QPU;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for QPUs ({@link org.quantil.qprov.core.model.agents.QPU}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class QpuDto {

    private String name;

    private String version;

    private Date lastUpdated;

    private Date lastCalibrated;

    private int maxShots;

    private int queueSize;

    private int numberOfQubits;

    public static QpuDto createDTO(QPU qpu) {
        return new QpuDto(qpu.getName(), qpu.getVersion(), qpu.getLastUpdated(), qpu.getLastCalibrated(), qpu.getMaxShots(), qpu.getQueueSize(),
                qpu.getQubits().size());
    }
}
