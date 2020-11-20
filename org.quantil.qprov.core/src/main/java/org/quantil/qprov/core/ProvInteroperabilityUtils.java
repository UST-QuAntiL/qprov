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

package org.quantil.qprov.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.sql.Activity;
import org.openprovenance.prov.sql.Agent;
import org.openprovenance.prov.sql.Bundle;
import org.openprovenance.prov.sql.Document;
import org.openprovenance.prov.sql.Entity;
import org.openprovenance.prov.sql.Namespace;
import org.openprovenance.prov.sql.QualifiedName;
import org.openprovenance.prov.sql.Type;
import org.openprovenance.prov.sql.Value;
import org.quantil.qprov.core.repositories.prov.QualifiedNameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

/**
 * Utility functions to convert PROV documents from the XML or Model package of the PROV toolbox to equivalent documents from the SQL package and in
 * the other direction. This is needed as the import/export is done by serialization to XML and the data has to be stored by entities from the SQL
 * package.
 * <p>
 * This can currently not done by the ModelMapper, as it creates invalid subclasses that are rejected by hibernate on saving the document.
 */
@Component
@AllArgsConstructor
public class ProvInteroperabilityUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProvInteroperabilityUtils.class);

    private final QualifiedNameRepository qualifiedNameRepository;

    /**
     * Parse a given PROV document from the Model package to a document from the SQL package.
     *
     * @param modelDocument the PROV document object from the model package
     * @return the created PROV document object from the SQL package
     */
    public Document createProvSQLDocument(org.openprovenance.prov.model.Document modelDocument) {
        final Document document = new Document();
        document.setNamespace(createProvSQLNamespace(modelDocument.getNamespace()));
        document.setStatementOrBundle(createProvSQLStatements(modelDocument.getStatementOrBundle()));
        return document;
    }

    /**
     * Parse a given PROV document from the Model package to a document from the XML package.
     *
     * @param modelDocument the PROV document object from the model package
     * @return the created PROV document object from the XML package
     */
    public org.openprovenance.prov.xml.Document createProvXMLDocument(org.openprovenance.prov.model.Document modelDocument) {
        final org.openprovenance.prov.xml.Document document = new org.openprovenance.prov.xml.Document();
        document.setNamespace(createProvModelNamespace(modelDocument.getNamespace()));
        document.getStatementOrBundle().addAll(createProvXMLStatements(modelDocument.getStatementOrBundle()));
        return document;
    }

    private Namespace createProvSQLNamespace(org.openprovenance.prov.model.Namespace modelNamespace) {
        final Namespace namespace = new Namespace();
        namespace.setDefaultNamespace(modelNamespace.getDefaultNamespace());
        namespace.setParent(modelNamespace.getParent());
        namespace.setPrefixes(modelNamespace.getPrefixes());
        namespace.setNamespaces(modelNamespace.getNamespaces());
        return namespace;
    }

    private org.openprovenance.prov.model.Namespace createProvModelNamespace(org.openprovenance.prov.model.Namespace modelNamespace) {
        final org.openprovenance.prov.model.Namespace namespace = new org.openprovenance.prov.model.Namespace();
        namespace.setDefaultNamespace(modelNamespace.getDefaultNamespace());
        namespace.setParent(modelNamespace.getParent());
        namespace.getPrefixes().putAll(modelNamespace.getPrefixes());
        namespace.getNamespaces().putAll(modelNamespace.getNamespaces());
        return namespace;
    }

    private List<StatementOrBundle> createProvSQLStatements(List<StatementOrBundle> modelStatementOrBundles) {
        final List<StatementOrBundle> statementOrBundles = new ArrayList<>();
        for (StatementOrBundle modelStatementOrBundle : modelStatementOrBundles) {
            switch (modelStatementOrBundle.getKind()) {
                case PROV_ENTITY:
                    statementOrBundles.add(createProvSQLEntities((org.openprovenance.prov.model.Entity) modelStatementOrBundle));
                    break;
                case PROV_ACTIVITY:
                    statementOrBundles.add(createProvSQLActivity((org.openprovenance.prov.model.Activity) modelStatementOrBundle));
                    break;
                case PROV_AGENT:
                    statementOrBundles.add(createProvSQLAgent((org.openprovenance.prov.model.Agent) modelStatementOrBundle));
                    break;
                case PROV_BUNDLE:
                    statementOrBundles.add(createProvSQLBundle((org.openprovenance.prov.model.Bundle) modelStatementOrBundle));
                    break;
                default:
                    logger.warn("PROV document contains elements that can currently not be parsed. Ignoring them.");
            }
        }
        return statementOrBundles;
    }

    private List<StatementOrBundle> createProvXMLStatements(List<StatementOrBundle> modelStatementOrBundles) {
        final List<StatementOrBundle> statementOrBundles = new ArrayList<>();
        for (StatementOrBundle modelStatementOrBundle : modelStatementOrBundles) {
            switch (modelStatementOrBundle.getKind()) {
                case PROV_ENTITY:
                    statementOrBundles.add(createProvXMLEntities((org.openprovenance.prov.model.Entity) modelStatementOrBundle));
                    break;
                case PROV_ACTIVITY:
                    statementOrBundles.add(createProvXMLActivity((org.openprovenance.prov.model.Activity) modelStatementOrBundle));
                    break;
                case PROV_AGENT:
                    statementOrBundles.add(createProvXMLAgent((org.openprovenance.prov.model.Agent) modelStatementOrBundle));
                    break;
                case PROV_BUNDLE:
                    statementOrBundles.add(createProvXMLBundle((org.openprovenance.prov.model.Bundle) modelStatementOrBundle));
                    break;
                default:
                    logger.warn("PROV document contains elements that can currently not be parsed. Ignoring them.");
            }
        }
        return statementOrBundles;
    }

    private QualifiedName createProvSQLQualifiedName(org.openprovenance.prov.model.QualifiedName modelQualifiedName) {
        // reuse existing qualified name
        final Optional<QualifiedName> qualifiedNameOptional = qualifiedNameRepository.findByUri(modelQualifiedName.getUri());
        if (qualifiedNameOptional.isPresent()) {
            return qualifiedNameOptional.get();
        }

        QualifiedName qualifiedName = new QualifiedName();
        qualifiedName.setLocalPart(modelQualifiedName.getLocalPart());
        qualifiedName.setNamespaceURI(modelQualifiedName.getNamespaceURI());
        qualifiedName.setPrefix(modelQualifiedName.getPrefix());
        qualifiedName.setUri(modelQualifiedName.getUri());
        qualifiedName = qualifiedNameRepository.save(qualifiedName);
        return qualifiedName;
    }

    private Value createProvSQLValue(org.openprovenance.prov.model.Value modelValue) {
        final Value value = new Value();
        value.setType(createProvSQLQualifiedName(modelValue.getType()));
        if (modelValue.getValue() instanceof org.openprovenance.prov.model.QualifiedName) {
            final org.openprovenance.prov.model.QualifiedName qualifiedName = (org.openprovenance.prov.model.QualifiedName) modelValue.getValue();
            value.setValue(createProvSQLQualifiedName(qualifiedName));
        } else {
            value.setValue(modelValue.getValue().toString());
        }
        return value;
    }

    private Type createProvSQLType(org.openprovenance.prov.model.Type modelType) {
        final Type type = new Type();
        type.setType(createProvSQLQualifiedName(modelType.getType()));
        if (modelType.getValue() instanceof org.openprovenance.prov.model.QualifiedName) {
            final org.openprovenance.prov.model.QualifiedName qualifiedName = (org.openprovenance.prov.model.QualifiedName) modelType.getValue();
            type.setValue(createProvSQLQualifiedName(qualifiedName));
        } else {
            type.setValue(modelType.getValue().toString());
        }
        return type;
    }

    private Entity createProvSQLEntities(org.openprovenance.prov.model.Entity modelEntity) {
        final Entity entity = new Entity();
        entity.setId(createProvSQLQualifiedName(modelEntity.getId()));
        if (Objects.nonNull(modelEntity.getValue())) {
            entity.setValue(createProvSQLValue(modelEntity.getValue()));
        }

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelEntity.getType()) {
            types.add(createProvSQLType(modelType));
        }
        entity.setType(types);
        logger.debug("Number of types: {}", types.size());

        //entity.set

        // TODO
        return entity;
    }

    private org.openprovenance.prov.xml.Entity createProvXMLEntities(org.openprovenance.prov.model.Entity modelEntity) {
        final org.openprovenance.prov.xml.Entity entity = new org.openprovenance.prov.xml.Entity();
        entity.setId(modelEntity.getId());
        if (Objects.nonNull(modelEntity.getValue())) {
            entity.setValue(modelEntity.getValue());
        }

        for (org.openprovenance.prov.model.Type modelType : modelEntity.getType()) {
            entity.getType().add(createProvSQLType(modelType));
        }
        logger.debug("Number of types: {}", entity.getType().size());

        // TODO
        return entity;
    }

    private Activity createProvSQLActivity(org.openprovenance.prov.model.Activity modelActivity) {
        final Activity activity = new Activity();
        activity.setId(createProvSQLQualifiedName(modelActivity.getId()));
        // TODO
        return activity;
    }

    private org.openprovenance.prov.xml.Activity createProvXMLActivity(org.openprovenance.prov.model.Activity modelActivity) {
        final org.openprovenance.prov.xml.Activity activity = new org.openprovenance.prov.xml.Activity();
        activity.setId(createProvSQLQualifiedName(modelActivity.getId()));
        // TODO
        return activity;
    }

    private Agent createProvSQLAgent(org.openprovenance.prov.model.Agent modelAgent) {
        final Agent agent = new Agent();
        agent.setId(createProvSQLQualifiedName(modelAgent.getId()));
        // TODO
        return agent;
    }

    private org.openprovenance.prov.xml.Agent createProvXMLAgent(org.openprovenance.prov.model.Agent modelAgent) {
        final org.openprovenance.prov.xml.Agent agent = new org.openprovenance.prov.xml.Agent();
        agent.setId(createProvSQLQualifiedName(modelAgent.getId()));
        // TODO
        return agent;
    }

    private Bundle createProvSQLBundle(org.openprovenance.prov.model.Bundle modelBundle) {
        final Bundle bundle = new Bundle();
        bundle.setId(createProvSQLQualifiedName(modelBundle.getId()));
        // TODO
        return bundle;
    }

    private org.openprovenance.prov.xml.Bundle createProvXMLBundle(org.openprovenance.prov.model.Bundle modelBundle) {
        final org.openprovenance.prov.xml.Bundle bundle = new org.openprovenance.prov.xml.Bundle();
        bundle.setId(createProvSQLQualifiedName(modelBundle.getId()));
        // TODO
        return bundle;
    }
}
