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

import java.util.List;
import java.util.UUID;

import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.StatementOrBundle;
import org.quantil.qprov.core.model.ProvDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for PROV Documents ({@link org.quantil.qprov.core.model.ProvDocument}).
 */
@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvDocumentDto {

    private UUID databaseId;

    private Namespace namespace;

    private List<StatementOrBundle> statementOrBundle;

    public static ProvDocumentDto createDTO(ProvDocument provDocument) {
        return new ProvDocumentDto(provDocument.getDatabaseId(), provDocument.getNamespace(), provDocument.getStatementOrBundle());
    }

    public static ProvDocument createPROV(ProvDocumentDto provDocumentDto) {
        final ProvDocument provDocument = new ProvDocument();
        provDocument.setDatabaseId(provDocumentDto.getDatabaseId());
        provDocument.setNamespace(provDocumentDto.getNamespace());
        provDocument.getStatementOrBundle().addAll(provDocument.getStatementOrBundle());
        return provDocument;
    }
}
