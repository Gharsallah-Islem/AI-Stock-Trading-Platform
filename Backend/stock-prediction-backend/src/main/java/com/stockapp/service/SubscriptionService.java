package com.stockapp.service;

import com.stockapp.dto.SubscriptionDTO;
import com.stockapp.exception.ApiException;
import com.stockapp.exception.ResourceNotFoundException;
import com.stockapp.model.Subscription;
import com.stockapp.model.Subscription.SubscriptionStatus;
import com.stockapp.model.Subscription.SubscriptionTier;
import com.stockapp.model.UsageTracking;
import com.stockapp.repository.SubscriptionRepository;
import com.stockapp.repository.UsageTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UsageTrackingRepository usageTrackingRepository;

    /**
     * Get user's current active subscription
     */
    public Optional<Subscription> getActiveSubscription(Long userId) {
        return subscriptionRepository.findActiveSubscriptionByUserId(userId, LocalDate.now());
    }

    /**
     * Get user's subscription tier (returns FREE if no active subscription)
     */
    public SubscriptionTier getUserTier(Long userId) {
        return getActiveSubscription(userId)
                .map(Subscription::getTier)
                .orElse(SubscriptionTier.FREE);
    }

    /**
     * Check if user can make a prediction
     */
    public boolean canMakePrediction(Long userId) {
        Subscription subscription = getActiveSubscription(userId).orElse(createFreeSubscription(userId));

        if (subscription.getTier().hasUnlimitedPredictions()) {
            return true;
        }

        // Check current month usage
        LocalDate monthStart = YearMonth.now().atDay(1);
        LocalDate monthEnd = YearMonth.now().atEndOfMonth();

        Integer usedThisMonth = usageTrackingRepository.getTotalPredictionsUsedThisMonth(
                userId, monthStart, monthEnd);

        return usedThisMonth < subscription.getTier().getMonthlyPredictions();
    }

    /**
     * Check if user can add more stocks to watchlist
     */
    public boolean canAddStock(Long userId) {
        Subscription subscription = getActiveSubscription(userId).orElse(createFreeSubscription(userId));

        if (subscription.getTier().hasUnlimitedStocks()) {
            return true;
        }

        // Check current stocks being tracked
        LocalDate weekStart = LocalDate.now().minusDays(7);
        LocalDate weekEnd = LocalDate.now();

        Integer currentStocks = usageTrackingRepository.getCurrentStocksTracked(
                userId, weekStart, weekEnd).orElse(0);

        return currentStocks < subscription.getTier().getMaxStocks();
    }

    /**
     * Record prediction usage
     */
    @Transactional
    public void recordPredictionUsage(Long userId) {
        if (!canMakePrediction(userId)) {
            throw new ApiException("Prediction limit exceeded for current subscription tier");
        }

        UsageTracking usage = getOrCreateTodayUsage(userId);
        usage.incrementPredictions();
        usageTrackingRepository.save(usage);

        log.info("Recorded prediction usage for user {}", userId);
    }

    /**
     * Record API call usage
     */
    @Transactional
    public void recordApiUsage(Long userId) {
        UsageTracking usage = getOrCreateTodayUsage(userId);
        usage.incrementApiCalls();
        usageTrackingRepository.save(usage);
    }

    /**
     * Update stocks tracked count
     */
    @Transactional
    public void updateStocksTracked(Long userId, int stockCount) {
        if (!canAddStock(userId) && stockCount > getUserTier(userId).getMaxStocks()) {
            throw new ApiException("Stock tracking limit exceeded for current subscription tier");
        }

        UsageTracking usage = getOrCreateTodayUsage(userId);
        usage.updateStocksTracked(stockCount);
        usageTrackingRepository.save(usage);
    }

    /**
     * Create or upgrade subscription
     */
    @Transactional
    public Subscription createOrUpgradeSubscription(Long userId, SubscriptionTier newTier, String paymentMethod) {
        // Cancel existing subscription if upgrading
        Optional<Subscription> existingSubscription = getActiveSubscription(userId);
        if (existingSubscription.isPresent()) {
            existingSubscription.get().setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(existingSubscription.get());
        }

        // Create new subscription
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setTier(newTier);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setMonthlyPrice(newTier.getPrice());
        subscription.setPaymentMethod(paymentMethod);

        if (newTier != SubscriptionTier.FREE) {
            subscription.setEndDate(LocalDate.now().plusMonths(1));
        }

        subscription = subscriptionRepository.save(subscription);
        log.info("Created {} subscription for user {}", newTier, userId);

        return subscription;
    }

    /**
     * Cancel subscription
     */
    @Transactional
    public void cancelSubscription(Long userId) {
        Subscription subscription = getActiveSubscription(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        log.info("Cancelled subscription for user {}", userId);
    }

    /**
     * Get user's usage statistics
     */
    public SubscriptionDTO getUserSubscriptionDetails(Long userId) {
        Subscription subscription = getActiveSubscription(userId).orElse(createFreeSubscription(userId));

        // Get current month usage
        LocalDate monthStart = YearMonth.now().atDay(1);
        LocalDate monthEnd = YearMonth.now().atEndOfMonth();

        Integer predictionsUsed = usageTrackingRepository.getTotalPredictionsUsedThisMonth(
                userId, monthStart, monthEnd);

        Integer stocksTracked = usageTrackingRepository.getCurrentStocksTracked(
                userId, LocalDate.now().minusDays(7), LocalDate.now()).orElse(0);

        return SubscriptionDTO.builder()
                .tier(subscription.getTier())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .monthlyPrice(subscription.getMonthlyPrice())
                .predictionsUsed(predictionsUsed)
                .predictionsLimit(subscription.getTier().getMonthlyPredictions())
                .stocksTracked(stocksTracked)
                .stocksLimit(subscription.getTier().getMaxStocks())
                .canMakePrediction(canMakePrediction(userId))
                .canAddStock(canAddStock(userId))
                .build();
    }

    /**
     * Get all subscription tiers with their features
     */
    public List<SubscriptionDTO> getAllTiers() {
        return List.of(SubscriptionTier.values()).stream()
                .map(tier -> SubscriptionDTO.builder()
                        .tier(tier)
                        .monthlyPrice(tier.getPrice())
                        .predictionsLimit(tier.getMonthlyPredictions())
                        .stocksLimit(tier.getMaxStocks())
                        .historyDays(tier.getHistoryDays())
                        .features(getFeaturesForTier(tier))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Process subscription renewals (scheduled task)
     */
    @Transactional
    public void processRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Subscription> expiring = subscriptionRepository.findExpiringSoon(today, tomorrow);

        for (Subscription subscription : expiring) {
            if (subscription.getAutoRenew()) {
                // Extend subscription by one month
                subscription.setEndDate(subscription.getEndDate().plusMonths(1));
                subscriptionRepository.save(subscription);
                log.info("Renewed subscription for user {}", subscription.getUserId());
            } else {
                // Mark as expired
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(subscription);
                log.info("Expired subscription for user {}", subscription.getUserId());
            }
        }
    }

    // Helper methods

    private Subscription createFreeSubscription(Long userId) {
        Subscription freeSubscription = new Subscription();
        freeSubscription.setUserId(userId);
        freeSubscription.setTier(SubscriptionTier.FREE);
        freeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        freeSubscription.setStartDate(LocalDate.now());
        freeSubscription.setMonthlyPrice(SubscriptionTier.FREE.getPrice());
        return subscriptionRepository.save(freeSubscription);
    }

    private UsageTracking getOrCreateTodayUsage(Long userId) {
        LocalDate today = LocalDate.now();
        return usageTrackingRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    UsageTracking newUsage = new UsageTracking();
                    newUsage.setUserId(userId);
                    newUsage.setDate(today);
                    return usageTrackingRepository.save(newUsage);
                });
    }

    private List<String> getFeaturesForTier(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> List.of(
                    "5 predictions per month",
                    "Track up to 3 stocks",
                    "7 days history",
                    "Basic LSTM model",
                    "Community support");
            case PREMIUM -> List.of(
                    "Unlimited predictions",
                    "Track up to 25 stocks",
                    "6 months history",
                    "LSTM + XGBoost models",
                    "Real-time alerts",
                    "Portfolio optimization",
                    "Advanced charts",
                    "Email support");
            case PRO -> List.of(
                    "Unlimited predictions",
                    "Unlimited stocks",
                    "2 years history",
                    "All AI models + Ensemble",
                    "Custom model training",
                    "API access",
                    "Automated trading signals",
                    "Risk management tools",
                    "Priority support",
                    "White-label options");
        };
    }
}