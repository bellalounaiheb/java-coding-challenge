package com.crewmeister.cmcodingchallenge.exchangerate.model;

import com.crewmeister.cmcodingchallenge.currency.model.Currency;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates",
    uniqueConstraints = {
            @UniqueConstraint(columnNames={"currency_id", "rate_date"})
    })
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    public Currency currency;
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;
    private BigDecimal rateValue;

    public ExchangeRate() {}

    public ExchangeRate(Currency currency, LocalDate rateDate, BigDecimal rateValue) {
        this.currency = currency;
        this.rateDate = rateDate;
        this.rateValue = rateValue;
    }

    public Long getId() {
        return id;
    }

    public Currency getCurrency() {
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
