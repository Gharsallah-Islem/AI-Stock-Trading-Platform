package com.stockapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {
    private String username;
    private List<PortfolioEntryDTO> holdings;
    private double totalValue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioEntryDTO {
        private String symbol;
        private double quantity;
        private double averagePrice;
        private double currentPrice;
    }
}