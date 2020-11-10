package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Parameter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    private OffsetDateTime date;

    private String name;

    private String unit;

    private BigDecimal value;

}