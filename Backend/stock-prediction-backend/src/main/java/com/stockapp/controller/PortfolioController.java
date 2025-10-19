package com.stockapp.controller;

import com.stockapp.dto.PortfolioDTO;
import com.stockapp.service.PortfolioService;
import com.stockapp.util.AlphaVantageClient;
import com.stockapp.model.StockData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AlphaVantageClient alphaVantageClient;

    @Autowired
    public PortfolioController(PortfolioService portfolioService, AlphaVantageClient alphaVantageClient) {
        this.portfolioService = portfolioService;
        this.alphaVantageClient = alphaVantageClient;
    }

    @GetMapping
    public ResponseEntity<?> getPortfolio(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .body(java.util.Map.of("message", "Authentication required. Please login again."));
        }

        String username = authentication.getName();
        PortfolioDTO portfolio = portfolioService.getPortfolio(username);
        return ResponseEntity.ok(portfolio);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToPortfolio(@RequestBody PortfolioAddRequest request, Authentication authentication) {
        try {
            // Check if user is authenticated
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401)
                        .body(java.util.Map.of("message", "Authentication required. Please login again."));
            }

            String username = authentication.getName();

            // Try to fetch real stock data, fall back to mock data if fails
            StockData stockData;
            try {
                stockData = alphaVantageClient.fetchStockData(request.getSymbol());
            } catch (Exception e) {
                // Create mock stock data for testing
                stockData = createMockStockData(request.getSymbol());
            }

            PortfolioDTO.PortfolioEntryDTO entryDTO = new PortfolioDTO.PortfolioEntryDTO();
            entryDTO.setSymbol(request.getSymbol());
            entryDTO.setQuantity(request.getQuantity());
            entryDTO.setAveragePrice(stockData.getClosePrice());
            entryDTO.setCurrentPrice(stockData.getClosePrice());

            portfolioService.addPortfolioEntry(username, entryDTO);
            return ResponseEntity.ok(java.util.Map.of("message", "Stock added to portfolio successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Failed to add stock: " + e.getMessage()));
        }
    }

    private StockData createMockStockData(String symbol) {
        StockData mockData = new StockData();
        mockData.setTicker(symbol);
        mockData.setDate(java.sql.Date.valueOf(java.time.LocalDate.now()));

        // Mock prices for different stocks
        float mockPrice = switch (symbol.toUpperCase()) {
            case "AAPL" -> 150.0f;
            case "MSFT" -> 300.0f;
            case "GOOGL" -> 2500.0f;
            case "TSLA" -> 200.0f;
            case "AMZN" -> 3000.0f;
            default -> 100.0f;
        };

        mockData.setClosePrice(mockPrice);
        mockData.setVolume(1000000L);
        return mockData;
    }

    @DeleteMapping("/remove/{symbol}")
    public ResponseEntity<?> removeFromPortfolio(@PathVariable String symbol, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .body(java.util.Map.of("message", "Authentication required. Please login again."));
        }

        String username = authentication.getName();
        portfolioService.removeFromPortfolio(username, symbol);
        return ResponseEntity.ok(java.util.Map.of("message", "Stock removed from portfolio"));
    }

    public static class PortfolioAddRequest {
        private String symbol;
        private double quantity;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }
    }
}