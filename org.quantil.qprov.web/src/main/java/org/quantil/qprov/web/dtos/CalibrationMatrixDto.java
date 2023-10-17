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

import java.util.Date;
import java.util.UUID;
import java.util.Vector;

import org.quantil.qprov.core.model.entities.CalibrationMatrix;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object for Calibration Matrices ({@link org.quantil.qprov.core.model.entities.CalibrationMatrix}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class CalibrationMatrixDto {

    private UUID id;

    private Date calibrationTime;

    private Vector<Vector<Double>> calibrationMatrix;

    public static CalibrationMatrixDto createDTO(CalibrationMatrix calibrationMatrix) {
        return new CalibrationMatrixDto(calibrationMatrix.getDatabaseId(), calibrationMatrix.getCalibrationTime(),
                calibrationMatrix.getCalibrationMatrix());
    }
}
