package com.crewmeister.cmcodingchallenge.exchangerate.dto;

import java.util.List;

public class ExchangeRatesForDateDTO {
    private String date;
    private String baseCurrency;
    private List<SimpleRateDTO> rates;

    public ExchangeRatesForDateDTO(String date, String baseCurrency, List<SimpleRateDTO> rates) {
        this.date = date;
        this.baseCurrency = baseCurrency;
        this.rates = rates;
    }
    public String getDate() {
        return date;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public List<SimpleRateDTO> getRates() {
        return rates;
    }
}
