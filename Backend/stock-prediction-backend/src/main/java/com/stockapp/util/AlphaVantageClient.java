package com.stockapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.exception.ApiException;
import com.stockapp.model.StockData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.time.LocalDate;

@Component
public class AlphaVantageClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alphavantage.api.key}")
    private String apiKey;

    public StockData fetchStockData(String ticker) {
        try {
            String url = String.format("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s", ticker, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ApiException("Alpha Vantage API request failed with status: " + response.statusCode(), org.springframework.http.HttpStatus.BAD_GATEWAY);
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            if (jsonNode.has("Error Message")) {
                throw new ApiException("Alpha Vantage API error: " + jsonNode.get("Error Message").asText(), org.springframework.http.HttpStatus.BAD_REQUEST);
            }

            JsonNode timeSeries = jsonNode.get("Time Series (Daily)");
            if (timeSeries == null) {
                throw new ApiException("No time series data found for ticker: " + ticker, org.springframework.http.HttpStatus.NOT_FOUND);
            }

            String latestDate = timeSeries.fieldNames().next();
            JsonNode latestData = timeSeries.get(latestDate);

            StockData stockData = new StockData();
            stockData.setTicker(ticker);
            stockData.setDate(Date.valueOf(LocalDate.parse(latestDate)));
            stockData.setClosePrice(Float.parseFloat(latestData.get("4. close").asText()));
            stockData.setVolume(Long.parseLong(latestData.get("5. volume").asText()));
            // Additional fields (rsi, macd, etc.) would require technical indicator API calls

            return stockData;

        } catch (Exception e) {
            throw new ApiException("Error fetching stock data from Alpha Vantage: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}