package org.quantil.qprov.core.entities;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Hub implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    private String name;

    private String title;

    private String description;

    private OffsetDateTime creationDate;

    private Boolean deleted;

    // private Object ui;

    // private Object groups;

    private Boolean _private;

    private Boolean licenseNotRequired;

    private Boolean isDefault;

}