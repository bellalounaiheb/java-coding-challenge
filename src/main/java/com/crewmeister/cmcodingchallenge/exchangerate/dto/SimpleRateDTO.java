package com.crewmeister.cmcodingchallenge.exchangerate.dto;

import java.math.BigDecimal;

public class SimpleRateDTO {
    private String currency;
    private BigDecimal value;

    public SimpleRateDTO(String currency, BigDecimal value) {
        this.currency = currency;
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getValue() {
        return value;
    }
}
