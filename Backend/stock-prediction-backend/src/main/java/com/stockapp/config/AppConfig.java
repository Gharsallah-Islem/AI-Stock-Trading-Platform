package com.stockapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String version = "1.0.0";
    private String environment = "development";
    private int maxRetries = 3;
    private int requestTimeout = 30000;
    private boolean enableCaching = true;
    private int cacheTtl = 3600;

    // Stock prediction settings
    private PredictionConfig prediction = new PredictionConfig();

    @Data
    public static class PredictionConfig {
        private int lookbackDays = 30;
        private double confidenceThreshold = 0.7;
        private int maxPredictionsPerDay = 10;
        private boolean enableRealTimeUpdates = true;
    }
}