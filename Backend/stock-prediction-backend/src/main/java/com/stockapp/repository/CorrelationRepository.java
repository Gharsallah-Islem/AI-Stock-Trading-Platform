package com.stockapp.repository;

import com.stockapp.model.Correlation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface CorrelationRepository extends JpaRepository<Correlation, Long> {
    Optional<Correlation> findByTicker1AndTicker2AndDate(String ticker1, String ticker2, Date date);

    @Query("SELECT c FROM Correlation c WHERE c.ticker1 = :ticker OR c.ticker2 = :ticker ORDER BY c.date DESC")
    List<Correlation> findByTickerOrderByDateDesc(String ticker);
}