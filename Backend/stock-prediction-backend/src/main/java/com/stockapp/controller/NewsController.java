package com.stockapp.controller;

import com.stockapp.dto.NewsDTO;
import com.stockapp.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {

    private final NewsService newsService;

    @Autowired
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Get latest financial news from X API
     */
    @GetMapping("/latest")
    public ResponseEntity<List<NewsDTO>> getLatestNews() {
        try {
            List<NewsDTO> news = newsService.getLatestNews();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get news for specific stock symbol
     */
    @GetMapping("/stock/{symbol}")
    public ResponseEntity<List<NewsDTO>> getStockNews(@PathVariable String symbol) {
        try {
            List<NewsDTO> news = newsService.getStockNews(symbol.toUpperCase());
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get trending financial topics
     */
    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrendingTopics() {
        try {
            List<String> topics = newsService.getTrendingTopics();
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get comprehensive news feed
     */
    @GetMapping("/feed")
    public ResponseEntity<List<NewsDTO>> getNewsFeed(
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<NewsDTO> news = symbol != null ?
                newsService.getComprehensiveNewsFeed(symbol.toUpperCase(), limit) :
                newsService.getLatestNews();
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get market sentiment overview
     */
    @GetMapping("/sentiment/overview")
    public ResponseEntity<Map<String, Object>> getMarketSentimentOverview() {
        try {
            NewsService.MarketSentimentOverview overview = newsService.getMarketSentimentOverview();

            Map<String, Object> response = new HashMap<>();
            response.put("positive", overview.getPositiveCount());
            response.put("negative", overview.getNegativeCount());
            response.put("neutral", overview.getNeutralCount());
            response.put("sentimentScore", overview.getSentimentScore());
            response.put("totalArticles", overview.getTotalArticles());
            response.put("overallSentiment", overview.getOverallSentiment());

            // Calculate percentages
            long total = overview.getTotalArticles();
            if (total > 0) {
                response.put("positivePercentage", (double) overview.getPositiveCount() / total * 100);
                response.put("negativePercentage", (double) overview.getNegativeCount() / total * 100);
                response.put("neutralPercentage", (double) overview.getNeutralCount() / total * 100);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search news by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<List<NewsDTO>> searchNews(@RequestParam String query) {
        try {
            // For now, return general financial news
            // Can be enhanced to search specific keywords in the future
            List<NewsDTO> news = newsService.getLatestNews();

            // Filter news that contains the query term
            List<NewsDTO> filteredNews = news.stream()
                .filter(article -> article.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                                 article.getContent().toLowerCase().contains(query.toLowerCase()))
                .toList();

            return ResponseEntity.ok(filteredNews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
