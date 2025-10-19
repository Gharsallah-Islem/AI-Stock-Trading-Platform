package com.stockapp.controller;

import com.stockapp.dto.SubscriptionDTO;
import com.stockapp.model.Subscription.SubscriptionTier;
import com.stockapp.service.SubscriptionService;
import com.stockapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    /**
     * Get current user's subscription details
     */
    @GetMapping("/current")
    public ResponseEntity<SubscriptionDTO> getCurrentSubscription(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            SubscriptionDTO subscription = subscriptionService.getUserSubscriptionDetails(userId);
            return ResponseEntity.ok(subscription);

        } catch (Exception e) {
            log.error("Error fetching current subscription: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all available subscription tiers
     */
    @GetMapping("/tiers")
    public ResponseEntity<List<SubscriptionDTO>> getSubscriptionTiers() {
        try {
            List<SubscriptionDTO> tiers = subscriptionService.getAllTiers();
            return ResponseEntity.ok(tiers);

        } catch (Exception e) {
            log.error("Error fetching subscription tiers: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if user can perform specific actions
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getPermissions(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            Map<String, Object> permissions = new HashMap<>();
            permissions.put("canMakePrediction", subscriptionService.canMakePrediction(userId));
            permissions.put("canAddStock", subscriptionService.canAddStock(userId));
            permissions.put("currentTier", subscriptionService.getUserTier(userId));

            return ResponseEntity.ok(permissions);

        } catch (Exception e) {
            log.error("Error checking permissions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Upgrade subscription
     */
    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeSubscription(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            String tierName = (String) request.get("tier");
            String paymentMethod = (String) request.get("paymentMethod");

            SubscriptionTier newTier = SubscriptionTier.valueOf(tierName.toUpperCase());

            subscriptionService.createOrUpgradeSubscription(userId, newTier, paymentMethod);

            // Return updated subscription details
            SubscriptionDTO updatedSubscription = subscriptionService.getUserSubscriptionDetails(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription upgraded successfully");
            response.put("subscription", updatedSubscription);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid subscription tier");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("Error upgrading subscription: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Upgrade failed");
            error.put("message", "Could not process subscription upgrade");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            subscriptionService.cancelSubscription(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Subscription cancelled successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cancellation failed");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Check usage limits before making prediction
     */
    @GetMapping("/check-prediction-limit")
    public ResponseEntity<Map<String, Object>> checkPredictionLimit(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            boolean canPredict = subscriptionService.canMakePrediction(userId);
            SubscriptionDTO subscription = subscriptionService.getUserSubscriptionDetails(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("canMakePrediction", canPredict);
            response.put("predictionsUsed", subscription.getPredictionsUsed());
            response.put("predictionsLimit", subscription.getPredictionsLimit());
            response.put("remainingPredictions", subscription.getRemainingPredictions());
            response.put("currentTier", subscription.getTier());

            if (!canPredict) {
                response.put("message", "Prediction limit reached. Please upgrade your subscription.");
                response.put("upgradeUrl", "/subscriptions/upgrade");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking prediction limit: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Record prediction usage (called after successful prediction)
     */
    @PostMapping("/record-prediction")
    public ResponseEntity<?> recordPredictionUsage(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            subscriptionService.recordPredictionUsage(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Prediction usage recorded");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error recording prediction usage: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to record usage");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}