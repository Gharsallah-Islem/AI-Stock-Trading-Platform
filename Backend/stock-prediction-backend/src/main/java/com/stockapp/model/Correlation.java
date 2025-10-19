package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "correlations", indexes = {
        @Index(name = "idx_ticker_date", columnList = "ticker1, ticker2, date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "ticker1", columnNames = {"ticker1", "ticker2", "date"})
})
@Data
public class Correlation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker1", nullable = false, length = 10)
    private String ticker1;

    @Column(name = "ticker2", nullable = false, length = 10)
    private String ticker2;

    @Column(name = "correlation", nullable = false)
    private float correlation;

    @Column(name = "date", nullable = false)
    private Date date;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());
}