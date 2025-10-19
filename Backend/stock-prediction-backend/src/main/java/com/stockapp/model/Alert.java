package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stock_symbol", nullable = false)
    private String stockSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private ConditionType condition;

    @Column(name = "target_value", nullable = false)
    private Double targetValue;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "message", length = 500)
    private String message;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_triggered", nullable = false)
    private Boolean isTriggered = false;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "trigger_price")
    private Double triggerPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum AlertType {
        PRICE_TARGET("Price Target"),
        PERCENTAGE_CHANGE("Percentage Change"),
        VOLUME_SPIKE("Volume Spike"),
        PREDICTION_CONFIDENCE("Prediction Confidence");

        private final String displayName;

        AlertType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ConditionType {
        ABOVE("Above"),
        BELOW("Below"),
        EQUALS("Equals");

        private final String displayName;

        ConditionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Helper methods
    public boolean shouldTrigger(Double currentPrice, Double volume, Double confidence) {
        if (!isActive || isTriggered) {
            return false;
        }

        boolean triggered = false;

        switch (alertType) {
            case PRICE_TARGET:
                triggered = checkCondition(currentPrice, targetValue);
                break;
            case PERCENTAGE_CHANGE:
                if (currentValue != null && currentPrice != null) {
                    double changePercent = ((currentPrice - currentValue) / currentValue) * 100;
                    triggered = checkCondition(Math.abs(changePercent), targetValue);
                }
                break;
            case VOLUME_SPIKE:
                if (volume != null) {
                    triggered = checkCondition(volume, targetValue);
                }
                break;
            case PREDICTION_CONFIDENCE:
                if (confidence != null) {
                    triggered = checkCondition(confidence, targetValue);
                }
                break;
        }

        return triggered;
    }

    private boolean checkCondition(Double actual, Double target) {
        if (actual == null || target == null) {
            return false;
        }

        switch (condition) {
            case ABOVE:
                return actual > target;
            case BELOW:
                return actual < target;
            case EQUALS:
                return Math.abs(actual - target) < 0.001; // Small tolerance for floating point
            default:
                return false;
        }
    }

    public void trigger(Double price) {
        this.isTriggered = true;
        this.triggeredAt = LocalDateTime.now();
        this.triggerPrice = price;
        this.isActive = false; // Disable after triggering
    }

    public String getFormattedMessage() {
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }

        return String.format("Alert: %s %s %s %s",
                stockSymbol,
                alertType.getDisplayName(),
                condition.getDisplayName().toLowerCase(),
                formatTargetValue());
    }

    private String formatTargetValue() {
        switch (alertType) {
            case PRICE_TARGET:
                return String.format("$%.2f", targetValue);
            case PERCENTAGE_CHANGE:
                return String.format("%.1f%%", targetValue);
            case VOLUME_SPIKE:
                return String.format("%.1fx", targetValue);
            case PREDICTION_CONFIDENCE:
                return String.format("%.1f%%", targetValue);
            default:
                return targetValue.toString();
        }
    }
}