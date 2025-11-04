package com.crewmeister.cmcodingchallenge.currency.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "currencies")
public class Currency {
    @Id
    @Column(length = 3, nullable = false, unique = true)
    private String code;
    private String name;
    private LocalDate lastUpdated;

    public Currency() {}

    public Currency(String code, String name, LocalDate lastUpdated) {
        this.code = code;
        this.name = name;
        this.lastUpdated = lastUpdated;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDate lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
