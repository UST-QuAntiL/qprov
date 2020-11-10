package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.Data;

@Data
@Entity
public class QPUDefaults implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    private BigDecimal buffer;

    @OneToOne
    private Discriminator discriminator;

    @ElementCollection
    private List<BigDecimal> measFreqEst = null;

    /*@OneToOne
    private MeasKernel measKernel;*/

    @ElementCollection
    private List<BigDecimal> qubitFreqEst = null;

}