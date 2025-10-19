package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "anomalies", indexes = {
        @Index(name = "idx_ticker_date", columnList = "ticker, date")
})
@Data
@NoArgsConstructor
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "date", nullable = false)
    private Date date;

    @Column(name = "anomaly_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AnomalyType anomalyType;

    @Column(name = "description")
    private String description;

    @Column(name = "severity")
    private Float severity;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    public enum AnomalyType {
        Price, News, Volume
    }
}