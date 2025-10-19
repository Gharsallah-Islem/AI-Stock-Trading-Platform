package com.stockapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.stockapp.repository")
public class StockPredictionBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockPredictionBackendApplication.class, args);
    }
}