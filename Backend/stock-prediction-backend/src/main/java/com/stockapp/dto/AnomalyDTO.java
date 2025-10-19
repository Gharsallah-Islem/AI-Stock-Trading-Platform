package com.stockapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDTO {
    private String symbol;
    private String anomalyType;
    private double value;
    private LocalDateTime timestamp;
    private String description;
}