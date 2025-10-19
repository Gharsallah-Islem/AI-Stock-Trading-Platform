package com.stockapp.repository;

import com.stockapp.model.NewsSentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;

public interface NewsSentimentRepository extends JpaRepository<NewsSentiment, Long> {
    @Query("SELECT n FROM NewsSentiment n WHERE n.ticker = :ticker AND n.date = :date")
    List<NewsSentiment> findByTickerAndDate(String ticker, Date date);

    @Query("SELECT n FROM NewsSentiment n WHERE n.ticker = :ticker ORDER BY n.date DESC")
    List<NewsSentiment> findByTickerOrderByDateDesc(String ticker);
}