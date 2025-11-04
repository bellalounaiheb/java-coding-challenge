package com.crewmeister.cmcodingchallenge.exchangerate;

import com.crewmeister.cmcodingchallenge.currency.model.Currency;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    public Currency currency;

    private LocalDate rateDate;

    private BigDecimal rateValue;

    public ExchangeRate() {}

    public ExchangeRate(Long id, LocalDate rateDate, BigDecimal rateValue) {
        this.id = id;
        this.rateDate = rateDate;
        this.rateValue = rateValue;
    }

    public Long getId() {
        return id;
    }

    public Currency getCurrency(Currency currency) {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public void setRateDate(LocalDate rateDate) {
        this.rateDate = rateDate;
    }

    public BigDecimal getRateValue() {
        return rateValue;
    }

    public void setRateValue(BigDecimal rateValue) {
        this.rateValue = rateValue;
    }
}
