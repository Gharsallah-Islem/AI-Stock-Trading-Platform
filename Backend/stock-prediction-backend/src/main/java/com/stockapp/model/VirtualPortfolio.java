package com.stockapp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "virtual_portfolio", indexes = {
        @Index(name = "idx_user_ticker", columnList = "user_id, ticker")
})
@Data
@NoArgsConstructor
public class VirtualPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "shares", nullable = false)
    private int shares;

    @Column(name = "buy_price", nullable = false)
    private float buyPrice;

    @Column(name = "current_value")
    private Float currentValue;

    @Column(name = "trade_date", nullable = false)
    private Date tradeDate;

    @Column(name = "balance", nullable = false)
    private float balance;

    @Column(name = "score", nullable = false)
    private int score = 0;

    @Column(name = "trade_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    public enum TradeType {
        Buy, Sell
    }
}