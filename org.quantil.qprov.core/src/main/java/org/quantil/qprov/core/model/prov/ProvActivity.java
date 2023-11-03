package org.quantil.qprov.core.model.prov;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.openprovenance.prov.sql.Activity;

@Entity
public class ProvActivity extends Activity {
    @Id
    private Long id;

    public Long getProvActivityId() {
        return id;
    }

    public void setProvActivityId(Long id) {
        this.id = id;
    }
}
