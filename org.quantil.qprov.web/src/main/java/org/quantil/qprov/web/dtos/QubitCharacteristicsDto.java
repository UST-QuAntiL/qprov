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

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.quantil.qprov.core.model.entities.QubitCharacteristics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for qubit characteristics ({@link org.quantil.qprov.core.model.entities.QubitCharacteristics}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class QubitCharacteristicsDto {

    private UUID id;

    private Date calibrationTime;

    private BigDecimal t1Time;

    private BigDecimal t2Time;

    private BigDecimal readoutError;

    public static QubitCharacteristicsDto createDTO(QubitCharacteristics qubitCharacteristics) {
        return new QubitCharacteristicsDto(qubitCharacteristics.getDatabaseId(), qubitCharacteristics.getCalibrationTime(),
                qubitCharacteristics.getT1Time(), qubitCharacteristics.getT2Time(), qubitCharacteristics.getReadoutError());
    }
}
