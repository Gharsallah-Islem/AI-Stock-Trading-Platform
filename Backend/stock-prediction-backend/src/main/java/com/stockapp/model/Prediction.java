package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "predictions", indexes = {
        @Index(name = "idx_ticker_date", columnList = "ticker, date")
})
@Data
@NoArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "date", nullable = false)
    private Date date;

    @Column(name = "predicted_price", nullable = false)
    private float predictedPrice;

    @Column(name = "advice", nullable = false)
    @Enumerated(EnumType.STRING)
    private Advice advice;

    @Column(name = "confidence", nullable = false)
    private float confidence;

    @Column(name = "model_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ModelType modelType;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    public enum Advice {
        Buy, Sell, Hold
    }

    public enum ModelType {
        LSTM, XGBoost, Ensemble
    }
}