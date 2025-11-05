package com.crewmeister.cmcodingchallenge.exchangerate.controller;

import com.crewmeister.cmcodingchallenge.exchangerate.dto.ExchangeRateDTO;
import com.crewmeister.cmcodingchallenge.exchangerate.dto.ExchangeRatesForDateDTO;
import com.crewmeister.cmcodingchallenge.exchangerate.dto.SimpleRateDTO;
import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exchangerate.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.service.ExchangeRateImporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "Get all EUR-FX exchange rates at all available dates",
            description = "Returns a paginated list of all EUR-FX exchange rates for all available dates"
    )
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
    @Operation(
            summary = "Trigger live exchange rate update from Bundesbank",
            description = "Initiates a manual update of all EUR-FX exchange rates by fetching the latest data "
            + "from the Deutsche Bundesbank API. This process retrieves the most recent foreign exchange "
            + "reference rates for all supported currencies and stores them in the local database and csv files. "
            + "It is also automatically scheduled to run daily."
    )
    @PostMapping("/update")
    public String updateRates() {
        importer.updateFromBundesbankApi();
        return "Bundesbank update triggered.";
    }

    /** User story 3 - Rates at a particular day */
    @Operation(
            summary = "Get EUR-FX exchange rates for a specific date",
            description = "Retrieves all available EUR-FX exchange rates for the specified date."
    )
    @Parameter(
            name = "date",
            description = "Taget date (ISO format: YYYY-MM-DD) for which exchange rates should be retrieved.",
            example = "2025-11-04"
    )
    @GetMapping(params = "date")
    public Object getExchangeRatesByDate(@RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        List<ExchangeRate> rates = exchangeRateRepository.findAllByRateDate(targetDate);

        if (rates.isEmpty()) {
            return Map.of(
                    "message", "No exchange rate records found for date " + targetDate
            );
        }

        List<SimpleRateDTO> rateList = rates.stream()
                .map(rate -> new SimpleRateDTO(
                        rate.getCurrency().getCode(),
                        rate.getRateValue()
                ))
                .collect(Collectors.toList());

        return new ExchangeRatesForDateDTO(targetDate.toString(), "EUR", rateList);
    }

    /** User story 4 - Convert an amount of currency to euro on a particular day */
    @Operation(
            summary = "Convert a given currency amount to EUR for a specific date",
            description = "Converts the specified amount of the given foreign currency to EUR at the given date"
    )
    @GetMapping("/convert")
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
