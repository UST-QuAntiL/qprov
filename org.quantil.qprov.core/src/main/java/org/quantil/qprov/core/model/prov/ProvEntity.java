package org.quantil.qprov.core.model.prov;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ProvEntity extends org.openprovenance.prov.sql.Entity {

    @Id
    private Long id;

    public Long getProvEntityId() {
        return id;
    }

    public void setProvEntityId(Long id) {
        this.id = id;
    }
}
