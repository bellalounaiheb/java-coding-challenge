package com.crewmeister.cmcodingchallenge.exchangerate.controller;

import com.crewmeister.cmcodingchallenge.currency.model.Currency;
import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import com.crewmeister.cmcodingchallenge.exchangerate.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.service.ExchangeRateImporter;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ExchangeRateController}.
 *
 * These tests verify the main API functionalities:
 * - Listing all exchange rates
 * - Fetching rates by specific date
 * - Converting an amount to EUR
 * - Triggering manual data update
 *
 * MockMvc simulates HTTP requests without starting the full web server,
 * while repository and service dependencies are mocked.
 */
@WebMvcTest(ExchangeRateController.class)
public class ExchangeRateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateRepository rateRepo;

    @MockBean
    private ExchangeRateImporter importer;

    private Currency usd;
    private ExchangeRate rateUSD;

    @BeforeEach
    void setup() {
        usd = new Currency("USD", "United States", LocalDate.now());
        rateUSD = new ExchangeRate(usd, LocalDate.of(2021, 1, 4), BigDecimal.valueOf(1.2265));
    }

    /**
     * Ensures that GET /api/rates/all-exchange-rates returns
     * paginated exchange rate results with correct fields.
     */
    @Test
    void testGetAllExchangeRates() throws Exception {
        Page<ExchangeRate> mockPage = new PageImpl<>(List.of(rateUSD), PageRequest.of(0, 1), 1);
        when(rateRepo.findAllByOrderByRateDateAsc(any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/rates/all-exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.content[0].baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.content[0].value").value(1.2265));
    }

    /**
     * Ensures that GET /api/rates?date=YYYY-MM-DD returns
     * the exchange rates for the specified date.
     */
    @Test
    void testGetExchangeRatesByDate() throws Exception {
        when(rateRepo.findAllByRateDate(LocalDate.of(2021, 1, 4)))
                .thenReturn(List.of(rateUSD));

        mockMvc.perform(get("/api/rates")
                        .param("date", "2021-01-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.rates[0].currency").value("USD"))
                .andExpect(jsonPath("$.rates[0].value").value(1.2265));
    }

    /**
     * Ensures that GET /api/rates?date=YYYY-MM-DD handles
     * the case where no rates exist for that date.
     */
    @Test
    void testGetExchangeRatesByDate_NoData() throws Exception {
        when(rateRepo.findAllByRateDate(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/rates")
                        .param("date", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No exchange rate records found for date 2025-01-01"));
    }

    /**
     * Ensures that GET /api/rates/convert successfully performs
     * conversion when a valid exchange rate exists.
     */
    @Test
    void testConvertToEuro_Success() throws Exception {
        when(rateRepo.findByCurrency_CodeAndRateDate(anyString(), any()))
                .thenReturn(Optional.of(rateUSD));

        mockMvc.perform(get("/api/rates/convert")
                        .param("currency", "USD")
                        .param("date", "2021-01-04")
                        .param("amount", "122.65"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Ensures that GET /api/rates/convert returns an appropriate
     * message when no exchange rate is found for the given currency/date.
     */
    @Test
    void testConvertToEuro_NoRateFound() throws Exception {
        when(rateRepo.findByCurrency_CodeAndRateDate(anyString(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rates/convert")
                        .param("currency", "GBP")
                        .param("date", "2021-01-04")
                        .param("amount", "122.65"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No exchange rate found for GBP on 2021-01-04"));
    }

    /**
     * Ensures that POST /api/rates/update triggers the importer
     * and returns the expected confirmation message.
     */
    @Test
    void testUpdateRates() throws Exception {
        Mockito.doNothing().when(importer).updateFromBundesbankApi();

        mockMvc.perform(post("/api/rates/update"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bundesbank update triggered."));
    }
}
