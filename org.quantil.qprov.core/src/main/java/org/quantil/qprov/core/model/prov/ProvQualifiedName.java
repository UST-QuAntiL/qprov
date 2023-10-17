package org.quantil.qprov.core.model.prov;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.openprovenance.prov.model.QualifiedNameUtils;

@Entity
public class ProvQualifiedName extends org.openprovenance.prov.sql.QualifiedName {

    @Id
    public Long getPk() {
        return this.pk;
    }

    @Basic
    @Column(
            name = "URI",
            columnDefinition = "TEXT"
    )
    public String getUri() {
        String var10000 = this.getNamespaceURI();
        return var10000 + new QualifiedNameUtils().unescapeProvLocalName(this.getLocalPart());
    }

    public void setUri(String uri) {
    }
}
