package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_user_subscription", columnList = "user_id"),
        @Index(name = "idx_subscription_status", columnList = "status"),
        @Index(name = "idx_subscription_tier", columnList = "tier")
})
@Data
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tier", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionTier tier = SubscriptionTier.FREE;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "monthly_price", precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Enums
    public enum SubscriptionTier {
        FREE(0, "Free", BigDecimal.ZERO, 5, 3, 7),
        PREMIUM(1, "Premium", new BigDecimal("29.00"), -1, 25, 180),
        PRO(2, "Pro Trader", new BigDecimal("99.00"), -1, -1, 730);

        private final int level;
        private final String displayName;
        private final BigDecimal price;
        private final int monthlyPredictions; // -1 = unlimited
        private final int maxStocks; // -1 = unlimited
        private final int historyDays;

        SubscriptionTier(int level, String displayName, BigDecimal price,
                int monthlyPredictions, int maxStocks, int historyDays) {
            this.level = level;
            this.displayName = displayName;
            this.price = price;
            this.monthlyPredictions = monthlyPredictions;
            this.maxStocks = maxStocks;
            this.historyDays = historyDays;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public int getMonthlyPredictions() {
            return monthlyPredictions;
        }

        public int getMaxStocks() {
            return maxStocks;
        }

        public int getHistoryDays() {
            return historyDays;
        }

        public boolean hasUnlimitedPredictions() {
            return monthlyPredictions == -1;
        }

        public boolean hasUnlimitedStocks() {
            return maxStocks == -1;
        }
    }

    public enum SubscriptionStatus {
        ACTIVE,
        CANCELLED,
        EXPIRED,
        SUSPENDED,
        PENDING_PAYMENT
    }

    // Helper methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE &&
                (endDate == null || endDate.isAfter(LocalDate.now()));
    }

    public boolean canMakePrediction(int currentMonthUsage) {
        if (!isActive())
            return false;
        return tier.hasUnlimitedPredictions() || currentMonthUsage < tier.getMonthlyPredictions();
    }

    public boolean canAddStock(int currentStockCount) {
        if (!isActive())
            return false;
        return tier.hasUnlimitedStocks() || currentStockCount < tier.getMaxStocks();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}