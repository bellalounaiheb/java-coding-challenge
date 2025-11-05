package com.crewmeister.cmcodingchallenge.currency.controller;

import com.crewmeister.cmcodingchallenge.currency.model.Currency;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CurrencyController}.
 *
 * These tests use MockMvc to simulate HTTP requests to the /api/currencies endpoint.
 * The CurrencyRepository is mocked to verify that the controller returns
 * the correct JSON structure and HTTP responses.
 */
@WebMvcTest(controllers = CurrencyController.class)
public class CurrencyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyRepository currencyRepository;

    /**
     * Ensures that GET /api/currencies returns a properly structured
     * CurrencyListDTO containing baseCurrency, total count, and a list of currencies.
     */
    @Test
    void testGetAllAvailableCurrencies() throws Exception {
        when(currencyRepository.findAll()).thenReturn(List.of(
                new Currency("USD", "United States", LocalDate.now()),
                new Currency("JPY", "Japan", LocalDate.now())
        ));

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("EUR"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.currencies[0].code").value("USD"))
                .andExpect(jsonPath("$.currencies[1].code").value("JPY"));
    }

    /**
     * Ensures that GET /api/currencies returns an empty list when
     * no currencies are found in the database.
     */
    @Test
    void testGetAllCurrencies_EmptyList() throws Exception {
        when(currencyRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.currencies").isEmpty());
    }
}
