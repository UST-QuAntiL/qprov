package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;

@Data
@Entity
public class QPUProperties implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    private String backendName;

    private OffsetDateTime lastUpdateDate;

    private String backendVersion;

    @OneToMany(cascade = CascadeType.ALL)
    private List<QPUPropsGate> gates = new ArrayList<>();

    /*@OneToMany(cascade = CascadeType.ALL)
    private List<List<Object>> qubits = new ArrayList<List<Object>>();*/

    @OneToMany(cascade = CascadeType.ALL)
    private List<Qubit> general = new ArrayList<>();

}