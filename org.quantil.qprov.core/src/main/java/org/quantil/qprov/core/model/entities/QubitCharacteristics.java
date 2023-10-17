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

package org.quantil.qprov.core.model.entities;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import org.quantil.qprov.core.Constants;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Characteristics of a qubit at a certain calibration time
 */
@EqualsAndHashCode
@Data
@Entity
public class QubitCharacteristics implements Comparable<QubitCharacteristics> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private Date calibrationTime;

    @Column(precision = Constants.BIG_DECIMAL_PRECISION, scale = Constants.BIG_DECIMAL_SCALE)
    private BigDecimal t1Time;

    @Column(precision = Constants.BIG_DECIMAL_PRECISION, scale = Constants.BIG_DECIMAL_SCALE)
    private BigDecimal t2Time;

    @Column(precision = Constants.BIG_DECIMAL_PRECISION, scale = Constants.BIG_DECIMAL_SCALE)
    private BigDecimal readoutError;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Qubit qubit;

    @Override
    public int compareTo(@NotNull QubitCharacteristics o) {
        return getCalibrationTime().compareTo(o.getCalibrationTime());
    }
}
