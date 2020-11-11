package org.quantil.qprov.core.model;

import java.math.BigDecimal;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.openprovenance.prov.model.Statement;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Qubit extends org.openprovenance.prov.xml.Entity implements ProvExtension<Qubit> {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "databaseId", updatable = false, nullable = false)
    private UUID databaseId;

    private BigDecimal t1Time;

    private BigDecimal t2Time;

    @Override
    public Statement toStandardCompliantProv(Qubit qubit) {
        // TODO
        return null;
    }
}
