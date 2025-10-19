package com.stockapp.controller;

import com.stockapp.dto.StockDataDTO;
import com.stockapp.dto.StockQuoteDTO;
import com.stockapp.service.PythonModelService;
import com.stockapp.service.StockService;
import com.stockapp.service.WatchlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    private final StockService stockService;
    private final WatchlistService watchlistService;
    private final PythonModelService pythonModelService;

    @Autowired
    public StockController(StockService stockService, WatchlistService watchlistService,
            PythonModelService pythonModelService) {
        this.stockService = stockService;
        this.watchlistService = watchlistService;
        this.pythonModelService = pythonModelService;
    }

    @GetMapping("/watchlist")
    public ResponseEntity<?> getWatchlist(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                // Return empty watchlist for unauthenticated users
                return ResponseEntity.ok(new ArrayList<StockDataDTO>());
            }

            String username = authentication.getName();
            List<StockDataDTO> watchlist = watchlistService.getUserWatchlist(username);
            return ResponseEntity.ok(watchlist);
        } catch (Exception e) {
            logger.error("Error fetching watchlist: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/watchlist")
    public ResponseEntity<?> addToWatchlist(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
            }

            String username = authentication.getName();
            String symbol = request.get("symbol");

            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("message", "Symbol is required"));
            }

            // Clean and validate the symbol
            String cleanSymbol = symbol.trim().toUpperCase();

            watchlistService.addToWatchlist(username, cleanSymbol);
            return ResponseEntity.ok(Map.of("message", "Stock added to watchlist successfully"));
        } catch (Exception e) {
            logger.error("Error adding stock to watchlist: ", e);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to add stock to watchlist: " + e.getMessage()));
        }
    }

    @DeleteMapping("/watchlist/{symbol}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable String symbol, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
            }

            String username = authentication.getName();
            watchlistService.removeFromWatchlist(username, symbol.toUpperCase());
            return ResponseEntity.ok(Map.of("message", "Stock removed from watchlist successfully"));
        } catch (Exception e) {
            logger.error("Error removing stock from watchlist: ", e);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to remove stock from watchlist: " + e.getMessage()));
        }
    }

    @GetMapping("/data/{symbol}")
    public ResponseEntity<?> getStockData(@PathVariable String symbol) {
        List<StockDataDTO> stockData = stockService.getStockDataHistory(symbol);
        return ResponseEntity.ok(stockData);
    }

    @GetMapping("/market-overview")
    public ResponseEntity<?> getMarketOverview() {
        try {
            Map<String, Object> marketOverview = new HashMap<>();

            // Get real-time market index data from Alpha Vantage
            Map<String, List<StockDataDTO>> marketData = stockService.getMarketOverviewData();
            Map<String, List<Map<String, Object>>> series = new HashMap<>();

            // Convert to the format expected by the frontend
            for (Map.Entry<String, List<StockDataDTO>> entry : marketData.entrySet()) {
                List<Map<String, Object>> seriesData = new ArrayList<>();
                for (StockDataDTO dto : entry.getValue()) {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", dto.getDate().toString());
                    point.put("close", dto.getClose());
                    seriesData.add(point);
                }
                series.put(entry.getKey(), seriesData);
            }

            marketOverview.put("series", series);

            // Add real-time market summary statistics (you can enhance this with real data)
            Map<String, Object> marketSummary = new HashMap<>();
            marketSummary.put("timestamp", new Date());
            marketSummary.put("tradingDay", LocalDate.now());

            // Get latest values for display
            if (!marketData.isEmpty()) {
                for (Map.Entry<String, List<StockDataDTO>> entry : marketData.entrySet()) {
                    List<StockDataDTO> data = entry.getValue();
                    if (!data.isEmpty()) {
                        StockDataDTO latest = data.get(data.size() - 1);
                        StockDataDTO previous = data.size() > 1 ? data.get(data.size() - 2) : latest;

                        double change = latest.getClose() - previous.getClose();
                        double changePercent = change / previous.getClose();

                        Map<String, Object> indexData = new HashMap<>();
                        indexData.put("value", latest.getClose());
                        indexData.put("change", change);
                        indexData.put("changePercent", changePercent);

                        String indexName = switch (entry.getKey()) {
                            case "^GSPC" -> "sp500";
                            case "^DJI" -> "dowJones";
                            case "^IXIC" -> "nasdaq";
                            default -> entry.getKey().toLowerCase();
                        };

                        marketOverview.put(indexName, indexData);
                    }
                }
            }

            marketOverview.put("summary", marketSummary);
            marketOverview.put("lastUpdated", new Date().toString());

            return ResponseEntity.ok(marketOverview);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching market overview: " + e.getMessage());
        }
    }

    /**
     * Helper method to create mock stock data for charts when real data isn't
     * available
     */
    private List<StockDataDTO> createMockStockData(String symbol, int days) {
        List<StockDataDTO> data = new ArrayList<>();
        Random random = new Random();
        LocalDate today = LocalDate.now();

        // Base price depends on the index
        double basePrice = 0;
        if (symbol.equals("^GSPC")) {
            basePrice = 4800.25; // S&P 500
        } else if (symbol.equals("^DJI")) {
            basePrice = 38750.35; // Dow Jones
        } else if (symbol.equals("^IXIC")) {
            basePrice = 16500.50; // NASDAQ
        } else {
            basePrice = 100.0;
        }

        double currentPrice = basePrice;
        double volatility = basePrice * 0.01; // 1% volatility

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // Add some random walk to the price
            double change = (random.nextDouble() - 0.5) * volatility;
            currentPrice += change;

            // Ensure price doesn't go too far from base
            if (Math.abs(currentPrice - basePrice) > basePrice * 0.1) {
                currentPrice = basePrice + (basePrice * 0.1 * (currentPrice > basePrice ? 1 : -1));
            }

            StockDataDTO dto = new StockDataDTO();
            dto.setTicker(symbol);
            dto.setDate(date);
            dto.setClosePrice(BigDecimal.valueOf(currentPrice));
            dto.setVolume((long) (random.nextInt(10000000) + 5000000));
            data.add(dto);
        }

        return data;
    }

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularStocks() {
        try {
            List<StockDataDTO> popularStocks = stockService.getPopularStocks();
            return ResponseEntity.ok(popularStocks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching popular stocks: " + e.getMessage());
        }
    }

    @GetMapping("/historical/{symbol}")
    public ResponseEntity<?> getHistoricalData(@PathVariable String symbol,
            @RequestParam(required = false) String period) {
        try {
            List<StockDataDTO> historicalData = stockService.getStockDataHistory(symbol);

            // Filter based on period if specified
            if (period != null && !historicalData.isEmpty()) {
                LocalDate endDate = LocalDate.now();
                LocalDate startDate;

                switch (period) {
                    case "1m":
                        startDate = endDate.minusMonths(1);
                        break;
                    case "3m":
                        startDate = endDate.minusMonths(3);
                        break;
                    case "6m":
                        startDate = endDate.minusMonths(6);
                        break;
                    case "1y":
                        startDate = endDate.minusYears(1);
                        break;
                    case "5y":
                        startDate = endDate.minusYears(5);
                        break;
                    default:
                        startDate = endDate.minusMonths(1); // Default to 1 month
                }

                final LocalDate filterStartDate = startDate;
                historicalData = historicalData.stream()
                        .filter(data -> !data.getDate().isBefore(filterStartDate))
                        .toList();
            }

            return ResponseEntity.ok(historicalData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching historical data: " + e.getMessage());
        }
    }

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<?> getStockQuote(@PathVariable String symbol) {
        try {
            // Create a simple mock quote for now
            Map<String, Object> quote = new HashMap<>();
            quote.put("symbol", symbol);
            quote.put("price", 100.0 + new Random().nextDouble() * 100);
            quote.put("change", (new Random().nextDouble() - 0.5) * 10);
            quote.put("changePercent", (new Random().nextDouble() - 0.5) * 0.1);
            quote.put("volume", 1000000 + new Random().nextInt(5000000));
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching stock quote: " + e.getMessage());
        }
    }
}
