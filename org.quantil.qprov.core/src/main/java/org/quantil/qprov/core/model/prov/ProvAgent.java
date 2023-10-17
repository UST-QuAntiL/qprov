package org.quantil.qprov.core.model.prov;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.openprovenance.prov.sql.Agent;

@Entity
public class ProvAgent extends Agent {
    @Id
    private Long id;

    public void setProvAgentId(Long id) {
        this.id = id;
    }

    public Long getProvAgentId() {
        return id;
    }
}
