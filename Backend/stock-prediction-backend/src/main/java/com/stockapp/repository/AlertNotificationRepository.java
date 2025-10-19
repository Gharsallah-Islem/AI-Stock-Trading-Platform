package com.stockapp.repository;

import com.stockapp.model.AlertNotification;
import com.stockapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    // Find notifications by user
    List<AlertNotification> findByUserOrderByCreatedAtDesc(User user);

    // Find unread notifications by user
    List<AlertNotification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // Find recent notifications
    @Query("SELECT n FROM AlertNotification n WHERE n.user = :user AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<AlertNotification> findRecentNotificationsByUser(@Param("user") User user,
            @Param("since") LocalDateTime since);

    // Count unread notifications
    @Query("SELECT COUNT(n) FROM AlertNotification n WHERE n.user = :user AND n.isRead = false")
    Long countUnreadNotificationsByUser(@Param("user") User user);

    // Find notifications by type
    List<AlertNotification> findByUserAndTypeOrderByCreatedAtDesc(User user, AlertNotification.NotificationType type);

    // Find notifications by severity
    List<AlertNotification> findByUserAndSeverityOrderByCreatedAtDesc(User user,
            AlertNotification.NotificationSeverity severity);

    // Find unsent notifications for batch processing
    @Query("SELECT n FROM AlertNotification n WHERE n.isSent = false ORDER BY n.createdAt ASC")
    List<AlertNotification> findUnsentNotifications();

    // Mark multiple notifications as read
    @Query("UPDATE AlertNotification n SET n.isRead = true, n.readAt = :readAt WHERE n.user = :user AND n.isRead = false")
    int markAllNotificationsAsReadForUser(@Param("user") User user, @Param("readAt") LocalDateTime readAt);

    // Delete old read notifications
    @Query("DELETE FROM AlertNotification n WHERE n.isRead = true AND n.readAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Get notification statistics
    @Query("SELECT n.type, COUNT(n) FROM AlertNotification n WHERE n.user = :user AND n.createdAt >= :since GROUP BY n.type")
    List<Object[]> getNotificationTypeStatistics(@Param("user") User user, @Param("since") LocalDateTime since);

    // Find paginated notifications
    @Query("SELECT n FROM AlertNotification n WHERE n.user = :user ORDER BY n.createdAt DESC")
    List<AlertNotification> findNotificationsByUserPaginated(@Param("user") User user,
            org.springframework.data.domain.Pageable pageable);
}