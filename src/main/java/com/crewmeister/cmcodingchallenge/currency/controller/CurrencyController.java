package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.currency.dto.CurrencyListDTO;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController()
@RequestMapping("/api/currencies")
public class CurrencyController {
    private final CurrencyRepository currencyRepository;

    public CurrencyController(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    /**
     * Returns all available (non-obsolete) currencies from the database.
     * User story 1 : Get a list of all available currencies
     */
    @GetMapping
    public CurrencyListDTO getAllCurrencies() {
        List<CurrencyDTO> list = currencyRepository.findAll().stream()
                .map(c -> new CurrencyDTO(c.getCode(), c.getName()))
                .collect(Collectors.toList());
        return new CurrencyListDTO("EUR", list);
    }
}
