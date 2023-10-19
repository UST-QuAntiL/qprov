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
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openprovenance.prov.model.Attribute;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.Location;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.Type;
import org.openprovenance.prov.sql.Activity;

@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvActivityDto {

    private QualifiedName id;

    private List<LangString> label;

    private List<Location> location;

    private List<Type> type;

    private List<Attribute> attributes;

    private List<Other> others;

    private Date startTime;

    private Date endTime;

    public static ProvActivityDto createDTO(Activity activity) {
        return new ProvActivityDto(activity.getId(), activity.getLabel(), activity.getLocation(), activity.getType(), activity.getAllAttributes(),
                activity.getOther(), activity.getStartTimeItem(), activity.getEndTimeItem());
    }
}
