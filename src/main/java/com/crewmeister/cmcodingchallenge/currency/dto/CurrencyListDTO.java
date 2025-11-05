package com.crewmeister.cmcodingchallenge.currency.dto;

import java.util.List;

public class CurrencyListDTO {
    private String baseCurrency;
    private int total;
    private List<CurrencyDTO> currencies;

    public CurrencyListDTO(String baseCurrency, List<CurrencyDTO> currencies) {
        this.baseCurrency = baseCurrency;
        this.currencies = currencies;
        this.total = currencies.size();
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }
    public int getTotal() {
        return total;
    }
    public List<CurrencyDTO> getCurrencies() {
        return currencies;
    }
}
