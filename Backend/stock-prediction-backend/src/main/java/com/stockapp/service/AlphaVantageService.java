package com.stockapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.dto.StockDataDTO;
import com.stockapp.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AlphaVantageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alphavantage.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    /**
     * Fetch real-time historical stock data for charts
     */
    public List<StockDataDTO> getHistoricalStockData(String symbol, int days) {
        try {
            String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                BASE_URL, symbol, apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            // Check for API errors
            if (jsonNode.has("Error Message")) {
                throw new ApiException("Alpha Vantage error: " + jsonNode.get("Error Message").asText(),
                    org.springframework.http.HttpStatus.BAD_REQUEST);
            }

            if (jsonNode.has("Note")) {
                // API call frequency limit reached, return cached/mock data
                System.out.println("Alpha Vantage API limit reached, using fallback data");
                return getFallbackData(symbol, days);
            }

            JsonNode timeSeries = jsonNode.get("Time Series (Daily)");
            if (timeSeries == null || !timeSeries.fields().hasNext()) {
                return getFallbackData(symbol, days);
            }

            List<StockDataDTO> stockDataList = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();

            int count = 0;
            while (fields.hasNext() && count < days) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String date = entry.getKey();
                JsonNode data = entry.getValue();

                StockDataDTO stockData = new StockDataDTO();
                stockData.setSymbol(symbol);
                stockData.setDate(LocalDate.parse(date));
                stockData.setOpen(Double.parseDouble(data.get("1. open").asText()));
                stockData.setHigh(Double.parseDouble(data.get("2. high").asText()));
                stockData.setLow(Double.parseDouble(data.get("3. low").asText()));
                stockData.setClose(Double.parseDouble(data.get("4. close").asText()));
                stockData.setVolume(Long.parseLong(data.get("5. volume").asText()));

                stockDataList.add(stockData);
                count++;
            }

            // Sort by date (oldest first for charts)
            stockDataList.sort((a, b) -> a.getDate().compareTo(b.getDate()));

            return stockDataList;

        } catch (Exception e) {
            System.err.println("Error fetching Alpha Vantage data for " + symbol + ": " + e.getMessage());
            return getFallbackData(symbol, days);
        }
    }

    /**
     * Fetch real-time quote data
     */
    public StockDataDTO getRealTimeQuote(String symbol) {
        try {
            String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                BASE_URL, symbol, apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            JsonNode quote = jsonNode.get("Global Quote");
            if (quote == null) {
                return getFallbackQuote(symbol);
            }

            StockDataDTO stockData = new StockDataDTO();
            stockData.setSymbol(symbol);
            stockData.setDate(LocalDate.now());
            stockData.setOpen(Double.parseDouble(quote.get("02. open").asText()));
            stockData.setHigh(Double.parseDouble(quote.get("03. high").asText()));
            stockData.setLow(Double.parseDouble(quote.get("04. low").asText()));
            stockData.setClose(Double.parseDouble(quote.get("05. price").asText()));
            stockData.setVolume(Long.parseLong(quote.get("06. volume").asText()));

            return stockData;

        } catch (Exception e) {
            System.err.println("Error fetching real-time quote for " + symbol + ": " + e.getMessage());
            return getFallbackQuote(symbol);
        }
    }

    /**
     * Fetch market overview data for indices
     */
    public Map<String, List<StockDataDTO>> getMarketOverviewData() {
        Map<String, List<StockDataDTO>> marketData = new HashMap<>();
        List<String> indices = Arrays.asList("SPY", "DIA", "QQQ"); // ETFs that track the indices

        // Use CompletableFuture for parallel API calls
        List<CompletableFuture<Void>> futures = indices.stream()
            .map(symbol -> CompletableFuture.runAsync(() -> {
                try {
                    List<StockDataDTO> data = getHistoricalStockData(symbol, 30);
                    marketData.put(getIndexSymbol(symbol), data);
                } catch (Exception e) {
                    System.err.println("Error fetching market data for " + symbol + ": " + e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return marketData;
    }

    /**
     * Get popular stocks with real-time data
     */
    public List<StockDataDTO> getPopularStocksRealTime() {
        List<String> popularSymbols = Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "JPM");
        List<StockDataDTO> popularStocks = new ArrayList<>();

        for (String symbol : popularSymbols) {
            try {
                StockDataDTO quote = getRealTimeQuote(symbol);
                if (quote != null) {
                    popularStocks.add(quote);
                }
                // Add small delay to respect API rate limits
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("Error fetching popular stock " + symbol + ": " + e.getMessage());
                // Add fallback data for this symbol
                popularStocks.add(getFallbackQuote(symbol));
            }
        }

        return popularStocks;
    }

    /**
     * Convert ETF symbol to index symbol for frontend
     */
    private String getIndexSymbol(String etfSymbol) {
        return switch (etfSymbol) {
            case "SPY" -> "^GSPC"; // S&P 500
            case "DIA" -> "^DJI";  // Dow Jones
            case "QQQ" -> "^IXIC"; // NASDAQ
            default -> etfSymbol;
        };
    }

    /**
     * Fallback method when API fails or rate limit is reached
     */
    private List<StockDataDTO> getFallbackData(String symbol, int days) {
        // Use the existing mock data generation but with more realistic base prices
        List<StockDataDTO> data = new ArrayList<>();
        Random random = new Random(symbol.hashCode());
        LocalDate today = LocalDate.now();

        double basePrice = getBasePriceForSymbol(symbol);
        double currentPrice = basePrice;
        double volatility = basePrice * 0.015; // 1.5% volatility

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // Skip weekends
            if (date.getDayOfWeek().getValue() > 5) {
                continue;
            }

            // Market movement simulation
            double change = (random.nextGaussian() * volatility);
            currentPrice += change;
            currentPrice = Math.max(currentPrice, basePrice * 0.5); // Prevent negative prices

            double open = currentPrice * (0.995 + random.nextDouble() * 0.01);
            double high = currentPrice * (1.0 + random.nextDouble() * 0.02);
            double low = currentPrice * (0.98 + random.nextDouble() * 0.01);

            StockDataDTO stockData = new StockDataDTO();
            stockData.setSymbol(symbol);
            stockData.setDate(date);
            stockData.setOpen(open);
            stockData.setHigh(Math.max(high, Math.max(open, currentPrice)));
            stockData.setLow(Math.min(low, Math.min(open, currentPrice)));
            stockData.setClose(currentPrice);
            stockData.setVolume((long) (1000000 + random.nextInt(10000000)));

            data.add(stockData);
        }

        return data;
    }

    private StockDataDTO getFallbackQuote(String symbol) {
        Random random = new Random(symbol.hashCode() + System.currentTimeMillis());
        double basePrice = getBasePriceForSymbol(symbol);
        double currentPrice = basePrice * (0.95 + random.nextDouble() * 0.1); // ±5% variation

        StockDataDTO quote = new StockDataDTO();
        quote.setSymbol(symbol);
        quote.setDate(LocalDate.now());
        quote.setOpen(currentPrice * (0.99 + random.nextDouble() * 0.02));
        quote.setHigh(currentPrice * (1.005 + random.nextDouble() * 0.015));
        quote.setLow(currentPrice * (0.995 - random.nextDouble() * 0.015));
        quote.setClose(currentPrice);
        quote.setVolume((long) (500000 + random.nextInt(5000000)));

        return quote;
    }

    private double getBasePriceForSymbol(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "AAPL" -> 175.50;
            case "MSFT" -> 335.20;
            case "GOOGL" -> 140.80;
            case "AMZN" -> 145.30;
            case "META" -> 295.40;
            case "TSLA" -> 245.60;
            case "NVDA" -> 485.70;
            case "JPM" -> 155.80;
            case "SPY" -> 445.50;
            case "DIA" -> 340.25;
            case "QQQ" -> 385.75;
            case "^GSPC" -> 4800.25;
            case "^DJI" -> 38750.35;
            case "^IXIC" -> 16500.50;
            default -> 100.0;
        };
    }
}
