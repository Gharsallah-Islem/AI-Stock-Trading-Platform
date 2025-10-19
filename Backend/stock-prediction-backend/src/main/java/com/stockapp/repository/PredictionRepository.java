package com.stockapp.repository;

import com.stockapp.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    Optional<Prediction> findByTickerAndDate(String ticker, Date date);

    @Query("SELECT p FROM Prediction p WHERE p.ticker = :ticker ORDER BY p.date DESC")
    List<Prediction> findByTickerOrderByDateDesc(String ticker);

    List<Prediction> findByTickerAndDateBetween(String ticker, Date startDate, Date endDate);
}
