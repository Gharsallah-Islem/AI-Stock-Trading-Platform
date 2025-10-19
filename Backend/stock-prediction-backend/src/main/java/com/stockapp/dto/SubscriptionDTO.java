package com.stockapp.dto;

import com.stockapp.model.Subscription.SubscriptionStatus;
import com.stockapp.model.Subscription.SubscriptionTier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SubscriptionDTO {

    private Long id;
    private SubscriptionTier tier;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyPrice;

    // Usage information
    private Integer predictionsUsed;
    private Integer predictionsLimit; // -1 for unlimited
    private Integer stocksTracked;
    private Integer stocksLimit; // -1 for unlimited
    private Integer historyDays;
    private Integer apiCallsUsed;

    // Permissions
    private Boolean canMakePrediction;
    private Boolean canAddStock;
    private Boolean hasApiAccess;
    private Boolean hasAdvancedFeatures;

    // Plan features
    private List<String> features;
    private String description;

    // Billing information
    private String paymentMethod;
    private Boolean autoRenew;
    private LocalDate nextBillingDate;

    // Computed fields
    public String getTierDisplayName() {
        return tier != null ? tier.getDisplayName() : "Unknown";
    }

    public boolean isUnlimitedPredictions() {
        return predictionsLimit != null && predictionsLimit == -1;
    }

    public boolean isUnlimitedStocks() {
        return stocksLimit != null && stocksLimit == -1;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE &&
                (endDate == null || endDate.isAfter(LocalDate.now()));
    }

    public int getRemainingPredictions() {
        if (isUnlimitedPredictions())
            return -1;
        if (predictionsLimit == null || predictionsUsed == null)
            return 0;
        return Math.max(0, predictionsLimit - predictionsUsed);
    }

    public int getRemainingStocks() {
        if (isUnlimitedStocks())
            return -1;
        if (stocksLimit == null || stocksTracked == null)
            return 0;
        return Math.max(0, stocksLimit - stocksTracked);
    }

    public double getUsagePercentage() {
        if (isUnlimitedPredictions())
            return 0.0;
        if (predictionsLimit == null || predictionsLimit == 0)
            return 0.0;
        if (predictionsUsed == null)
            return 0.0;
        return Math.min(100.0, (predictionsUsed * 100.0) / predictionsLimit);
    }
}