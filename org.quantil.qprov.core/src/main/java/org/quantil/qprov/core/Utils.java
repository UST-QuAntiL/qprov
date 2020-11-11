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

import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;

public final class Utils {

    private static final ProvFactory pFactory = InteropFramework.newXMLProvFactory();

    private Utils() {
    }

    /**
     * Generate a PROV QualifiedName using the default namespace from the QProv system
     *
     * @param localName the local name for the QualifiedName
     * @return the QualifiedName using the given local name and the QProv namespace
     */
    public static QualifiedName generateQualifiedName(String localName) {
        final Namespace ns = new Namespace();
        ns.addKnownNamespaces();
        ns.register(Constants.DEFAULT_NAMESPACE_PREFIX, Constants.DEFAULT_NAMESPACE);
        return ns.qualifiedName(Constants.DEFAULT_NAMESPACE_PREFIX, localName, pFactory);
    }
}
