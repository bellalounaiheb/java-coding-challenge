package com.crewmeister.cmcodingchallenge.config;

import com.crewmeister.cmcodingchallenge.exchangerate.service.ExchangeRateImporter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final ExchangeRateImporter exchangeRateImporter;

    public DataInitializer(ExchangeRateImporter exchangeRateImporter) {
        this.exchangeRateImporter = exchangeRateImporter;
    }

    @Override
    public void run(String... args) {
        System.out.println("Starting FX data import...");
        exchangeRateImporter.importCsvData();
        System.out.println("Import complete.");
    }
}
