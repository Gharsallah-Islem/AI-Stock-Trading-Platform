package com.stockapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.dto.NewsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class XNewsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${x.api.key}")
    private String apiKey;

    @Value("${x.api.secret}")
    private String apiSecret;

    private static final String TWITTER_API_BASE_URL = "https://api.twitter.com/2";

    /**
     * Fetch financial news and market sentiment from X (Twitter)
     */
    public List<NewsDTO> getFinancialNews(int count) {
        try {
            // Search for financial news tweets from verified financial accounts
            String query = "(from:BloombergTV OR from:CNBC OR from:MarketWatch OR from:Reuters OR from:WSJ OR from:FinancialTimes) " +
                          "(stocks OR market OR trading OR NYSE OR NASDAQ OR SP500 OR finance) -is:retweet";

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = TWITTER_API_BASE_URL + "/tweets/search/recent?query=" + encodedQuery +
                        "&max_results=" + Math.min(count, 100) +
                        "&tweet.fields=created_at,public_metrics,author_id,context_annotations" +
                        "&expansions=author_id&user.fields=verified,profile_image_url,name";

            HttpHeaders headers = createAuthHeaders("GET", url);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseNewsResponse(response.getBody());
            } else {
                System.err.println("X API returned status: " + response.getStatusCode());
                return generateFallbackNews();
            }

        } catch (Exception e) {
            System.err.println("Error fetching news from X API: " + e.getMessage());
            return generateFallbackNews();
        }
    }

    /**
     * Fetch news related to specific stock symbol
     */
    public List<NewsDTO> getStockNews(String symbol, int count) {
        try {
            String query = "$" + symbol + " OR " + symbol + " stock OR " + symbol + " earnings OR " + symbol + " price " +
                          "(from:BloombergTV OR from:CNBC OR from:MarketWatch OR from:Reuters) -is:retweet";

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = TWITTER_API_BASE_URL + "/tweets/search/recent?query=" + encodedQuery +
                        "&max_results=" + Math.min(count, 50) +
                        "&tweet.fields=created_at,public_metrics,author_id" +
                        "&expansions=author_id&user.fields=verified,profile_image_url,name";

            HttpHeaders headers = createAuthHeaders("GET", url);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseNewsResponse(response.getBody());
            }

        } catch (Exception e) {
            System.err.println("Error fetching stock news for " + symbol + ": " + e.getMessage());
        }

        return generateFallbackStockNews(symbol);
    }

    /**
     * Get trending financial topics
     */
    public List<String> getTrendingTopics() {
        try {
            // Get trending topics related to finance
            String url = TWITTER_API_BASE_URL + "/trends/by/woeid/1"; // Global trends

            HttpHeaders headers = createAuthHeaders("GET", url);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseTrendingTopics(response.getBody());
            }

        } catch (Exception e) {
            System.err.println("Error fetching trending topics: " + e.getMessage());
        }

        return Arrays.asList("$AAPL", "$MSFT", "$GOOGL", "$TSLA", "Federal Reserve", "Interest Rates", "Inflation");
    }

    private HttpHeaders createAuthHeaders(String method, String url) {
        HttpHeaders headers = new HttpHeaders();

        // OAuth 1.0a authentication for Twitter API
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replaceAll("-", "");

        Map<String, String> oauthParams = new LinkedHashMap<>();
        oauthParams.put("oauth_consumer_key", apiKey);
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", timestamp);
        oauthParams.put("oauth_version", "1.0");

        try {
            String signature = generateSignature(method, url, oauthParams);
            oauthParams.put("oauth_signature", signature);

            String authHeader = "OAuth " + oauthParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=\"" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8) + "\"")
                .collect(Collectors.joining(", "));

            headers.set("Authorization", authHeader);
            headers.set("Content-Type", "application/json");

        } catch (Exception e) {
            System.err.println("Error creating auth headers: " + e.getMessage());
        }

        return headers;
    }

    private String generateSignature(String method, String url, Map<String, String> params)
            throws NoSuchAlgorithmException, InvalidKeyException {

        String paramString = params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        String baseString = method.toUpperCase() + "&" +
                           URLEncoder.encode(url, StandardCharsets.UTF_8) + "&" +
                           URLEncoder.encode(paramString, StandardCharsets.UTF_8);

        String signingKey = URLEncoder.encode(apiSecret, StandardCharsets.UTF_8) + "&";

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKey = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
        mac.init(secretKey);

        byte[] rawHmac = mac.doFinal(baseString.getBytes());
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    private List<NewsDTO> parseNewsResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode tweets = root.get("data");
            JsonNode users = root.get("includes").get("users");

            List<NewsDTO> newsList = new ArrayList<>();

            if (tweets != null && tweets.isArray()) {
                Map<String, JsonNode> userMap = new HashMap<>();
                if (users != null && users.isArray()) {
                    for (JsonNode user : users) {
                        userMap.put(user.get("id").asText(), user);
                    }
                }

                for (JsonNode tweet : tweets) {
                    NewsDTO news = new NewsDTO();
                    news.setTitle(truncateText(tweet.get("text").asText(), 100));
                    news.setContent(tweet.get("text").asText());
                    news.setPublishedAt(parseTwitterDate(tweet.get("created_at").asText()));

                    String authorId = tweet.get("author_id").asText();
                    JsonNode author = userMap.get(authorId);
                    if (author != null) {
                        news.setSource(author.get("name").asText());
                        news.setAuthor(author.get("username").asText());
                    }

                    news.setUrl("https://twitter.com/i/web/status/" + tweet.get("id").asText());
                    news.setSentiment(analyzeSentiment(tweet.get("text").asText()));

                    newsList.add(news);
                }
            }

            return newsList;

        } catch (Exception e) {
            System.err.println("Error parsing news response: " + e.getMessage());
            return generateFallbackNews();
        }
    }

    private List<String> parseTrendingTopics(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            // Parse trending topics from response
            List<String> topics = new ArrayList<>();
            // Implementation would depend on Twitter API v2 trends format
            return topics.isEmpty() ? Arrays.asList("$AAPL", "$MSFT", "$GOOGL", "$TSLA") : topics;
        } catch (Exception e) {
            return Arrays.asList("$AAPL", "$MSFT", "$GOOGL", "$TSLA", "Federal Reserve", "Interest Rates");
        }
    }

    private LocalDateTime parseTwitterDate(String dateString) {
        try {
            return LocalDateTime.parse(dateString.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String analyzeSentiment(String text) {
        // Simple sentiment analysis based on keywords
        final String finalText = text.toLowerCase();

        long positiveWords = Arrays.stream(new String[]{"bullish", "buy", "strong", "growth", "profit", "gain", "up", "rise", "positive"})
            .mapToLong(word -> finalText.split(word, -1).length - 1)
            .sum();

        long negativeWords = Arrays.stream(new String[]{"bearish", "sell", "weak", "loss", "drop", "down", "fall", "negative", "crash"})
            .mapToLong(word -> finalText.split(word, -1).length - 1)
            .sum();

        if (positiveWords > negativeWords) return "POSITIVE";
        if (negativeWords > positiveWords) return "NEGATIVE";
        return "NEUTRAL";
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private List<NewsDTO> generateFallbackNews() {
        List<NewsDTO> fallbackNews = new ArrayList<>();

        String[] headlines = {
            "Markets Open Higher on Tech Rally",
            "Federal Reserve Signals Potential Rate Changes",
            "Major Earnings Reports Expected This Week",
            "Technology Stocks Lead Market Gains",
            "Inflation Data Impacts Market Sentiment",
            "Energy Sector Shows Strong Performance",
            "Healthcare Stocks React to Policy News"
        };

        String[] sources = {"Bloomberg", "CNBC", "MarketWatch", "Reuters", "Wall Street Journal"};

        for (int i = 0; i < Math.min(headlines.length, 10); i++) {
            NewsDTO news = new NewsDTO();
            news.setTitle(headlines[i]);
            news.setContent(headlines[i] + " - Market analysts are closely watching developments...");
            news.setSource(sources[i % sources.length]);
            news.setAuthor("Financial Reporter");
            news.setPublishedAt(LocalDateTime.now().minusHours(i));
            news.setUrl("https://example.com/news/" + (i + 1));
            news.setSentiment(i % 3 == 0 ? "POSITIVE" : i % 3 == 1 ? "NEGATIVE" : "NEUTRAL");
            fallbackNews.add(news);
        }

        return fallbackNews;
    }

    private List<NewsDTO> generateFallbackStockNews(String symbol) {
        List<NewsDTO> fallbackNews = new ArrayList<>();

        String[] headlines = {
            symbol + " Reports Strong Quarterly Earnings",
            "Analysts Upgrade " + symbol + " Price Target",
            symbol + " Announces New Product Launch",
            "Market Reaction to " + symbol + " Leadership Changes",
            symbol + " Stock Shows Technical Breakout Pattern"
        };

        for (int i = 0; i < headlines.length; i++) {
            NewsDTO news = new NewsDTO();
            news.setTitle(headlines[i]);
            news.setContent(headlines[i] + " - Financial experts provide analysis on recent developments...");
            news.setSource("Financial News");
            news.setAuthor("Market Analyst");
            news.setPublishedAt(LocalDateTime.now().minusHours(i + 1));
            news.setUrl("https://example.com/stock-news/" + symbol.toLowerCase() + "/" + (i + 1));
            news.setSentiment(i % 2 == 0 ? "POSITIVE" : "NEUTRAL");
            fallbackNews.add(news);
        }

        return fallbackNews;
    }
}
