package com.crewmeister.cmcodingchallenge.exchangerate.controller;

import com.crewmeister.cmcodingchallenge.exchangerate.dto.ExchangeRateDTO;
import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exchangerate.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.service.ExchangeRateImporter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rates")
public class ExchangeRateController {
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateImporter importer;

    public ExchangeRateController(ExchangeRateRepository exchangeRateRepository, ExchangeRateImporter importer) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.importer = importer;
    }

    /** User story 2: Get all EUR-FX exchange rates at all dates as a collection */
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

    @PostMapping("/update")
    public String updateRates() {
        importer.updateFromBundesbankApi();
        return "Bundesbank update triggered.";
    }

    /** User story 3 - Rates at a particular day */
    @GetMapping(params = "date")
    public Object getExchangeRatesByDate(@RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        List<ExchangeRate> rates = exchangeRateRepository.findAllByRateDate(targetDate);

        if (rates.isEmpty()) {
            return Map.of(
                    "message", "No exchange rate records found for date " + targetDate
            );
        }

        return rates.stream()
                .map(rate -> new ExchangeRateDTO(
                        "EUR",
                        rate.getCurrency().getCode(),
                        rate.getRateDate().toString(),
                        rate.getRateValue()
                ))
                .collect(Collectors.toList());
    }


}
