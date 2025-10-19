package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Entity
@Table(name = "stock_data", indexes = {
        @Index(name = "idx_ticker_date", columnList = "ticker, date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "ticker", columnNames = {"ticker", "date"})
})
@Data
@NoArgsConstructor
public class StockData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "date", nullable = false)
    private Date date;

    @Column(name = "close_price", nullable = false)
    private float closePrice;

    @Column(name = "volume", nullable = false)
    private long volume;

    private Float rsi;

    private Float macd;

    private Float vix;

    @Column(name = "sma_50")
    private Float sma50;

    @Column(name = "ema_20")
    private Float ema20;

    @Column(name = "bb_upper")
    private Float bbUpper;

    @Column(name = "bb_lower")
    private Float bbLower;

    private Float atr;

    private Long obv;

    private Float stochastic;

    @Column(name = "interest_rate")
    private Float interestRate;
}