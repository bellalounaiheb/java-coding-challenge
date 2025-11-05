package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.dto.CurrencyDTO;
import com.crewmeister.cmcodingchallenge.currency.dto.CurrencyListDTO;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cache.annotation.Cacheable;
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
     * Returns all available (non-obsolete) currencies.
     * User story 1 : Get a list of all available currencies
     */
    @Operation(
            summary = "Get all available currencies",
            description = "Retrieves a list of all available (non-obsolete) currencies"
    )
    @GetMapping
    @Cacheable("currencies")
    public CurrencyListDTO getAllCurrencies() {
        System.out.println("Fetching currencies from database"); // to test caching
        List<CurrencyDTO> list = currencyRepository.findAll().stream()
                .map(c -> new CurrencyDTO(c.getCode(), c.getName()))
                .collect(Collectors.toList());
        return new CurrencyListDTO("EUR", list);
    }
}
