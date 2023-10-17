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

import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.Value;
import org.openprovenance.prov.xml.Other;
import org.openprovenance.prov.xml.Type;
import org.quantil.qprov.core.Constants;
import org.quantil.qprov.core.model.ProvTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static final ProvFactory pFactory = InteropFramework.getDefaultFactory();

    private Utils() {
    }

    /**
     * Generate a PROV QualifiedName using the given namespace prefix or the default namespace from the QProv system if none is defined
     *
     * @param localName       the local name for the QualifiedName
     * @param namespacePrefix the namespace prefix for the QualifiedName or null if the default namespace should be used
     * @return the QualifiedName using the given local name and the given or default namespace
     */
    public static QualifiedName generateQualifiedName(String localName, String namespacePrefix) {
        // register all known namespaces
        final Namespace ns = new Namespace();
        ns.addKnownNamespaces();
        ns.register(Constants.DEFAULT_NAMESPACE_PREFIX, Constants.DEFAULT_NAMESPACE);

        // use default QProv namespace or given namespace prefix for the QualifiedName
        if (Objects.isNull(namespacePrefix)) {
            return ns.qualifiedName(Constants.DEFAULT_NAMESPACE_PREFIX, localName, pFactory);
        } else {
            return ns.qualifiedName(namespacePrefix, localName, pFactory);
        }
    }

    /**
     * Create a PROV Other element with the given name, value, and type
     *
     * @param name  the name of the Other element to create
     * @param value the value of the Other element to create
     * @param type  the type of the Other element to create
     * @return the created Other element
     */
    public static Other createOtherElement(String name, String value, String type) {
        final Other other = new Other();
        other.setElementName(generateQualifiedName(name, null));
        other.setValue(value);
        other.setType(generateQualifiedName(type, null));
        return other;
    }

    /**
     * Create a Type element with a given name
     *
     * @param typeName the local name of the type
     * @return the created type element
     */
    public static Type createTypeElement(String typeName) {
        final Type type = new Type();
        type.setType(generateQualifiedName(Constants.DATA_TYPE_QNAME, Constants.NAMESPACE_XSD_PREFIX));
        type.setValue(generateQualifiedName(typeName, null));
        return type;
    }

    /**
     * Create a value element of type string
     *
     * @param value the value to use as String
     * @return the PROV Value element
     */
    public static Value createStringValueElement(String value) {
        return pFactory.newValue(value, pFactory.getName().XSD_STRING);
    }

    /**
     * Parse a given PROV document to a ProvTemplate
     *
     * @param modelDocument the PROV document object
     * @return the created ProvTemplate
     */
    public static ProvTemplate createProvTemplate(org.openprovenance.prov.model.Document modelDocument) {
        final ProvTemplate document = new ProvTemplate();
        document.setNamespace(modelDocument.getNamespace());
        document.setStatementOrBundle(modelDocument.getStatementOrBundle());
        return document;
    }
}
