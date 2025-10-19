package com.stockapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteDTO {
    private double c;    // Current price
    private double d;    // Change
    private double dp;   // Percent change
    private double h;    // High price of the day
    private double l;    // Low price of the day
    private double o;    // Open price of the day
    private double pc;   // Previous close price
    private long t;      // Timestamp
    private String symbol; // Stock symbol
}
