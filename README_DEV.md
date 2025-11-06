# Crewmeister Java Coding Challenge
![Java](https://img.shields.io/badge/Java-11-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.4.1-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.x.x-orange?logo=apachemaven)
![Build](https://img.shields.io/badge/Build-Success-success)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## How to Run the Application

**Prerequisites**
   - Java **11**
   - Maven **3.x.x**
---

1. **Clone the repository**

   ```bash
   https://github.com/bellalounaiheb/java-coding-challenge.git
   cd java-coding-challenge
   ```
   
2. **Build and run the application locally**

   ```bash
   mvn clean spring-boot:run
   ```

3. **Access the Application**

   - ***Swagger UI:*** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   - ***H2 Console:*** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)


### H2 Connection Details
- ***JDBC URL:*** `jdbc:h2:mem:fxdb`
- ***Username:*** `sa`
- ***Password:*** *(leave empty)*

### Note

    On startup, the `DataInitializer` automatically triggers the `ExchangeRateImporter` to load and parse all available currencies and exchange rates from CSV files under: `src/main/resources/data/`
    
    This process initializes the in-memory H2 database with currency and rate data before any API calls are made.


### Example Console Output on Startup

<details>
  <summary>Click to expand console output</summary>
  
```text
### Starting FX data import from CSV files...
### Importing: BBEX3.D.AUD.EUR.BB.AC.000.csv
### AUD -> 6873 inserted, 0 skipped
### Import complete! 1 file(s) processed successfully.
```
</details>

---

## Project Overview

This Spring Boot application implements the **Crewmeister Java Coding Challenge**, providing REST APIs to manage and retrieve **EUR-based exchange rates** from the **Deutsche Bundesbank**.

The project allows users to:
- List all available currencies.
- Retrieve exchange rates at all available dates.
- Retrieve exchange rates by date.
- Convert any foreign currency amount to EUR at a given date.
- Trigger manual or automatic data updates from Bundesbank.

All data is imported from and appended to CSV files, and continuously enriched with live updates from the Bundesbank API.

---

## Architecture Overview

**Tech Stack:**
- Java 11 · Spring Boot 2.4.1
- Spring Data JPA + H2 (in-memory DB)
- Spring Cache (in-memory caching with @Cacheable)
- Swagger / OpenAPI 3 for documentation
- JUnit 5 + Mockito + MockMvc for testing

**Main Components:**

| Layer | Package | Responsibility |
|--------|----------|----------------|
| **Model** | `currency.model`, `exchangerate.model` | Defines JPA entities (`Currency`, `ExchangeRate`) that map to database tables. |
| **Repository** | `currency.repository`, `exchangerate.repository` | Provides CRUD and query operations using Spring Data JPA. |
| **Service** | `exchangerate.service` | Handles CSV import, Bundesbank API fetch, idempotent persistence, and scheduled updates. |
| **DTOs** | `currency.dto`, `exchangerate.dto` | Defines lightweight objects for transferring structured data between backend and API responses. |
| **Controller** | `currency.controller`, `exchangerate.controller` | Exposes REST endpoints implementing all user stories. |
| **Config** | `config` | Contains `DataInitializer` (startup import) and Swagger/OpenAPI configuration. |

---

**Supporting Structure:**

| Directory | Location | Description |
|------------|-----------|-------------|
| **Resources** | `src/main/resources/data/` | Contains the CSV files used to bootstrap exchange rate and currency data. |
| **Test** | `src/test/java/com/crewmeister/cmcodingchallenge/...` | Contains JUnit and MockMvc unit tests for controller and context verification. |

---
## Features Implemented

| Category | Description |
|-----------|-------------|
| **Data Import** | Automatically imports FX data from CSV files located in `/resources/data` and updates them using the Bundesbank API |
| **Currencies API** | Provides a list of all available currencies (`/api/currencies`) |
| **Exchange Rates API** | Fetches all EUR-FX exchange rates and supports filtering by date |
| **Conversion API** | Converts any currency amount to EUR based on a specific historical date |
| **Bundesbank Update** | Triggers a manual or scheduled update of exchange rates via the Bundesbank API, appending new data back to CSV files and updating the database |
| **Caching Layer** | Uses Spring Cache to store frequent exchange rate and currency responses for faster API performance |
| **Swagger Integration** | Interactive documentation at `/swagger-ui/index.html` |
| **Unit Tests** | JUnit + MockMvc tests for controllers and services |


---

## API Documentation (Swagger)

After running the application:

- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

The Swagger page lists all available endpoints with descriptions, parameters, and example responses.

---

## Example API Endpoints

| Method | Endpoint                                                        | Description |
|--------|-----------------------------------------------------------------|-------------|
| `GET` | `/api/currencies`                                               | Get all available currencies |
| `GET` | `/api/rates/all-exchange-rates`                                 | Paginated list of all EUR-FX exchange rates |
| `GET` | `/api/rates?date=2025-11-04`                                    | Get exchange rates for a specific date |
| `GET` | `/api/rates/convert?currency=USD&date=2025-10-10&amount=122.65` | Convert a foreign currency amount to EUR |
| `POST` | `/api/rates/update`                                             | Trigger Bundesbank live update |

## Example API Usage

### Convert Currency to EUR

```bash
GET http://localhost:8080/api/rates/convert?currency=USD&date=2025-10-10&amount=122.65
```

**Response:**

```json
{
  "message": "On 2025-10-10, 122.65 USD = 105.73 EUR"
}
```

### Get Rates for a Date

```bash
GET http://localhost:8080/api/rates?date=2021-01-04
```

**Response:**

```json
{
  "date": "2021-01-04",
  "baseCurrency": "EUR",
  "rates": [
    { "currency": "AUD", "value": 1.59 },
    { "currency": "GBP", "value": 1.96 }
  ]
}
```

---

## Data Flow Summary

On startup, **`DataInitializer`** calls **`ExchangeRateImporter`** to load FX data from `resources/data/`.

---

## Caching Behavior

- Implemented using Spring’s `@Cacheable` annotations.
- Caches frequently accessed data such as:
    - `/api/currencies`
    - `/api/rates` and `/api/rates/convert`
- Reduces repeated database queries and improves API response time.
- Cache entries are automatically refreshed when new Bundesbank updates occur.

---

### Importer Behavior
- Parses CSV files and persists currencies and rates into the **H2 database**.
- Avoids duplicates *(idempotent import)*.
- Update rates live from the **Bundesbank REST API**.

---

### API Layer
- Controllers return simplified DTOs such as:
    - `CurrencyListDTO`
    - `ExchangeRateDTO`  
      for clean JSON responses.
- Frequently requested data (e.g., exchange rates and currency lists) is **cached using Spring Cache** to improve performance.

---

### Automation
- A **daily scheduled task** (`@Scheduled`) automatically updates exchange rates.
- The scheduled update runs daily shortly after the **Bundesbank’s publication** (usually around **10:00 CET**).
- During this process, the latest exchange rates are written **both to the in-memory H2 database and back into the CSV files** in `src/main/resources/data/`, ensuring data consistency between the local store and the imported files.

---

## Running Tests

To run all unit tests, execute:

```bash
mvn test
```

### Expected Output

```diff
+ Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
+ BUILD SUCCESS
```

## Test Overview

| Test Class | Description |
|-------------|-------------|
| **`CurrencyControllerTest`** | Validates `/api/currencies` returns the correct structure. |
| **`ExchangeRateControllerTest`** | Tests listing, date filtering, conversion, and update endpoints. |

All tests use **`MockMvc`** to simulate HTTP requests **without starting a full web server**. Dependencies (**`ExchangeRateRepository`**, **`CurrencyRepository`**, **`ExchangeRateImporter`**) are **mocked using Mockito**.

---

## Future Improvements

- Add **service-layer tests** for `ExchangeRateImporter`.
- Extend **conversion logic** to support **multi-currency (EUR ↔ others)**.
- Integrate a **persistent database** (PostgreSQL/MySQL) instead of H2.
- Enhance **caching strategy** with TTL and cache invalidation policies; add **error-handling middleware**.
- Include **response examples in Swagger** via `@ApiResponses`.

---

## License / Disclaimer

> *Developed as part of the Crewmeister Java Coding Challenge (2025).*  
> **Bundesbank data** is public and used under its **open data terms**.


