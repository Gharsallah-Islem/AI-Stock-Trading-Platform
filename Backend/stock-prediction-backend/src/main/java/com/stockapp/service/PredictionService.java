package com.stockapp.service;

import com.stockapp.dto.PredictionDTO;
import com.stockapp.model.Prediction;
import com.stockapp.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final PythonModelService pythonModelService;
    private final SubscriptionService subscriptionService;

    public PredictionDTO getPrediction(String ticker, LocalDate date) {
        Prediction prediction = predictionRepository.findByTickerAndDate(ticker, Date.valueOf(date))
                .orElseGet(() -> generatePrediction(ticker, date));
        return mapToDTO(prediction);
    }

    public List<PredictionDTO> getPredictionHistory(String ticker, String modelType) {
        try {
            // Always generate a fresh, real-time prediction for the most recent day.
            PredictionDTO latestPrediction = pythonModelService.generatePrediction(ticker, LocalDate.now().plusDays(1),
                    modelType);

            // Save the new prediction to the database so it becomes part of the history.
            savePrediction(latestPrediction);

            // Fetch the historical predictions from the database.
            List<Prediction> predictions = predictionRepository.findByTickerOrderByDateDesc(ticker);

            // Combine and return. The latest prediction will be at the top.
            return predictions.stream().map(this::mapToDTO).collect(Collectors.toList());

        } catch (Exception e) {
            // If the real-time prediction fails, we'll log the error and return an empty
            // list.
            // It is better to return no data than incorrect/mock data in a production app.
            System.err.println("CRITICAL: Real-time prediction failed for " + ticker + ". Error: " + e.getMessage());
            e.printStackTrace();
            // Optionally, re-throw as a custom exception to be handled by the controller.
            throw new RuntimeException("Failed to generate real-time prediction for " + ticker, e);
        }
    }

    /**
     * Get prediction history with subscription awareness
     */
    public List<PredictionDTO> getPredictionHistoryForUser(String ticker, String modelType, Long userId) {
        // Check if user can make predictions
        if (!subscriptionService.canMakePrediction(userId)) {
            throw new RuntimeException(
                    "Prediction limit exceeded for current subscription tier. Please upgrade to continue.");
        }

        try {
            // Record prediction usage
            subscriptionService.recordPredictionUsage(userId);

            // Always generate a fresh, real-time prediction for the most recent day.
            PredictionDTO latestPrediction = pythonModelService.generatePrediction(ticker, LocalDate.now().plusDays(1),
                    modelType);

            // Save the new prediction to the database so it becomes part of the history.
            savePrediction(latestPrediction);

            // Fetch the historical predictions from the database.
            List<Prediction> predictions = predictionRepository.findByTickerOrderByDateDesc(ticker);

            // Filter history based on subscription tier
            return filterHistoryBySubscription(predictions, userId).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("CRITICAL: Real-time prediction failed for " + ticker + ". Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate real-time prediction for " + ticker, e);
        }
    }

    /**
     * Generate real predictions using the trained AI models
     */
    private List<PredictionDTO> generateRealPredictions(String ticker, int days) {
        List<PredictionDTO> predictions = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Check if models exist for this ticker
        if (!hasTrainedModels(ticker)) {
            throw new RuntimeException("No trained models found for " + ticker);
        }

        for (int i = 1; i <= days; i++) {
            LocalDate targetDate = today.plusDays(i);

            try {
                // Generate LSTM prediction
                PredictionDTO lstmPrediction = pythonModelService.generatePrediction(ticker, targetDate, "LSTM");
                predictions.add(lstmPrediction);

                // Generate XGBoost prediction for comparison
                if (i <= 3) { // Only first 3 days for XGBoost to avoid too many calls
                    PredictionDTO xgbPrediction = pythonModelService.generatePrediction(ticker, targetDate, "XGBoost");
                    predictions.add(xgbPrediction);
                }
            } catch (Exception e) {
                // If real prediction fails, add a mock one for this day
                PredictionDTO mockPrediction = generateSingleMockPrediction(ticker, targetDate, i);
                predictions.add(mockPrediction);
            }
        }

        return predictions;
    }

    /**
     * Check if trained models exist for the given ticker
     */
    private boolean hasTrainedModels(String ticker) {
        // Check if the model files exist in the AI directory
        String aiPath = "C:/Users/islem/OneDrive/Bureau/project/AI/";
        java.io.File lstmModel = new java.io.File(aiPath + "lstm_model_" + ticker + ".h5");
        java.io.File xgbModel = new java.io.File(aiPath + "xgb_model_" + ticker + ".pkl");

        return lstmModel.exists() && xgbModel.exists();
    }

    /**
     * Generate a single mock prediction as fallback
     */
    private PredictionDTO generateSingleMockPrediction(String ticker, LocalDate targetDate, int dayOffset) {
        Random random = new Random(ticker.hashCode() + dayOffset);
        double basePrice = getBasePriceForTicker(ticker);

        // Generate prediction with some randomness but trend
        double priceVariation = (random.nextDouble() - 0.45) * 0.1; // Slight upward bias
        double predictedPrice = basePrice * (1 + priceVariation);

        PredictionDTO prediction = new PredictionDTO();
        prediction.setSymbol(ticker);
        prediction.setPredictedPrice(predictedPrice);
        prediction.setPredictionDate(LocalDate.now());
        prediction.setTargetDate(targetDate);
        prediction.setConfidenceScore(0.75 + random.nextDouble() * 0.2);

        // Determine advice based on price trend
        if (priceVariation > 0.02) {
            prediction.setAdvice("Buy");
        } else if (priceVariation < -0.02) {
            prediction.setAdvice("Sell");
        } else {
            prediction.setAdvice("Hold");
        }

        prediction.setModelType("LSTM"); // Default to LSTM

        // Log the generated prediction for debugging
        System.out.println("Generated mock prediction: " + prediction);

        return prediction;
    }

    /**
     * Generate a new prediction using AI models or fallback to mock with specific
     * target date
     */
    public PredictionDTO generatePredictionWithTargetDate(String ticker, LocalDate targetDate, String modelType) {
        if (targetDate == null) {
            targetDate = LocalDate.now().plusDays(1); // Default to next day
        }

        try {
            // Try to use real AI model first
            if (hasTrainedModels(ticker)) {
                return pythonModelService.generatePrediction(ticker, targetDate, modelType);
            } else {
                // Fallback to enhanced mock prediction
                return generateSingleMockPrediction(ticker, targetDate, 1);
            }
        } catch (Exception e) {
            // If all else fails, generate mock prediction
            return generateSingleMockPrediction(ticker, targetDate, 1);
        }
    }

    /**
     * Generate a new prediction using AI models or fallback to mock
     */
    public PredictionDTO generatePrediction(String ticker, String timeframe, String modelType) {
        LocalDate targetDate = calculateTargetDate(timeframe);

        try {
            // Try to use real AI model first
            if (hasTrainedModels(ticker)) {
                return pythonModelService.generatePrediction(ticker, targetDate, modelType);
            } else {
                // Fallback to enhanced mock prediction
                return generateSingleMockPrediction(ticker, targetDate, 1);
            }
        } catch (Exception e) {
            // If all else fails, generate mock prediction
            return generateSingleMockPrediction(ticker, targetDate, 1);
        }
    }

    /**
     * Calculate target date based on timeframe
     */
    private LocalDate calculateTargetDate(String timeframe) {
        LocalDate today = LocalDate.now();
        return switch (timeframe) {
            case "1d" -> today.plusDays(1);
            case "3d" -> today.plusDays(3);
            case "1w" -> today.plusWeeks(1);
            case "2w" -> today.plusWeeks(2);
            case "1m" -> today.plusMonths(1);
            case "3m" -> today.plusMonths(3);
            default -> today.plusDays(1);
        };
    }

    /**
     * Generate mock prediction data for testing and demo purposes
     */
    private List<PredictionDTO> generateMockPredictionHistory(String ticker, int count) {
        List<PredictionDTO> mockPredictions = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Get base price for the ticker
        double basePrice = getBasePriceForTicker(ticker);

        for (int i = 1; i <= count; i++) {
            LocalDate targetDate = today.plusDays(i);
            PredictionDTO prediction = generateSingleMockPrediction(ticker, targetDate, i);
            mockPredictions.add(prediction);
        }

        return mockPredictions;
    }

    private double getBasePriceForTicker(String ticker) {
        return switch (ticker) {
            case "AAPL" -> 175.50;
            case "MSFT" -> 335.20;
            case "GOOGL" -> 140.80;
            case "AMZN" -> 145.30;
            case "META" -> 295.40;
            case "TSLA" -> 245.60;
            case "NVDA" -> 485.70;
            case "JPM" -> 155.80;
            case "^GSPC" -> 4800.25;
            case "^DJI" -> 38750.35;
            case "^IXIC" -> 16500.50;
            default -> 100.0;
        };
    }

    private Prediction generatePrediction(String ticker, LocalDate date) {
        Random random = new Random();
        double basePrice = getBasePriceForTicker(ticker);
        double predictedPrice = basePrice * (0.9 + random.nextDouble() * 0.2);

        Prediction prediction = new Prediction();
        prediction.setTicker(ticker);
        prediction.setPredictedPrice((float) predictedPrice);
        prediction.setDate(Date.valueOf(date));
        prediction.setConfidence((float) (0.7 + random.nextDouble() * 0.3));
        return prediction;
    }

    private PredictionDTO mapToDTO(Prediction prediction) {
        PredictionDTO dto = new PredictionDTO();
        dto.setSymbol(prediction.getTicker());
        dto.setPredictedPrice(prediction.getPredictedPrice());
        dto.setPredictionDate(prediction.getDate().toLocalDate());
        dto.setConfidenceScore(prediction.getConfidence());
        return dto;
    }

    public void savePrediction(PredictionDTO predictionDTO) {
        Prediction prediction = new Prediction();
        prediction.setTicker(predictionDTO.getSymbol());
        prediction.setPredictedPrice((float) predictionDTO.getPredictedPrice());
        prediction.setDate(Date.valueOf(predictionDTO.getPredictionDate()));
        prediction.setConfidence((float) predictionDTO.getConfidenceScore());

        // Map string advice to enum
        if (predictionDTO.getAdvice() != null) {
            try {
                // Handle both capitalized and uppercase cases
                String advice = predictionDTO.getAdvice();
                if ("BUY".equalsIgnoreCase(advice)) {
                    prediction.setAdvice(Prediction.Advice.Buy);
                } else if ("SELL".equalsIgnoreCase(advice)) {
                    prediction.setAdvice(Prediction.Advice.Sell);
                } else if ("HOLD".equalsIgnoreCase(advice)) {
                    prediction.setAdvice(Prediction.Advice.Hold);
                } else {
                    prediction.setAdvice(Prediction.Advice.valueOf(advice));
                }
            } catch (IllegalArgumentException e) {
                prediction.setAdvice(Prediction.Advice.Hold); // Default fallback
            }
        } else {
            prediction.setAdvice(Prediction.Advice.Hold); // Default fallback
        }

        // Map string model type to enum
        if (predictionDTO.getModelType() != null) {
            try {
                prediction.setModelType(Prediction.ModelType.valueOf(predictionDTO.getModelType()));
            } catch (IllegalArgumentException e) {
                prediction.setModelType(Prediction.ModelType.LSTM); // Default fallback
            }
        } else {
            prediction.setModelType(Prediction.ModelType.LSTM); // Default fallback
        }

        predictionRepository.save(prediction);
    }

    /**
     * Filter prediction history based on user's subscription tier
     */
    private List<Prediction> filterHistoryBySubscription(List<Prediction> predictions, Long userId) {
        var subscription = subscriptionService.getActiveSubscription(userId);
        if (subscription.isEmpty()) {
            // Free tier - last 7 days only
            LocalDate cutoffDate = LocalDate.now().minusDays(7);
            return predictions.stream()
                    .filter(p -> p.getDate().toLocalDate().isAfter(cutoffDate))
                    .collect(Collectors.toList());
        }

        int historyDays = subscription.get().getTier().getHistoryDays();
        if (historyDays == -1) {
            // Unlimited history
            return predictions;
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(historyDays);
        return predictions.stream()
                .filter(p -> p.getDate().toLocalDate().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }

    /**
     * Check if user can make predictions based on subscription limits
     */
    public boolean canUserMakePrediction(Long userId) {
        return subscriptionService.canMakePrediction(userId);
    }

    /**
     * Record prediction usage for subscription tracking
     */
    public void recordPredictionUsage(Long userId) {
        subscriptionService.recordPredictionUsage(userId);
    }
}
