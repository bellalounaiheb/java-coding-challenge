package com.crewmeister.cmcodingchallenge.exchangerate.service;

import com.crewmeister.cmcodingchallenge.currency.model.Currency;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles importing and updating exchange rate data.
 *
 * Sources:
 *  - CSV files from src/main/resources/data/
 *  - Live updates via Bundesbank API
 *
 * Responsibilities:
 *  - Parse and persist currency and exchange rate data into H2 database
 *  - Maintain idempotency (avoid duplicate inserts)
 *  - Append new fetched data back to CSVs
 *  - Schedule daily automatic Bundesbank updates
 */

@Service
public class ExchangeRateImporter {
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateImporter(CurrencyRepository currencyRepository, ExchangeRateRepository exchangeRateRepository) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Imports all available FX data from CSV files under /resources/data.
     *  - Iterates over each CSV file found in the folder
     *  - Parses metadata (currency code, country)
     *  - Inserts new exchange rate rows into the database
     *  - Skips duplicates based on (currency, date)
     */
    public void importCsvData() {
        System.out.println("### Starting FX data import from CSV files...");

        try {
            // Automatically load all CSVs under src/main/resources/data/
            Resource[] csvFiles = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:data/*.csv");

            if (csvFiles.length == 0) {
                System.out.println("### No CSV files found in resources/data/");
                return;
            }

            int importedCount = 0;

            for (Resource file : csvFiles) {
                String fileName = file.getFilename();
                if (fileName == null || !fileName.endsWith(".csv")) continue;

                System.out.printf("### Importing: %s%n", fileName);
                try {
                    parseFile(file);
                    importedCount++;
                } catch (Exception e) {
                    System.err.printf("### Skipped %s due to error: %s%n", fileName, e.getMessage());
                }
            }

            System.out.printf("### Import complete! %d file(s) processed successfully.%n", importedCount);

        } catch (IOException e) {
            throw new RuntimeException("### Failed to import CSV data: " + e.getMessage(), e);
        }
    }

    /** Parses a single CSV file, extracts currency metadata, and loads all daily rates */
    private void parseFile(Resource file) {
        String currencyCode = null;
        String currencyName = null;
        LocalDate lastUpdated = LocalDate.now();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean readingData = false;
            int inserted = 0;
            int skipped = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Header detection (before actual data lines)
                if (!readingData) {
                    // skip non-useful metadata lines
                    if (line.toLowerCase().startsWith("last update")
                            || line.toLowerCase().startsWith("comment")
                            || line.toLowerCase().startsWith("source")
                            || line.toLowerCase().startsWith("decimals")
                            || line.toLowerCase().startsWith("unit")) {
                        continue;
                    }

                    // detect currency header line
                    if (currencyCode == null && line.contains("EUR 1 =")) {
                        String[] parts = line.split(",", -1);
                        String metadata = (parts.length > 1 ? parts[1] : line)
                                .replace("\"", "")
                                .replaceAll(",+$", "")
                                .trim();

                        System.out.println("### HEADER DEBUG: " + metadata);
                        currencyCode = extractCode(metadata);
                        currencyName = extractName(metadata);
                        System.out.printf("### Parsed code=%s, name=%s%n", currencyCode, currencyName);
                        continue;
                    }

                    // detect beginning of actual data lines
                    if (line.matches("^(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4}).*")) {
                        readingData = true;
                    }
                }

                // Data section
                if (readingData && line.matches("^(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4}).*")) {
                    String[] parts = line.split(",");
                    if (parts.length < 2 || parts[1].equals(".") || parts[1].isBlank()) continue;

                    LocalDate date = null;
                    String rawDate = parts[0].trim();

                    // Try both ISO and Bundesbank date formats
                    try {
                        date = LocalDate.parse(rawDate); // ISO
                    } catch (Exception e1) {
                        try {
                            date = LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("M/d/yyyy"));
                        } catch (Exception e2) {
                            System.out.printf("### Skipped invalid date '%s' in %s%n", rawDate, file.getFilename());
                            continue;
                        }
                    }

                    BigDecimal value;
                    try {
                        value = new BigDecimal(parts[1].trim());
                    } catch (NumberFormatException e) {
                        System.out.printf("### Skipped invalid rate '%s' on %s%n", parts[1], rawDate);
                        continue;
                    }

                    // Retrieve or create currency
                    Currency currency = getOrCreateCurrency(currencyCode, currencyName, lastUpdated);
                    if (currency == null) {
                        System.out.println("### Skipped " + file.getFilename() + " (invalid code: " + currencyCode + ")");
                        return;
                    }

                    // Check for existing rate (idempotent)
                    boolean exists = exchangeRateRepository.existsByCurrencyCodeAndRateDate(currencyCode, date);

                    if (exists) {
                        skipped++;
                        continue;
                    }

                    try {
                        exchangeRateRepository.save(new ExchangeRate(currency, date, value));
                        inserted++;
                    } catch (DataIntegrityViolationException e) {
                        skipped++;
                    }
                }
            }

            System.out.printf("### %s -> %d inserted, %d skipped%n", currencyCode, inserted, skipped);

        } catch (Exception e) {
            System.out.println("### Failed to process " + file.getFilename() + ": " + e.getMessage());
        }
    }


    /** Extracts 3-letter ISO currency code (e.g. "USD") from the header line. */
    private String extractCode(String line) {
        if (line == null || line.isBlank()) return null;
        Pattern codePattern = Pattern.compile("EUR\\s*1\\s*=\\s*([A-Z]{3})");
        Matcher matcher = codePattern.matcher(line);
        if (matcher.find()) return matcher.group(1).trim();

        for (String token : line.split("\\s+")) {
            if (token.matches("^[A-Z]{3}$")) return token;
        }
        return null;
    }

    /** Extracts country name or description from the header line. */
    private String extractName(String line) {
        if (line == null || line.isBlank()) return "Unknown Country";

        Pattern pattern = Pattern.compile("EUR 1 =\\s*\\w+\\s*\\.\\.\\.\\s*/\\s*(.*)$");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String desc = matcher.group(1).trim().replaceAll("[,]+$", "").trim();
            if (desc.contains("/")) desc = desc.split("/")[0].trim();
            return desc;
        }

        String[] tokens = line.split("/");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].contains("...")) {
                String desc = tokens[i + 1].trim().replaceAll("[,]+$", "").trim();
                if (desc.contains("/")) desc = desc.split("/")[0].trim();
                return desc;
            }
        }

        return "Unknown Country";
    }

    /** Retrieves existing currency from DB, or creates it if missing. */
    private Currency getOrCreateCurrency(String code, String name, LocalDate updated) {
        if (code == null || code.length() != 3) {
            System.out.println("### Skipping invalid currency: code=" + code + ", name=" + name);
            return null;
        }

        Optional<Currency> existing = currencyRepository.findById(code);
        if (existing.isPresent()) return existing.get();

        Currency c = new Currency(code, name, updated);
        return currencyRepository.save(c);
    }


    /**
     * Scheduled task — fetches and updates exchange rates from Bundesbank API daily.
     * Runs automatically at 11:00 AM local time.
     */
    @Scheduled(cron = "0 0 11 * * *")
    @CacheEvict(value = { "ratesByDate", "currencies" }, allEntries = true)
    public void updateFromBundesbankApi() {
        System.out.println("### Starting Bundesbank API update for all currencies...");

        List<Currency> currencies = currencyRepository.findAll();
        if (currencies.isEmpty()) {
            System.out.println("### No currencies found in DB — import CSVs first.");
            return;
        }

        int successCount = 0, failedCount = 0;

        for (Currency currency : currencies) {
            String code = currency.getCode();

            // Skip EUR (no conversion needed)
            if ("EUR".equalsIgnoreCase(code)) continue;

            try {
                boolean updated = fetchAndUpdateCurrency(code);
                if (updated) successCount++;
            } catch (Exception e) {
                failedCount++;
                System.err.printf("### Failed for %s: %s%n", code, e.getMessage());
            }

            // Respect Bundesbank API rate limits — 2 seconds pause between calls
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignored) {}
        }

        System.out.printf("### Update finished: %d currencies updated, %d failed%n",
                successCount, failedCount);
    }

    @Transactional
    public boolean fetchAndUpdateCurrency(String currencyCode) throws IOException {
        String apiUrl = String.format(
                "https://api.statistiken.bundesbank.de/rest/data/BBEX3/D.%s.EUR.BB.AC.000",
                currencyCode
        );

        System.out.println("### Fetching " + currencyCode + " from Bundesbank...");

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.sdmx.data+json");
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        if (conn.getResponseCode() != 200) {
            throw new IOException("HTTP " + conn.getResponseCode());
        }

        String json;
        try (InputStream in = conn.getInputStream()) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }

        return parseBundesbankJson(json);
    }

    /** Parses Bundesbank JSON response, persists new rates and appends them to CSV files */
    private boolean parseBundesbankJson(String jsonResponse) {
        JSONObject root = new JSONObject(jsonResponse);
        JSONObject data = root.getJSONObject("data");

        JSONArray timePeriods = data
                .getJSONObject("structure")
                .getJSONObject("dimensions")
                .getJSONArray("observation")
                .getJSONObject(0)
                .getJSONArray("values");

        JSONArray dataSets = data.getJSONArray("dataSets");
        JSONObject series = dataSets.getJSONObject(0).getJSONObject("series");

        int insertedTotal = 0;

        for (String seriesKey : series.keySet()) {
            JSONObject serie = series.getJSONObject(seriesKey);
            JSONObject observations = serie.getJSONObject("observations");

            JSONArray seriesDims = data
                    .getJSONObject("structure")
                    .getJSONObject("dimensions")
                    .getJSONArray("series");

            String currencyCode = null;
            for (int i = 0; i < seriesDims.length(); i++) {
                JSONObject dim = seriesDims.getJSONObject(i);
                if ("BBK_STD_CURRENCY".equals(dim.getString("id"))) {
                    currencyCode = dim.getJSONArray("values").getJSONObject(0).getString("id");
                    break;
                }
            }
            if (currencyCode == null) currencyCode = "UNKNOWN";

            Currency currency = currencyRepository.findById(currencyCode).orElse(null);
            if (currency == null) {
                currency = new Currency(currencyCode, currencyCode, LocalDate.now());
                currency = currencyRepository.save(currency);
            }

            // Collect new rates to later append to CSV
            List<ExchangeRate> newRates = new ArrayList<>();

            int inserted = 0, skipped = 0;

            for (String obsKey : observations.keySet()) {
                int timeIndex = Integer.parseInt(obsKey);
                JSONArray obsArray = observations.getJSONArray(obsKey);
                if (obsArray.isNull(0)) {
                    skipped++;
                    continue;
                }

                String dateStr = timePeriods.getJSONObject(timeIndex).getString("id");
                BigDecimal rate = new BigDecimal(obsArray.getString(0));

                LocalDate date;
                // Handle both date formats (ISO or M/d/yyyy)
                try {
                    date = LocalDate.parse(dateStr); // yyyy-MM-dd
                } catch (Exception e) {
                    date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
                }

                // Use your repository method
                if (!exchangeRateRepository.existsByCurrencyCodeAndRateDate(currencyCode, date)) {
                    ExchangeRate saved = exchangeRateRepository.save(new ExchangeRate(currency, date, rate));
                    newRates.add(saved);
                    inserted++;
                } else {
                    skipped++;
                }
            }

            insertedTotal += inserted;

            // After processing all dates for this currency — append to CSV if there are new ones
            if (!newRates.isEmpty()) {
                appendRatesToCsv(currency, newRates);
            }

            if (inserted == 0)
                System.out.printf("### %s already up-to-date (%d skipped)%n", currencyCode, skipped);
            else
                System.out.printf("### %s -> %d new, %d skipped%n", currencyCode, inserted, skipped);
        }

        return insertedTotal > 0;
    }

    /** Appends new rates at the end of their respective CSV file */
    private void appendRatesToCsv(Currency currency, List<ExchangeRate> newRates) {
        if (newRates.isEmpty()) return;

        String fileName = String.format("src/main/resources/data/BBEX3.D.%s.EUR.BB.AC.000.csv", currency.getCode());
        Path path = Paths.get(fileName);

        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
                Files.writeString(path,
                        String.format("Euro foreign exchange reference rate of the ECB / EUR 1 = %s ... / %s%n",
                                currency.getCode(), currency.getName()),
                        StandardOpenOption.APPEND);
                Files.writeString(path, "last update," + LocalDate.now() + "\n", StandardOpenOption.APPEND);
            } else {
                // Update 'last update' line if it exists
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).toLowerCase().startsWith("last update")) {
                        lines.set(i, "last update," + LocalDate.now());
                        break;
                    }
                }
                Files.write(path, lines, StandardCharsets.UTF_8);
            }

            // Bundesbank-style date format (M/d/yyyy)
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d/yyyy");

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
                for (ExchangeRate rate : newRates) {
                    writer.write(String.format("%s,%s%n",
                            rate.getRateDate().format(fmt),
                            rate.getRateValue().stripTrailingZeros().toPlainString()));
                }
            }

            System.out.printf("### Appended %d new rates to %s%n", newRates.size(), path.getFileName());
        } catch (IOException e) {
            System.err.printf("### Failed to write CSV for %s: %s%n", currency.getCode(), e.getMessage());
        }
    }
}
