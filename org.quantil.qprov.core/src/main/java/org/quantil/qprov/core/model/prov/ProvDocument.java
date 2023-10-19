package org.quantil.qprov.core.model.prov;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.openprovenance.prov.sql.Document;

@Entity
public class ProvDocument extends Document {
    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

