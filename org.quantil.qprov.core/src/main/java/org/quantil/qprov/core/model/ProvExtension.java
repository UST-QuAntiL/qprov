package org.quantil.qprov.core.model;

import org.openprovenance.prov.model.Statement;

/**
 * Interface to define PROV extensions that can be transformed into standard-compliant PROV
 */
public interface ProvExtension<T> {

    /**
     * Transform the given PROV extension statement to a standard-compliant PROV statement to enable the export of
     * standard-compliant PROV graphs
     *
     * @param extensionStatement the PROV statement using extensions
     * @return the standard-compliant PROV statement
     */
    Statement toStandardCompliantProv(T extensionStatement);
}
