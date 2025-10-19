package com.stockapp.repository;

import com.stockapp.model.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {

    /**
     * Find usage for specific user and date
     */
    Optional<UsageTracking> findByUserIdAndDate(Long userId, LocalDate date);

    /**
     * Find usage for user within date range
     */
    List<UsageTracking> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Get current month usage for user
     */
    @Query("SELECT ut FROM UsageTracking ut WHERE ut.userId = :userId " +
            "AND ut.date BETWEEN :monthStart AND :monthEnd")
    List<UsageTracking> findCurrentMonthUsage(@Param("userId") Long userId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /**
     * Calculate total predictions used this month
     */
    @Query("SELECT COALESCE(SUM(ut.predictionsUsed), 0) FROM UsageTracking ut " +
            "WHERE ut.userId = :userId AND ut.date BETWEEN :monthStart AND :monthEnd")
    Integer getTotalPredictionsUsedThisMonth(@Param("userId") Long userId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    /**
     * Get current stocks being tracked by user
     */
    @Query("SELECT MAX(ut.stocksTracked) FROM UsageTracking ut WHERE ut.userId = :userId " +
            "AND ut.date BETWEEN :weekStart AND :weekEnd")
    Optional<Integer> getCurrentStocksTracked(@Param("userId") Long userId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    /**
     * Find top API users for analytics
     */
    @Query("SELECT ut.userId, SUM(ut.apiCallsMade) as totalCalls FROM UsageTracking ut " +
            "WHERE ut.date BETWEEN :startDate AND :endDate GROUP BY ut.userId " +
            "ORDER BY totalCalls DESC")
    List<Object[]> findTopApiUsers(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Delete old usage data (for cleanup)
     */
    void deleteByDateBefore(LocalDate cutoffDate);
}