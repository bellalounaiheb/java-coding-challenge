package com.crewmeister.cmcodingchallenge.exchangerate.controller;

import com.crewmeister.cmcodingchallenge.exchangerate.dto.ExchangeRateDTO;
import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exchangerate.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.service.ExchangeRateImporter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rates")
public class ExchangeRateController {
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateImporter importer;

    public ExchangeRateController(ExchangeRateRepository exchangeRateRepository, ExchangeRateImporter importer) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.importer = importer;
    }

    /** User story 2: Get all EUR-FX exchange rates at all dates */
    @GetMapping("all-exchange-rates")
    public Page<ExchangeRateDTO> getAllExchangeRates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExchangeRate> ratePage = exchangeRateRepository.findAllByOrderByRateDateAsc(pageable);

        return ratePage.map(rate -> new ExchangeRateDTO(
                "EUR",
                rate.getCurrency().getCode(),
                rate.getRateDate().toString(),
                rate.getRateValue()
        ));
    }


}
