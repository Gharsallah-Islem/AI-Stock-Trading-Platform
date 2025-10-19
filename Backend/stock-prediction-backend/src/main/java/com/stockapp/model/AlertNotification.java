package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private NotificationSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder.Default
    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Additional metadata
    @Column(name = "stock_symbol")
    private String stockSymbol;

    @Column(name = "trigger_price")
    private Double triggerPrice;

    @Column(name = "metadata", length = 2000)
    private String metadata; // JSON string for additional data

    // Enums
    public enum NotificationSeverity {
        INFO("Info"),
        SUCCESS("Success"),
        WARNING("Warning"),
        ERROR("Error");

        private final String displayName;

        NotificationSeverity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum NotificationType {
        ALERT_TRIGGERED("Alert Triggered"),
        PRICE_TARGET_HIT("Price Target Hit"),
        VOLUME_SPIKE("Volume Spike"),
        PREDICTION_UPDATE("Prediction Update"),
        SYSTEM_NOTIFICATION("System Notification"),
        SUBSCRIPTION_UPDATE("Subscription Update");

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Helper methods
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }

    public boolean isUnread() {
        return !isRead;
    }

    public String getTimeAgo() {
        if (createdAt == null) {
            return "Unknown";
        }

        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(createdAt, now);

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else if (days < 30) {
            return days + " days ago";
        } else {
            return createdAt.toLocalDate().toString();
        }
    }

    // Static factory methods
    public static AlertNotification createForAlert(Alert alert, String title, String message) {
        return AlertNotification.builder()
                .user(alert.getUser())
                .alert(alert)
                .title(title)
                .message(message)
                .severity(NotificationSeverity.INFO)
                .type(NotificationType.ALERT_TRIGGERED)
                .stockSymbol(alert.getStockSymbol())
                .triggerPrice(alert.getTriggerPrice())
                .build();
    }

    public static AlertNotification createPriceAlert(User user, String stockSymbol, Double price, String message) {
        return AlertNotification.builder()
                .user(user)
                .title("Price Alert")
                .message(message)
                .severity(NotificationSeverity.SUCCESS)
                .type(NotificationType.PRICE_TARGET_HIT)
                .stockSymbol(stockSymbol)
                .triggerPrice(price)
                .build();
    }

    public static AlertNotification createSystemNotification(User user, String title, String message,
            NotificationSeverity severity) {
        return AlertNotification.builder()
                .user(user)
                .title(title)
                .message(message)
                .severity(severity)
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .build();
    }
}