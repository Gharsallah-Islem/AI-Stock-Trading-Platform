package com.stockapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDTO {
    private String symbol;
    private double predictedPrice;
    private LocalDate predictionDate;
    private LocalDate targetDate; // When the prediction is for
    private double confidenceScore;
    private String advice; // Added to match Prediction.Advice
    private String modelType; // Added to match Prediction.ModelType
}