package com.stockapp.repository;

import com.stockapp.model.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface StockDataRepository extends JpaRepository<StockData, Long> {
    Optional<StockData> findByTickerAndDate(String ticker, Date date);

    @Query("SELECT s FROM StockData s WHERE s.ticker = :ticker ORDER BY s.date DESC")
    List<StockData> findByTickerOrderByDateDesc(String ticker);

    List<StockData> findByTicker(String ticker);
}