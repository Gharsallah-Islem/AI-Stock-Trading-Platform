package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "usage_tracking", indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, date"),
        @Index(name = "idx_date", columnList = "date")
})
@Data
@NoArgsConstructor
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "predictions_used", nullable = false)
    private Integer predictionsUsed = 0;

    @Column(name = "stocks_tracked", nullable = false)
    private Integer stocksTracked = 0;

    @Column(name = "api_calls_made", nullable = false)
    private Integer apiCallsMade = 0;

    @Column(name = "premium_features_used", nullable = false)
    private Integer premiumFeaturesUsed = 0;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Helper methods for usage tracking
    public void incrementPredictions() {
        this.predictionsUsed++;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void incrementApiCalls() {
        this.apiCallsMade++;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void incrementPremiumFeatures() {
        this.premiumFeaturesUsed++;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void updateStocksTracked(int count) {
        this.stocksTracked = count;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}