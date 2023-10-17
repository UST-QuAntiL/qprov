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
import lombok.NoArgsConstructor;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.sql.Document;

import java.util.List;

/**
 * Data transfer object for PROV Documents ({@link org.openprovenance.prov.sql.Document}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvDocumentDto {

    private Long databaseId;

    private Namespace namespace;

    private List<StatementOrBundle> statementOrBundle;

    public static ProvDocumentDto createDTO(Document provDocument) {
        return new ProvDocumentDto(provDocument.getPk(), provDocument.getNamespace(),
                provDocument.getStatementOrBundle());
    }
}
