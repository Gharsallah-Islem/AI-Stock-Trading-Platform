package com.stockapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.exception.ApiException;
import com.stockapp.model.NewsSentiment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Base64;

@Component
public class XApiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${x.api.key}")
    private String apiKey;

    @Value("${x.api.secret}")
    private String apiSecret;

    /**
     * Placeholder bearer token creation.
     * For the official Twitter/X v2 API you must perform a Client Credentials
     * (OAuth2) flow
     * to exchange the apiKey/apiSecret for a bearer token via POST
     * https://api.twitter.com/oauth2/token
     * with grant_type=client_credentials. Here we assume apiKey already represents
     * a bearer token if it
     * starts with 'Bearer '. Otherwise we Base64-encode the pair (temporary
     * fallback).
     */
    private String getBearerToken() {
        if (apiKey == null || apiSecret == null || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ApiException("X API credentials are not configured",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (apiKey.startsWith("Bearer ")) {
            return apiKey; // already a bearer token supplied in config
        }
        String credentials = apiKey + ":" + apiSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials; // Mark as Basic for future real token exchange
    }

    public NewsSentiment fetchNewsSentiment(String ticker) {
        try {
            // Updated to a hypothetical v2 endpoint; adjust based on X API docs
            String query = URLEncoder.encode(ticker + " stock", StandardCharsets.UTF_8);
            String url = "https://api.twitter.com/2/tweets/search/recent?query=" + query
                    + "&max_results=10&tweet.fields=created_at";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getBearerToken())
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ApiException("X API authentication failed (" + response.statusCode() + ")",
                        org.springframework.http.HttpStatus.UNAUTHORIZED);
            } else if (response.statusCode() >= 400) {
                throw new ApiException("X API request failed with status: " + response.statusCode(),
                        org.springframework.http.HttpStatus.BAD_GATEWAY);
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            if (jsonNode.has("errors")) {
                throw new ApiException("X API error: " + jsonNode.get("errors").get(0).get("message").asText(),
                        org.springframework.http.HttpStatus.BAD_REQUEST);
            }

            // Assuming the response contains a list of tweets; parse the first relevant one
            JsonNode data = jsonNode.get("data");
            if (data == null || data.isEmpty()) {
                throw new ApiException("No data found for ticker: " + ticker,
                        org.springframework.http.HttpStatus.NOT_FOUND);
            }

            NewsSentiment sentiment = new NewsSentiment();
            sentiment.setTicker(ticker);
            sentiment.setDate(Date.valueOf(LocalDate.now())); // Use actual date from tweet if available
            // Sentiment score is hypothetical; X API doesn’t provide this directly—consider
            // a separate sentiment service
            sentiment.setSentiment(estimateSentimentFromText(data.get(0).get("text").asText()));
            sentiment.setHeadline(data.get(0).get("text").asText()); // Use tweet text as headline
            sentiment.setSource("X");
            sentiment.setEventType(NewsSentiment.EventType.Other);

            return sentiment;

        } catch (Exception e) {
            throw new ApiException("Error fetching news sentiment from X API: " + e.getMessage(),
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Placeholder method to estimate sentiment (replace with a proper NLP service)
    private float estimateSentimentFromText(String text) {
        // Simple heuristic: count positive/negative words (e.g., "good", "bad")
        // In production, use a library like Stanford NLP or a pre-trained model
        String lowerText = text.toLowerCase();
        int positiveWords = (lowerText.contains("good") ? 1 : 0) + (lowerText.contains("rise") ? 1 : 0);
        int negativeWords = (lowerText.contains("bad") ? 1 : 0) + (lowerText.contains("fall") ? 1 : 0);
        return (float) (positiveWords - negativeWords) / (positiveWords + negativeWords + 1); // Normalized [-1, 1]
    }
}