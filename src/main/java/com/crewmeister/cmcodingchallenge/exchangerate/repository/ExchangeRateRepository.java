package com.crewmeister.cmcodingchallenge.exchangerate.repository;

import com.crewmeister.cmcodingchallenge.exchangerate.model.ExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Page<ExchangeRate> findAllByOrderByRateDateAsc(Pageable pageable);
    List<ExchangeRate> findAllByRateDate(LocalDate rateDate);
    Optional<ExchangeRate> findByCurrency_CodeAndRateDate(String code, LocalDate rateDate);
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
            "FROM ExchangeRate e WHERE e.currency.code = :code AND e.rateDate = :date")
    boolean existsByCurrencyCodeAndRateDate(@Param("code") String code, @Param("date") LocalDate date);
}
