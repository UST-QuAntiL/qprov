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
import org.quantil.qprov.core.model.entities.GateCharacteristics;
import org.quantil.qprov.core.model.entities.QubitCharacteristics;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * Data transfer object for qubit characteristics ({@link QubitCharacteristics}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class GateCharacteristicsDto {

    private UUID id;

    private Date calibrationTime;

    private BigDecimal gateTime;

    private BigDecimal gateErrorRate;

    public static GateCharacteristicsDto createDTO(GateCharacteristics gateCharacteristics) {
        return new GateCharacteristicsDto(gateCharacteristics.getDatabaseId(), gateCharacteristics.getCalibrationTime(),
                gateCharacteristics.getGateTime(),
                gateCharacteristics.getGateErrorRate());
    }
}
