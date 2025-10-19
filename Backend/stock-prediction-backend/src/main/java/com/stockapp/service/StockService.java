package com.stockapp.service;

import com.stockapp.dto.StockDataDTO;
import com.stockapp.exception.ResourceNotFoundException;
import com.stockapp.model.StockData;
import com.stockapp.repository.StockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockDataRepository stockDataRepository;
    private final AlphaVantageService alphaVantageService;

    public StockDataDTO getStockData(String ticker, LocalDate date) {
        StockData stockData = stockDataRepository.findByTickerAndDate(ticker, Date.valueOf(date))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock data not found for ticker: " + ticker + " on date: " + date));
        return mapToDTO(stockData);
    }

    public List<StockDataDTO> getStockDataHistory(String ticker) {
        // First try to get real-time data from Alpha Vantage
        try {
            List<StockDataDTO> realTimeData = alphaVantageService.getHistoricalStockData(ticker, 90);
            if (realTimeData != null && !realTimeData.isEmpty()) {
                return realTimeData;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch real-time data for " + ticker + ", falling back to database");
        }

        // Fallback to database data
        List<StockData> stockDataList = stockDataRepository.findByTickerOrderByDateDesc(ticker);

        // If no data in database either, generate mock data as last resort
        if (stockDataList.isEmpty()) {
            return generateMockStockData(ticker, 90);
        }

        return stockDataList.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<StockDataDTO> getPopularStocks() {
        // Get real-time popular stocks data from Alpha Vantage
        try {
            List<StockDataDTO> realTimePopular = alphaVantageService.getPopularStocksRealTime();
            if (realTimePopular != null && !realTimePopular.isEmpty()) {
                return realTimePopular;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch real-time popular stocks, using fallback");
        }

        // Fallback logic (existing mock data generation)
        List<String> popularSymbols = List.of("AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA", "JPM");
        List<StockDataDTO> popularStocks = new ArrayList<>();

        for (String symbol : popularSymbols) {
            List<StockDataDTO> stockData = getStockDataHistory(symbol);
            if (!stockData.isEmpty()) {
                StockDataDTO latest = stockData.get(0);
                latest.setVolume((long) (5000000 + new Random().nextInt(10000000)));
                popularStocks.add(latest);
            }
        }

        return popularStocks;
    }

    public Map<String, List<StockDataDTO>> getMarketOverviewData() {
        // Get real-time market data from Alpha Vantage
        try {
            return alphaVantageService.getMarketOverviewData();
        } catch (Exception e) {
            System.err.println("Failed to fetch real-time market data, using fallback");
            // Return fallback market data
            Map<String, List<StockDataDTO>> fallbackData = new HashMap<>();
            List<String> indices = Arrays.asList("^GSPC", "^DJI", "^IXIC");

            for (String index : indices) {
                fallbackData.put(index, generateMockStockData(index, 30));
            }

            return fallbackData;
        }
    }

    /**
     * Generate mock stock data for testing and demo purposes
     */
    private List<StockDataDTO> generateMockStockData(String ticker, int days) {
        List<StockDataDTO> data = new ArrayList<>();
        Random random = new Random(ticker.hashCode()); // Consistent data for same ticker
        LocalDate today = LocalDate.now();

        // Base price depends on the ticker
        double basePrice = getBasePriceForTicker(ticker);
        double currentPrice = basePrice;
        double volatility = basePrice * 0.02; // 2% volatility

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // Add some random walk to the price with trend
            double trend = (random.nextDouble() - 0.48) * volatility; // Slight upward bias
            double dailyChange = trend + (random.nextGaussian() * volatility * 0.5);

            // Start with the opening price (previous day's close + gap)
            double openPrice = currentPrice + (random.nextGaussian() * volatility * 0.2); // Small gap up/down
            openPrice = Math.max(openPrice, basePrice * 0.1); // Ensure positive

            // Calculate the closing price based on daily change
            double closePrice = openPrice + dailyChange;
            closePrice = Math.max(closePrice, basePrice * 0.1); // Ensure positive

            // Ensure price doesn't go too far from base (within 20%)
            if (Math.abs(closePrice - basePrice) > basePrice * 0.2) {
                closePrice = basePrice + (basePrice * 0.2 * (closePrice > basePrice ? 1 : -1));
            }

            // Calculate high and low based on intraday volatility
            double intraDayVolatility = Math.abs(closePrice - openPrice) * 0.5 + volatility * 0.3;
            double highPrice = Math.max(openPrice, closePrice) + (random.nextDouble() * intraDayVolatility);
            double lowPrice = Math.min(openPrice, closePrice) - (random.nextDouble() * intraDayVolatility);

            // Ensure logical constraints
            highPrice = Math.max(highPrice, Math.max(openPrice, closePrice));
            lowPrice = Math.min(lowPrice, Math.min(openPrice, closePrice));
            lowPrice = Math.max(lowPrice, basePrice * 0.05); // Ensure reasonable low

            currentPrice = closePrice; // Update for next iteration

            StockDataDTO dto = new StockDataDTO();
            dto.setTicker(ticker);
            dto.setDate(date);
            dto.setOpen(openPrice);
            dto.setHigh(highPrice);
            dto.setLow(lowPrice);
            dto.setClose(closePrice);
            dto.setVolume((long) (1000000 + random.nextInt(20000000)));

            // Add technical indicators
            dto.setRsi(BigDecimal.valueOf(30 + random.nextDouble() * 40)); // RSI between 30-70
            dto.setMacd(BigDecimal.valueOf((random.nextDouble() - 0.5) * 2));
            dto.setSma50(BigDecimal.valueOf(closePrice * (0.98 + random.nextDouble() * 0.04)));
            dto.setEma20(BigDecimal.valueOf(closePrice * (0.99 + random.nextDouble() * 0.02)));

            data.add(dto);
        }

        return data;
    }

    private double getBasePriceForTicker(String ticker) {
        return switch (ticker) {
            case "AAPL" -> 175.50;
            case "MSFT" -> 335.20;
            case "GOOGL" -> 140.80;
            case "AMZN" -> 145.30;
            case "META" -> 295.40;
            case "TSLA" -> 245.60;
            case "NVDA" -> 485.70;
            case "JPM" -> 155.80;
            case "^GSPC" -> 4800.25;
            case "^DJI" -> 38750.35;
            case "^IXIC" -> 16500.50;
            default -> 100.0;
        };
    }

    private StockDataDTO mapToDTO(StockData stockData) {
        StockDataDTO dto = new StockDataDTO();
        dto.setSymbol(stockData.getTicker());
        dto.setClose((double) stockData.getClosePrice());
        dto.setVolume(stockData.getVolume());
        dto.setDate(stockData.getDate().toLocalDate());

        // Map optional technical indicators if they exist
        if (stockData.getRsi() != null) {
            dto.setRsi(BigDecimal.valueOf(stockData.getRsi()));
        }
        if (stockData.getMacd() != null) {
            dto.setMacd(BigDecimal.valueOf(stockData.getMacd()));
        }
        if (stockData.getVix() != null) {
            dto.setVix(BigDecimal.valueOf(stockData.getVix()));
        }
        if (stockData.getSma50() != null) {
            dto.setSma50(BigDecimal.valueOf(stockData.getSma50()));
        }
        if (stockData.getEma20() != null) {
            dto.setEma20(BigDecimal.valueOf(stockData.getEma20()));
        }
        if (stockData.getBbUpper() != null) {
            dto.setBbUpper(BigDecimal.valueOf(stockData.getBbUpper()));
        }
        if (stockData.getBbLower() != null) {
            dto.setBbLower(BigDecimal.valueOf(stockData.getBbLower()));
        }
        if (stockData.getAtr() != null) {
            dto.setAtr(BigDecimal.valueOf(stockData.getAtr()));
        }
        if (stockData.getObv() != null) {
            dto.setObv(stockData.getObv());
        }
        if (stockData.getStochastic() != null) {
            dto.setStochastic(BigDecimal.valueOf(stockData.getStochastic()));
        }
        if (stockData.getInterestRate() != null) {
            dto.setInterestRate(BigDecimal.valueOf(stockData.getInterestRate()));
        }

        return dto;
    }
}