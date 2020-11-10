package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;

@Data
@Entity
public class QPUPropsGate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    private String gate;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Parameter> parameters = null;

    @ElementCollection
    private List<BigDecimal> qubits = null;

}