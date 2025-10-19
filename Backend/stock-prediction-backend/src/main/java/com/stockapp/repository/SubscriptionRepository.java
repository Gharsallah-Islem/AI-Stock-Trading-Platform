package com.stockapp.repository;

import com.stockapp.model.Subscription;
import com.stockapp.model.Subscription.SubscriptionStatus;
import com.stockapp.model.Subscription.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find active subscription for a user
     */
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status = 'ACTIVE' " +
            "AND (s.endDate IS NULL OR s.endDate > :currentDate)")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") Long userId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Find all subscriptions for a user
     */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find subscriptions by status
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find subscriptions expiring soon
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :startDate AND :endDate")
    List<Subscription> findExpiringSoon(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find subscriptions by tier
     */
    List<Subscription> findByTier(SubscriptionTier tier);

    /**
     * Find by Stripe subscription ID
     */
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Count active subscribers by tier
     */
    @Query("SELECT s.tier, COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' " +
            "AND (s.endDate IS NULL OR s.endDate > :currentDate) GROUP BY s.tier")
    List<Object[]> countActiveSubscribersByTier(@Param("currentDate") LocalDate currentDate);

    /**
     * Calculate monthly recurring revenue
     */
    @Query("SELECT SUM(s.monthlyPrice) FROM Subscription s WHERE s.status = 'ACTIVE' " +
            "AND (s.endDate IS NULL OR s.endDate > :currentDate)")
    Optional<Double> calculateMonthlyRecurringRevenue(@Param("currentDate") LocalDate currentDate);
}