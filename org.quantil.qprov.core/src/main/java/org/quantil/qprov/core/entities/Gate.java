package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Gate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    // public List<List<BigDecimal>> couplingMap = null;

    public String name;

    @ElementCollection
    public List<String> parameters = null;

    public String qasmDef;


}