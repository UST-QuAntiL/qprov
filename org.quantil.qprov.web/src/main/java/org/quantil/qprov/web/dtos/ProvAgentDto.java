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

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.sql.Agent;

@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvAgentDto {

    private QualifiedName id;

    private List<LangString> label;

    private List<Location> location;

    private List<Type> type;

    private List<Attribute> attributes;

    private List<Other> others;

    public static ProvAgentDto createDTO(Agent agent) {
        return new ProvAgentDto(agent.getId(), agent.getLabel(), agent.getLocation(), agent.getType(), agent.getAllAttributes(), agent.getOther());
    }
}
