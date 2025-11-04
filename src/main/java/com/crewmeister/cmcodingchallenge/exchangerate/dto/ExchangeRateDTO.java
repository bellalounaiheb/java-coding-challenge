package com.crewmeister.cmcodingchallenge.exchangerate.dto;

import java.math.BigDecimal;

public class ExchangeRateDTO {
    private String baseCurrency;
    private String currency;
    private String date;
    private BigDecimal value;

    public ExchangeRateDTO(String baseCurrency, String currency, String date, BigDecimal value) {
        this.baseCurrency = baseCurrency;
        this.currency = currency;
        this.date = date;
        this.value = value;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDate() {
        return date;
    }

    public BigDecimal getValue() {
        return value;
    }
}
