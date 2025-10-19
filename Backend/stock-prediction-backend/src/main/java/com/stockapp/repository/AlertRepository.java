package com.stockapp.repository;

import com.stockapp.model.Alert;
import com.stockapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Find alerts by user
    List<Alert> findByUserOrderByCreatedAtDesc(User user);

    // Find active alerts by user
    List<Alert> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user);

    // Find triggered alerts by user
    List<Alert> findByUserAndIsTriggeredTrueOrderByTriggeredAtDesc(User user);

    // Find active alerts for a specific stock
    List<Alert> findByUserAndStockSymbolAndIsActiveTrueOrderByCreatedAtDesc(User user, String stockSymbol);

    // Find alerts that need to be checked (active, not triggered)
    @Query("SELECT a FROM Alert a WHERE a.isActive = true AND a.isTriggered = false")
    List<Alert> findActiveAlertsForMonitoring();

    // Find alerts by stock symbol for monitoring
    @Query("SELECT a FROM Alert a WHERE a.stockSymbol = :stockSymbol AND a.isActive = true AND a.isTriggered = false")
    List<Alert> findActiveAlertsByStockSymbol(@Param("stockSymbol") String stockSymbol);

    // Count active alerts by user
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.user = :user AND a.isActive = true")
    Long countActiveAlertsByUser(@Param("user") User user);

    // Count triggered alerts by user in a time period
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.user = :user AND a.isTriggered = true AND a.triggeredAt >= :since")
    Long countTriggeredAlertsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    // Find alerts by user and alert type
    List<Alert> findByUserAndAlertTypeOrderByCreatedAtDesc(User user, Alert.AlertType alertType);

    // Find recently created alerts
    @Query("SELECT a FROM Alert a WHERE a.user = :user AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlertsByUser(@Param("user") User user, @Param("since") LocalDateTime since);

    // Find alerts that will trigger soon (for predictive notifications)
    @Query("SELECT a FROM Alert a WHERE a.isActive = true AND a.isTriggered = false " +
            "AND a.alertType = 'PRICE_TARGET' " +
            "AND ((a.condition = 'ABOVE' AND :currentPrice >= (a.targetValue * 0.95)) " +
            "OR (a.condition = 'BELOW' AND :currentPrice <= (a.targetValue * 1.05)))")
    List<Alert> findAlertsNearTrigger(@Param("currentPrice") Double currentPrice);

    // Delete old triggered alerts
    @Query("DELETE FROM Alert a WHERE a.isTriggered = true AND a.triggeredAt < :cutoffDate")
    int deleteOldTriggeredAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Statistics queries
    @Query("SELECT a.alertType, COUNT(a) FROM Alert a WHERE a.user = :user GROUP BY a.alertType")
    List<Object[]> getAlertTypeStatistics(@Param("user") User user);

    @Query("SELECT a.stockSymbol, COUNT(a) FROM Alert a WHERE a.user = :user GROUP BY a.stockSymbol ORDER BY COUNT(a) DESC")
    List<Object[]> getTopWatchedStocks(@Param("user") User user);

    // Performance metrics
    @Query("SELECT " +
            "COUNT(CASE WHEN a.isTriggered = true THEN 1 END) as triggered, " +
            "COUNT(a) as total, " +
            "(COUNT(CASE WHEN a.isTriggered = true THEN 1 END) * 100.0 / COUNT(a)) as accuracy " +
            "FROM Alert a WHERE a.user = :user AND a.createdAt >= :since")
    Object[] getAlertAccuracyStats(@Param("user") User user, @Param("since") LocalDateTime since);
}