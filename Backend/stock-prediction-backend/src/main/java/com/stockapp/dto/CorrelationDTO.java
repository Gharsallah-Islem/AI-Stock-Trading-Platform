package com.stockapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationDTO {
    private String symbol;
    private String correlatedSymbol;
    private double correlationCoefficient;
    private String timeframe;
}