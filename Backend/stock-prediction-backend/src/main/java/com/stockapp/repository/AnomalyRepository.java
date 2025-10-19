package com.stockapp.repository;

import com.stockapp.model.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
    @Query("SELECT a FROM Anomaly a WHERE a.ticker = :ticker AND a.date = :date")
    List<Anomaly> findByTickerAndDate(String ticker, Date date);

    @Query("SELECT a FROM Anomaly a WHERE a.ticker = :ticker ORDER BY a.date DESC")
    List<Anomaly> findByTickerOrderByDateDesc(String ticker);
}