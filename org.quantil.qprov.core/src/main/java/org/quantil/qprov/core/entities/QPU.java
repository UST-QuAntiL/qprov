package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import lombok.Data;

@Data
@Entity
public class QPU implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    @Column(nullable = false)
    public String provider;

    @Column(nullable = false)
    public String backendName;

    @Column(nullable = false)
    public String backendVersion;

    @OneToOne(cascade = CascadeType.ALL)
    public QPUProperties properties;

    public Boolean allowQObject;

    public BigDecimal nQubits;

    @ElementCollection
    public List<String> basisGates = null;

    @OneToMany(cascade = CascadeType.ALL)
    public List<Gate> gates = null;

    public Boolean local;

    public Boolean simulator;

    public Boolean conditional;

    public Boolean openPulse;

    public Boolean memory;

    public BigDecimal maxShots;

    // public List<List<BigDecimal>> couplingMap = null;

    public BigDecimal nUchannels;

    // public List<List<Object>> uChannelLo = null;

    @ElementCollection
    public List<BigDecimal> measLevels = null;

    // public List<List<BigDecimal>> qubitLoRange = null;

    // public List<List<BigDecimal>> measLoRange = null;

    public BigDecimal dt;

    public BigDecimal dtm;

    @ElementCollection
    public List<BigDecimal> repTimes = null;

    @ElementCollection
    public List<String> measKernels = null;

    @ElementCollection
    public List<String> discriminators = null;

    public Boolean creditsRequired;

    public String description;

    // private Hamiltonian hamiltonian;

    public BigDecimal maxExperiments;

    // private List<List<BigDecimal>> measMap = null;

    public BigDecimal nRegisters;

    public OffsetDateTime onlineDate;

    public String sampleName;

    public String url;

    public Boolean allowQCircuit;

    public Boolean allowObjectStorage;

}