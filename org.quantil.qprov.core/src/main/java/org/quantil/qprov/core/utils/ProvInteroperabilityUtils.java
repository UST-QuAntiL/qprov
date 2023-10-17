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

package org.quantil.qprov.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.quantil.qprov.core.model.prov.ProvQualifiedName;
import org.quantil.qprov.core.repositories.prov.QualifiedNameRepository;

import lombok.AllArgsConstructor;
import org.openprovenance.prov.model.LangString;
import org.openprovenance.prov.model.Statement;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.sql.Activity;
import org.openprovenance.prov.sql.Agent;
import org.openprovenance.prov.sql.Bundle;
import org.openprovenance.prov.sql.Document;
import org.openprovenance.prov.sql.Entity;
import org.openprovenance.prov.sql.InternationalizedString;
import org.openprovenance.prov.sql.Location;
import org.openprovenance.prov.sql.Namespace;
import org.openprovenance.prov.sql.Other;
import org.openprovenance.prov.sql.Type;
import org.openprovenance.prov.sql.Used;
import org.openprovenance.prov.sql.Value;
import org.openprovenance.prov.sql.WasAssociatedWith;
import org.openprovenance.prov.sql.WasAttributedTo;
import org.openprovenance.prov.sql.WasGeneratedBy;
import org.openprovenance.prov.sql.WasInfluencedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
                    statementOrBundles.add(createProvSQLEntity((org.openprovenance.prov.model.Entity) modelStatementOrBundle));
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
                case PROV_ATTRIBUTION:
                    statementOrBundles.add(createProvSQLAttribution((org.openprovenance.prov.model.WasAttributedTo) modelStatementOrBundle));
                    break;
                case PROV_ASSOCIATION:
                    statementOrBundles.add(createProvSQLAssociation((org.openprovenance.prov.model.WasAssociatedWith) modelStatementOrBundle));
                    break;
                case PROV_GENERATION:
                    statementOrBundles.add(createProvSQLGeneration((org.openprovenance.prov.model.WasGeneratedBy) modelStatementOrBundle));
                    break;
                case PROV_USAGE:
                    statementOrBundles.add(createProvSQLUsed((org.openprovenance.prov.model.Used) modelStatementOrBundle));
                    break;
                case PROV_INFLUENCE:
                    statementOrBundles.add(createProvSQLWasInfluencedBy((org.openprovenance.prov.model.WasInfluencedBy) modelStatementOrBundle));
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
                    statementOrBundles.add(createProvXMLEntity((org.openprovenance.prov.model.Entity) modelStatementOrBundle));
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
                case PROV_ATTRIBUTION:
                    statementOrBundles.add(createProvXMLAttribution((org.openprovenance.prov.model.WasAttributedTo) modelStatementOrBundle));
                    break;
                case PROV_ASSOCIATION:
                    statementOrBundles.add(createProvXMLAssociation((org.openprovenance.prov.model.WasAssociatedWith) modelStatementOrBundle));
                    break;
                case PROV_GENERATION:
                    statementOrBundles.add(createProvXMLGeneration((org.openprovenance.prov.model.WasGeneratedBy) modelStatementOrBundle));
                    break;
                case PROV_USAGE:
                    statementOrBundles.add(createProvXMLUsed((org.openprovenance.prov.model.Used) modelStatementOrBundle));
                    break;
                case PROV_INFLUENCE:
                    statementOrBundles.add(createProvXMLWasInfluencedBy((org.openprovenance.prov.model.WasInfluencedBy) modelStatementOrBundle));
                    break;
                default:
                    logger.warn("PROV document contains elements that can currently not be parsed. Ignoring them.");
            }
        }
        return statementOrBundles;
    }

    private ProvQualifiedName createProvSQLQualifiedName(org.openprovenance.prov.model.QualifiedName modelQualifiedName) {
        if (Objects.isNull(modelQualifiedName)) {
            return null;
        }

        // reuse existing qualified name
        final Optional<ProvQualifiedName> qualifiedNameOptional = qualifiedNameRepository.findByUri(modelQualifiedName.getUri());
        if (qualifiedNameOptional.isPresent()) {
            return qualifiedNameOptional.get();
        }

        ProvQualifiedName qualifiedName = new ProvQualifiedName();
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

    private Other createProvSQLOther(org.openprovenance.prov.model.Other modelOther) {
        final Other other = new Other();
        other.setElementName(createProvSQLQualifiedName(modelOther.getElementName()));
        other.setType(createProvSQLQualifiedName(modelOther.getType()));
        if (modelOther.getValue() instanceof org.openprovenance.prov.model.QualifiedName) {
            final org.openprovenance.prov.model.QualifiedName qualifiedName = (org.openprovenance.prov.model.QualifiedName) modelOther.getValue();
            other.setValue(createProvSQLQualifiedName(qualifiedName));
        } else {
            other.setValue(modelOther.getValue().toString());
        }
        return other;
    }

    private Location createProvSQLLocation(org.openprovenance.prov.model.Location modelLocation) {
        final Location location = new Location();
        location.setType(createProvSQLQualifiedName(modelLocation.getType()));
        if (modelLocation.getValue() instanceof org.openprovenance.prov.model.QualifiedName) {
            final org.openprovenance.prov.model.QualifiedName qualifiedName = (org.openprovenance.prov.model.QualifiedName) modelLocation.getValue();
            location.setValue(createProvSQLQualifiedName(qualifiedName));
        } else {
            location.setValue(modelLocation.getValue().toString());
        }
        return location;
    }

    private LangString createProvSQLLabel(org.openprovenance.prov.model.LangString modelLabel) {
        final LangString langString = new InternationalizedString();
        langString.setLang(modelLabel.getLang());
        langString.setValue(modelLabel.getValue());
        return langString;
    }

    private LangString createProvXMLLabel(org.openprovenance.prov.model.LangString modelLabel) {
        final LangString langString = new org.openprovenance.prov.xml.InternationalizedString();
        langString.setLang(modelLabel.getLang());
        langString.setValue(modelLabel.getValue());
        return langString;
    }

    private Entity createProvSQLEntity(org.openprovenance.prov.model.Entity modelEntity) {
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

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelEntity.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        entity.setOther(others);

        final List<org.openprovenance.prov.model.Location> locations = new ArrayList<>();
        for (org.openprovenance.prov.model.Location modelLocation : modelEntity.getLocation()) {
            locations.add(createProvSQLLocation(modelLocation));
        }
        entity.setLocation(locations);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelEntity.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        entity.setLabel(labels);

        return entity;
    }

    private org.openprovenance.prov.xml.Entity createProvXMLEntity(org.openprovenance.prov.model.Entity modelEntity) {
        final org.openprovenance.prov.xml.Entity entity = new org.openprovenance.prov.xml.Entity();
        entity.setId(modelEntity.getId());
        entity.setValue(modelEntity.getValue());
        entity.getType().addAll(modelEntity.getType());
        entity.getOther().addAll(modelEntity.getOther());
        entity.getLocation().addAll(modelEntity.getLocation());
        for (org.openprovenance.prov.model.LangString modelLabel : modelEntity.getLabel()) {
            entity.getLabel().add(createProvXMLLabel(modelLabel));
        }

        return entity;
    }

    private Activity createProvSQLActivity(org.openprovenance.prov.model.Activity modelActivity) {
        final Activity activity = new Activity();
        activity.setId(createProvSQLQualifiedName(modelActivity.getId()));

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelActivity.getType()) {
            types.add(createProvSQLType(modelType));
        }
        activity.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelActivity.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        activity.setOther(others);

        final List<org.openprovenance.prov.model.Location> locations = new ArrayList<>();
        for (org.openprovenance.prov.model.Location modelLocation : modelActivity.getLocation()) {
            locations.add(createProvSQLLocation(modelLocation));
        }
        activity.setLocation(locations);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelActivity.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        activity.setLabel(labels);

        activity.setStartTime(modelActivity.getStartTime());
        activity.setEndTime(modelActivity.getEndTime());

        return activity;
    }

    private org.openprovenance.prov.xml.Activity createProvXMLActivity(org.openprovenance.prov.model.Activity modelActivity) {
        final org.openprovenance.prov.xml.Activity activity = new org.openprovenance.prov.xml.Activity();
        activity.setId(createProvSQLQualifiedName(modelActivity.getId()));
        activity.getType().addAll(modelActivity.getType());
        activity.getOther().addAll(modelActivity.getOther());
        activity.getLocation().addAll(modelActivity.getLocation());
        for (org.openprovenance.prov.model.LangString modelLabel : modelActivity.getLabel()) {
            activity.getLabel().add(createProvXMLLabel(modelLabel));
        }

        activity.setStartTime(modelActivity.getStartTime());
        activity.setEndTime(modelActivity.getEndTime());

        return activity;
    }

    private Agent createProvSQLAgent(org.openprovenance.prov.model.Agent modelAgent) {
        final Agent agent = new Agent();
        agent.setId(createProvSQLQualifiedName(modelAgent.getId()));

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelAgent.getType()) {
            types.add(createProvSQLType(modelType));
        }
        agent.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelAgent.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        agent.setOther(others);

        final List<org.openprovenance.prov.model.Location> locations = new ArrayList<>();
        for (org.openprovenance.prov.model.Location modelLocation : modelAgent.getLocation()) {
            locations.add(createProvSQLLocation(modelLocation));
        }
        agent.setLocation(locations);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelAgent.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        agent.setLabel(labels);

        return agent;
    }

    private org.openprovenance.prov.xml.Agent createProvXMLAgent(org.openprovenance.prov.model.Agent modelAgent) {
        final org.openprovenance.prov.xml.Agent agent = new org.openprovenance.prov.xml.Agent();
        agent.setId(createProvSQLQualifiedName(modelAgent.getId()));
        agent.getType().addAll(modelAgent.getType());
        agent.getOther().addAll(modelAgent.getOther());
        agent.getLocation().addAll(modelAgent.getLocation());
        for (org.openprovenance.prov.model.LangString modelLabel : modelAgent.getLabel()) {
            agent.getLabel().add(createProvXMLLabel(modelLabel));
        }
        return agent;
    }

    private Bundle createProvSQLBundle(org.openprovenance.prov.model.Bundle modelBundle) {
        final Bundle bundle = new Bundle();
        bundle.setId(createProvSQLQualifiedName(modelBundle.getId()));

        final List<StatementOrBundle> transformedStatementOrBundles = createProvSQLStatements(
                modelBundle.getStatement().stream().map(statement -> (StatementOrBundle) statement).collect(Collectors.toList()));
        bundle.setStatement(transformedStatementOrBundles.stream()
                .map(statementOrBundle -> (Statement) statementOrBundle).collect(Collectors.toList()));
        return bundle;
    }

    private org.openprovenance.prov.xml.Bundle createProvXMLBundle(org.openprovenance.prov.model.Bundle modelBundle) {
        final org.openprovenance.prov.xml.Bundle bundle = new org.openprovenance.prov.xml.Bundle();
        bundle.setId(modelBundle.getId());

        final List<StatementOrBundle> transformedStatementOrBundles = createProvXMLStatements(
                modelBundle.getStatement().stream().map(statement -> (StatementOrBundle) statement).collect(Collectors.toList()));
        bundle.getStatement().addAll(transformedStatementOrBundles.stream()
                .map(statementOrBundle -> (Statement) statementOrBundle).collect(Collectors.toList()));
        return bundle;
    }

    private WasAttributedTo createProvSQLAttribution(org.openprovenance.prov.model.WasAttributedTo modelWasAttributedTo) {
        final WasAttributedTo wasAttributedTo = new WasAttributedTo();
        wasAttributedTo.setId(createProvSQLQualifiedName(modelWasAttributedTo.getId()));
        wasAttributedTo.setAgent(createProvSQLQualifiedName(modelWasAttributedTo.getAgent()));
        wasAttributedTo.setEntity(createProvSQLQualifiedName(modelWasAttributedTo.getEntity()));

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelWasAttributedTo.getType()) {
            types.add(createProvSQLType(modelType));
        }
        wasAttributedTo.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelWasAttributedTo.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        wasAttributedTo.setOther(others);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasAttributedTo.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        wasAttributedTo.setLabel(labels);

        return wasAttributedTo;
    }

    private org.openprovenance.prov.xml.WasAttributedTo createProvXMLAttribution(org.openprovenance.prov.model.WasAttributedTo modelWasAttributedTo) {
        final org.openprovenance.prov.xml.WasAttributedTo wasAttributedTo = new org.openprovenance.prov.xml.WasAttributedTo();
        wasAttributedTo.setId(modelWasAttributedTo.getId());
        wasAttributedTo.setAgent(modelWasAttributedTo.getAgent());
        wasAttributedTo.setEntity(modelWasAttributedTo.getEntity());
        wasAttributedTo.getType().addAll(modelWasAttributedTo.getType());
        wasAttributedTo.getOther().addAll(modelWasAttributedTo.getOther());
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasAttributedTo.getLabel()) {
            wasAttributedTo.getLabel().add(createProvXMLLabel(modelLabel));
        }
        return wasAttributedTo;
    }

    private WasAssociatedWith createProvSQLAssociation(org.openprovenance.prov.model.WasAssociatedWith modelWasAssociatedWith) {
        final WasAssociatedWith wasAssociatedWith = new WasAssociatedWith();
        wasAssociatedWith.setId(createProvSQLQualifiedName(modelWasAssociatedWith.getId()));
        wasAssociatedWith.setAgent(createProvSQLQualifiedName(modelWasAssociatedWith.getAgent()));
        wasAssociatedWith.setActivity(createProvSQLQualifiedName(modelWasAssociatedWith.getActivity()));
        wasAssociatedWith.setPlan(createProvSQLQualifiedName(modelWasAssociatedWith.getPlan()));

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelWasAssociatedWith.getType()) {
            types.add(createProvSQLType(modelType));
        }
        wasAssociatedWith.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelWasAssociatedWith.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        wasAssociatedWith.setOther(others);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasAssociatedWith.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        wasAssociatedWith.setLabel(labels);

        return wasAssociatedWith;
    }

    private org.openprovenance.prov.xml.WasAssociatedWith createProvXMLAssociation(
            org.openprovenance.prov.model.WasAssociatedWith modelWasAssociatedWith) {
        final org.openprovenance.prov.xml.WasAssociatedWith wasAssociatedWith = new org.openprovenance.prov.xml.WasAssociatedWith();
        wasAssociatedWith.setId(modelWasAssociatedWith.getId());
        wasAssociatedWith.setAgent(modelWasAssociatedWith.getAgent());
        wasAssociatedWith.setActivity(modelWasAssociatedWith.getActivity());
        wasAssociatedWith.setPlan(wasAssociatedWith.getPlan());
        wasAssociatedWith.getType().addAll(modelWasAssociatedWith.getType());
        wasAssociatedWith.getOther().addAll(modelWasAssociatedWith.getOther());
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasAssociatedWith.getLabel()) {
            wasAssociatedWith.getLabel().add(createProvXMLLabel(modelLabel));
        }
        return wasAssociatedWith;
    }

    private org.openprovenance.prov.xml.WasGeneratedBy createProvXMLGeneration(org.openprovenance.prov.model.WasGeneratedBy modelWasGeneratedBy) {
        final org.openprovenance.prov.xml.WasGeneratedBy wasGeneratedBy = new org.openprovenance.prov.xml.WasGeneratedBy();
        wasGeneratedBy.setId(modelWasGeneratedBy.getId());
        wasGeneratedBy.setEntity(modelWasGeneratedBy.getEntity());
        wasGeneratedBy.setActivity(modelWasGeneratedBy.getActivity());
        wasGeneratedBy.setTime(modelWasGeneratedBy.getTime());
        wasGeneratedBy.getType().addAll(modelWasGeneratedBy.getType());
        wasGeneratedBy.getOther().addAll(modelWasGeneratedBy.getOther());
        wasGeneratedBy.getLocation().addAll(modelWasGeneratedBy.getLocation());
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasGeneratedBy.getLabel()) {
            wasGeneratedBy.getLabel().add(createProvXMLLabel(modelLabel));
        }
        return wasGeneratedBy;
    }

    private WasGeneratedBy createProvSQLGeneration(org.openprovenance.prov.model.WasGeneratedBy modelWasGeneratedBy) {
        final WasGeneratedBy wasGeneratedBy = new WasGeneratedBy();
        wasGeneratedBy.setId(createProvSQLQualifiedName(modelWasGeneratedBy.getId()));
        wasGeneratedBy.setEntity(createProvSQLQualifiedName(modelWasGeneratedBy.getEntity()));
        wasGeneratedBy.setActivity(createProvSQLQualifiedName(modelWasGeneratedBy.getActivity()));
        wasGeneratedBy.setTime(modelWasGeneratedBy.getTime());

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelWasGeneratedBy.getType()) {
            types.add(createProvSQLType(modelType));
        }
        wasGeneratedBy.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelWasGeneratedBy.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        wasGeneratedBy.setOther(others);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasGeneratedBy.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        wasGeneratedBy.setLabel(labels);

        final List<org.openprovenance.prov.model.Location> locations = new ArrayList<>();
        for (org.openprovenance.prov.model.Location modelLocation : modelWasGeneratedBy.getLocation()) {
            locations.add(createProvSQLLocation(modelLocation));
        }
        wasGeneratedBy.setLocation(locations);

        return wasGeneratedBy;
    }

    private org.openprovenance.prov.xml.Used createProvXMLUsed(org.openprovenance.prov.model.Used modelUsed) {
        final org.openprovenance.prov.xml.Used used = new org.openprovenance.prov.xml.Used();
        used.setId(modelUsed.getId());
        used.setEntity(modelUsed.getEntity());
        used.setActivity(modelUsed.getActivity());
        used.setTime(modelUsed.getTime());
        used.getType().addAll(modelUsed.getType());
        used.getOther().addAll(modelUsed.getOther());
        used.getLocation().addAll(modelUsed.getLocation());
        for (org.openprovenance.prov.model.LangString modelLabel : modelUsed.getLabel()) {
            used.getLabel().add(createProvXMLLabel(modelLabel));
        }
        return used;
    }

    private Used createProvSQLUsed(org.openprovenance.prov.model.Used modelUsed) {
        final Used used = new Used();
        used.setId(createProvSQLQualifiedName(modelUsed.getId()));
        used.setEntity(createProvSQLQualifiedName(modelUsed.getEntity()));
        used.setActivity(createProvSQLQualifiedName(modelUsed.getActivity()));
        used.setTime(modelUsed.getTime());

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelUsed.getType()) {
            types.add(createProvSQLType(modelType));
        }
        used.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelUsed.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        used.setOther(others);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelUsed.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        used.setLabel(labels);

        final List<org.openprovenance.prov.model.Location> locations = new ArrayList<>();
        for (org.openprovenance.prov.model.Location modelLocation : modelUsed.getLocation()) {
            locations.add(createProvSQLLocation(modelLocation));
        }
        used.setLocation(locations);

        return used;
    }

    private org.openprovenance.prov.xml.WasInfluencedBy createProvXMLWasInfluencedBy(
            org.openprovenance.prov.model.WasInfluencedBy modelWasInfluencedBy) {
        final org.openprovenance.prov.xml.WasInfluencedBy wasInfluencedBy = new org.openprovenance.prov.xml.WasInfluencedBy();
        wasInfluencedBy.setId(modelWasInfluencedBy.getId());
        wasInfluencedBy.setInfluencee(modelWasInfluencedBy.getInfluencee());
        wasInfluencedBy.setInfluencer(modelWasInfluencedBy.getInfluencer());
        wasInfluencedBy.getType().addAll(modelWasInfluencedBy.getType());
        wasInfluencedBy.getOther().addAll(modelWasInfluencedBy.getOther());
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasInfluencedBy.getLabel()) {
            wasInfluencedBy.getLabel().add(createProvXMLLabel(modelLabel));
        }
        return wasInfluencedBy;
    }

    private WasInfluencedBy createProvSQLWasInfluencedBy(org.openprovenance.prov.model.WasInfluencedBy modelWasInfluencedBy) {
        final WasInfluencedBy wasInfluencedBy = new WasInfluencedBy();
        wasInfluencedBy.setId(createProvSQLQualifiedName(modelWasInfluencedBy.getId()));
        wasInfluencedBy.setInfluencee(createProvSQLQualifiedName(modelWasInfluencedBy.getInfluencee()));
        wasInfluencedBy.setInfluencer(createProvSQLQualifiedName(modelWasInfluencedBy.getInfluencer()));

        final List<org.openprovenance.prov.model.Type> types = new ArrayList<>();
        for (org.openprovenance.prov.model.Type modelType : modelWasInfluencedBy.getType()) {
            types.add(createProvSQLType(modelType));
        }
        wasInfluencedBy.setType(types);

        final List<org.openprovenance.prov.model.Other> others = new ArrayList<>();
        for (org.openprovenance.prov.model.Other modelOther : modelWasInfluencedBy.getOther()) {
            others.add(createProvSQLOther(modelOther));
        }
        wasInfluencedBy.setOther(others);

        final List<org.openprovenance.prov.model.LangString> labels = new ArrayList<>();
        for (org.openprovenance.prov.model.LangString modelLabel : modelWasInfluencedBy.getLabel()) {
            labels.add(createProvSQLLabel(modelLabel));
        }
        wasInfluencedBy.setLabel(labels);

        return wasInfluencedBy;
    }
}
