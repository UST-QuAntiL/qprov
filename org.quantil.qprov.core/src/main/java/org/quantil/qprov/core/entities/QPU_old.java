/*******************************************************************************
 * Copyright (c) 2020 the QProv contributors.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
public class QPU_old implements Serializable {

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

    public Boolean local;

    public Boolean simulator;

    public Boolean conditional;

    public Boolean openPulse;

    public Boolean memory;

    public BigDecimal maxShots;

    public BigDecimal nUchannels;

    // public List<List<BigDecimal>> couplingMap = null;

    @ElementCollection
    public List<BigDecimal> measLevels = null;

    // public List<List<Object>> uChannelLo = null;

    public BigDecimal dt;

    // public List<List<BigDecimal>> qubitLoRange = null;

    // public List<List<BigDecimal>> measLoRange = null;

    public BigDecimal dtm;

    @ElementCollection
    public List<BigDecimal> repTimes = null;

    @ElementCollection
    public List<String> measKernels = null;

    @ElementCollection
    public List<String> discriminators = null;

    public Boolean creditsRequired;

    public String description;

    public BigDecimal maxExperiments;

    // private Hamiltonian hamiltonian;

    public BigDecimal nRegisters;

    // private List<List<BigDecimal>> measMap = null;

    public OffsetDateTime onlineDate;

    public String sampleName;

    public String url;

    public Boolean allowQCircuit;

    public Boolean allowObjectStorage;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;
}
