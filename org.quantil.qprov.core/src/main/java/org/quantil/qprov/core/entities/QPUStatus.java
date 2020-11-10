package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class QPUStatus implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    private Boolean state;

    private String message;

    private Boolean status;

    private BigDecimal lengthQueue;

    private String backendVersion;

}