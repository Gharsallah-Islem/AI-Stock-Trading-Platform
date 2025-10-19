package com.stockapp.controller;

import com.stockapp.dto.PredictionDTO;
import com.stockapp.service.PredictionService;
import com.stockapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final UserService userService;

    @Autowired
    public PredictionController(PredictionService predictionService, UserService userService) {
        this.predictionService = predictionService;
        this.userService = userService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getPrediction(@PathVariable String symbol,
            @RequestParam(defaultValue = "Ensemble") String modelType, Authentication authentication) {
        try {
            // Get user ID from authentication
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            // Use subscription-aware prediction method
            List<PredictionDTO> predictions = predictionService.getPredictionHistoryForUser(symbol, modelType, userId);
            return ResponseEntity.ok(predictions);
        } catch (RuntimeException e) {
            // Handle subscription limit errors specifically
            if (e.getMessage().contains("limit exceeded")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Subscription Limit Reached");
                errorResponse.put("message", e.getMessage());
                errorResponse.put("action", "upgrade");
                return ResponseEntity.status(402).body(errorResponse); // 402 Payment Required
            }

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Prediction Generation Failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Prediction Generation Failed");
            errorResponse.put("message",
                    "Could not generate a real-time prediction. The model may be offline or the stock symbol is invalid. "
                            + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/performance")
    public ResponseEntity<?> getPerformance() {
        try {
            // Create sample performance data
            Map<String, Object> performanceData = new HashMap<>();

            // Model accuracy metrics
            Map<String, Object> accuracyMetrics = new HashMap<>();
            accuracyMetrics.put("overall", 0.78);
            accuracyMetrics.put("lastMonth", 0.81);
            accuracyMetrics.put("lastWeek", 0.85);

            performanceData.put("accuracy", accuracyMetrics);

            // Performance by model type
            Map<String, Object> modelPerformance = new HashMap<>();

            Map<String, Object> lstmModel = new HashMap<>();
            lstmModel.put("accuracy", 0.76);
            lstmModel.put("mse", 0.024);
            lstmModel.put("mae", 0.18);

            Map<String, Object> xgboostModel = new HashMap<>();
            xgboostModel.put("accuracy", 0.79);
            xgboostModel.put("mse", 0.021);
            xgboostModel.put("mae", 0.16);

            Map<String, Object> ensembleModel = new HashMap<>();
            ensembleModel.put("accuracy", 0.82);
            ensembleModel.put("mse", 0.019);
            ensembleModel.put("mae", 0.15);

            modelPerformance.put("LSTM", lstmModel);
            modelPerformance.put("XGBoost", xgboostModel);
            modelPerformance.put("Ensemble", ensembleModel);

            performanceData.put("models", modelPerformance);

            // Historical performance over time
            List<Map<String, Object>> historicalPerformance = new ArrayList<>();
            LocalDate today = LocalDate.now();
            Random random = new Random(42); // Fixed seed for reproducibility

            for (int i = 0; i < 30; i++) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", today.minusDays(i));
                dataPoint.put("accuracy", 0.75 + random.nextDouble() * 0.15);
                dataPoint.put("returns", (random.nextDouble() - 0.3) * 10);
                historicalPerformance.add(dataPoint);
            }

            performanceData.put("history", historicalPerformance);

            return ResponseEntity.ok(performanceData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching model performance: " + e.getMessage());
        }
    }

    @GetMapping("/features/{symbol}")
    public ResponseEntity<?> getPredictionFeatures(@PathVariable String symbol) {
        try {
            Map<String, Object> featureImportance = new HashMap<>();

            // Add different feature categories
            List<Map<String, Object>> technicalFeatures = new ArrayList<>();
            technicalFeatures.add(createFeature("RSI", 0.85, "Overbought"));
            technicalFeatures.add(createFeature("MACD", 0.78, "Bullish Crossover"));
            technicalFeatures.add(createFeature("Moving Averages", 0.82, "Above 200 SMA"));
            technicalFeatures.add(createFeature("Bollinger Bands", 0.76, "Near Upper Band"));
            technicalFeatures.add(createFeature("Volume", 0.71, "Above Average"));

            List<Map<String, Object>> fundamentalFeatures = new ArrayList<>();
            fundamentalFeatures.add(createFeature("P/E Ratio", 0.69, "Below Sector Average"));
            fundamentalFeatures.add(createFeature("Revenue Growth", 0.72, "Strong YoY Growth"));
            fundamentalFeatures.add(createFeature("Profit Margin", 0.65, "Improving"));

            List<Map<String, Object>> sentimentFeatures = new ArrayList<>();
            sentimentFeatures.add(createFeature("News Sentiment", 0.79, "Positive"));
            sentimentFeatures.add(createFeature("Social Media", 0.68, "Bullish"));
            sentimentFeatures.add(createFeature("Analyst Ratings", 0.75, "Buy"));

            List<Map<String, Object>> macroFeatures = new ArrayList<>();
            macroFeatures.add(createFeature("Market Trend", 0.81, "Bullish"));
            macroFeatures.add(createFeature("Sector Performance", 0.74, "Outperforming"));
            macroFeatures.add(createFeature("Interest Rates", 0.70, "Stable"));
            macroFeatures.add(createFeature("VIX", 0.73, "Low Volatility"));

            featureImportance.put("technical", technicalFeatures);
            featureImportance.put("fundamental", fundamentalFeatures);
            featureImportance.put("sentiment", sentimentFeatures);
            featureImportance.put("macro", macroFeatures);

            // Add overall prediction confidence
            featureImportance.put("overallConfidence", 0.82);
            featureImportance.put("predictionDate", LocalDate.now());
            featureImportance.put("symbol", symbol);

            return ResponseEntity.ok(featureImportance);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching prediction features: " + e.getMessage());
        }
    }

    private Map<String, Object> createFeature(String name, double importance, String signal) {
        Map<String, Object> feature = new HashMap<>();
        feature.put("name", name);
        feature.put("importance", importance);
        feature.put("signal", signal);
        return feature;
    }

    @PostMapping
    public ResponseEntity<?> createPrediction(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            // Extract prediction parameters from request
            String symbol = (String) request.get("symbol");
            String timeframe = (String) request.get("timeframe");
            String targetDateStr = (String) request.get("targetDate");
            String modelType = (String) request.get("modelType");

            // Log the received request for debugging
            System.out.println("Received prediction request: " + request);

            if (symbol == null || symbol.trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Symbol is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Convert targetDate to timeframe if provided, or use direct targetDate
            LocalDate targetDate = null;
            if (targetDateStr != null && !targetDateStr.trim().isEmpty()) {
                try {
                    targetDate = LocalDate.parse(targetDateStr);
                    // Calculate timeframe from targetDate
                    long daysUntilTarget = LocalDate.now().until(targetDate).getDays();
                    if (daysUntilTarget <= 1)
                        timeframe = "1d";
                    else if (daysUntilTarget <= 3)
                        timeframe = "3d";
                    else if (daysUntilTarget <= 7)
                        timeframe = "1w";
                    else if (daysUntilTarget <= 14)
                        timeframe = "2w";
                    else if (daysUntilTarget <= 30)
                        timeframe = "1m";
                    else
                        timeframe = "3m";
                } catch (Exception e) {
                    System.out.println("Error parsing targetDate: " + targetDateStr + ", using default");
                    timeframe = "1d";
                }
            }

            if (timeframe == null) {
                timeframe = "1d"; // Default to 1 day
            }

            // Set default model type if not provided
            if (modelType == null || modelType.trim().isEmpty()) {
                modelType = "LSTM"; // Default model type
            }

            // Generate prediction with improved method
            PredictionDTO prediction = predictionService.generatePredictionWithTargetDate(symbol, targetDate,
                    modelType);

            // Ensure all required fields are set
            if (prediction.getPredictionDate() == null) {
                prediction.setPredictionDate(LocalDate.now());
            }
            if (prediction.getTargetDate() == null && targetDate != null) {
                prediction.setTargetDate(targetDate);
            }
            if (prediction.getTargetDate() == null) {
                prediction.setTargetDate(LocalDate.now().plusDays(1));
            }

            // Save prediction if user is authenticated
            if (authentication != null) {
                predictionService.savePrediction(prediction);
            }

            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            // Log the full error for debugging
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating prediction");
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
            errorResponse.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Generate a single prediction with subscription awareness
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generatePrediction(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "LSTM") String modelType,
            @RequestParam(required = false) String timeframe,
            Authentication authentication) {
        try {
            // Get user ID from authentication
            String username = authentication.getName();
            Long userId = userService.getUserIdByUsername(username);

            // Check if user can make predictions
            if (!predictionService.canUserMakePrediction(userId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Subscription Limit Reached");
                errorResponse.put("message",
                        "You have reached your prediction limit for this period. Please upgrade to continue.");
                errorResponse.put("action", "upgrade");
                return ResponseEntity.status(402).body(errorResponse); // 402 Payment Required
            }

            // Generate prediction with proper subscription tracking
            PredictionDTO prediction = predictionService.generatePrediction(symbol,
                    timeframe != null ? timeframe : "1d", modelType);

            // Record prediction usage
            predictionService.recordPredictionUsage(userId);

            // Save prediction to database
            predictionService.savePrediction(prediction);

            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating prediction");
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
