package com.crewmeister.cmcodingchallenge.exchangerate.controller;

import com.crewmeister.cmcodingchallenge.exchangerate.dto.ExchangeRateDTO;
import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exchangerate.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.service.ExchangeRateImporter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /** Manual test to fetch new data*/
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

    /** User story 4 - Convert an amount of currency to euro on a particular day */
    @GetMapping("/Convert")
    public Object convertToEuro(
            @RequestParam String currency,
            @RequestParam String date,
            @RequestParam BigDecimal amount
    ) {
        LocalDate targetDate = LocalDate.parse(date);
        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findByCurrency_CodeAndRateDate(currency.toUpperCase(), targetDate);

        if (rateOpt.isEmpty()) {
            return Map.of(
                    "message", "No exchange rate found for " + currency.toUpperCase() + " on " + targetDate
            );
        }

        ExchangeRate rate = rateOpt.get();

        BigDecimal eurAmount = amount.divide(rate.getRateValue(), 4, RoundingMode.HALF_UP);

        String message = String.format(
                "On %s, %.2f %s = %.2f EUR",
                targetDate,
                amount,
                currency.toUpperCase(),
                eurAmount
        );

        return Map.of("message", message);
    }


}
