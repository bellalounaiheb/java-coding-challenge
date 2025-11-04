package com.crewmeister.cmcodingchallenge.currency.dto;

public class CurrencyDTO {
    private String code;
    private String country;

    public CurrencyDTO(String code, String country) {
        this.code = code;
        this.country = country;
    }


    public String getCode() {
        return code;
    }
    public String getCountry() {
        return country;
    }
}
