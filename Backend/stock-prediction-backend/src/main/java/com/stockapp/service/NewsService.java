package com.stockapp.service;

import com.stockapp.dto.NewsSentimentDTO;
import com.stockapp.dto.NewsDTO;
import com.stockapp.model.NewsSentiment;
import com.stockapp.repository.NewsSentimentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsSentimentRepository newsSentimentRepository;
    private final XNewsService xNewsService;

    /**
     * Get latest financial news from X (Twitter) API
     */
    public List<NewsDTO> getLatestNews() {
        return xNewsService.getFinancialNews(20);
    }

    /**
     * Get news specific to a stock symbol
     */
    public List<NewsDTO> getStockNews(String symbol) {
        return xNewsService.getStockNews(symbol, 15);
    }

    /**
     * Get trending financial topics
     */
    public List<String> getTrendingTopics() {
        return xNewsService.getTrendingTopics();
    }

    /**
     * Get comprehensive news feed with both real-time and sentiment analysis
     */
    public List<NewsDTO> getComprehensiveNewsFeed(String symbol, int limit) {
        List<NewsDTO> realTimeNews = xNewsService.getStockNews(symbol, limit);

        // If real-time news is limited, supplement with general financial news
        if (realTimeNews.size() < limit) {
            List<NewsDTO> generalNews = xNewsService.getFinancialNews(limit - realTimeNews.size());
            realTimeNews.addAll(generalNews);
        }

        return realTimeNews.stream()
            .sorted((a, b) -> b.getPublishedAt().compareTo(a.getPublishedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get market sentiment overview
     */
    public MarketSentimentOverview getMarketSentimentOverview() {
        List<NewsDTO> recentNews = xNewsService.getFinancialNews(50);

        long positiveCount = recentNews.stream()
            .filter(news -> "POSITIVE".equals(news.getSentiment()))
            .count();

        long negativeCount = recentNews.stream()
            .filter(news -> "NEGATIVE".equals(news.getSentiment()))
            .count();

        long neutralCount = recentNews.stream()
            .filter(news -> "NEUTRAL".equals(news.getSentiment()))
            .count();

        double sentimentScore = ((double) positiveCount - negativeCount) / recentNews.size();

        return new MarketSentimentOverview(
            positiveCount,
            negativeCount,
            neutralCount,
            sentimentScore,
            recentNews.size()
        );
    }

    // Existing sentiment analysis methods
    public List<NewsSentimentDTO> getNewsSentiment(String ticker, LocalDate date) {
        List<NewsSentiment> sentiments = newsSentimentRepository.findByTickerAndDate(ticker, Date.valueOf(date));
        return sentiments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<NewsSentimentDTO> getNewsSentimentHistory(String ticker) {
        List<NewsSentiment> sentiments = newsSentimentRepository.findByTickerOrderByDateDesc(ticker);
        return sentiments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private NewsSentimentDTO mapToDTO(NewsSentiment s) {
        NewsSentimentDTO dto = new NewsSentimentDTO();
        dto.setId(s.getId());
        dto.setSymbol(s.getTicker());
        dto.setTitle(s.getHeadline());
        dto.setContent(s.getContent());
        dto.setUrl(s.getUrl());
        dto.setPublishedAt(LocalDateTime.of(s.getDate().toLocalDate(), LocalDateTime.now().toLocalTime()));
        dto.setSource(s.getSource());
        dto.setSentiment(s.getSentiment());
        dto.setConfidence(s.getConfidence() == null ? 0.0 : s.getConfidence());
        dto.setKeywords(s.getKeywordList().toArray(new String[0]));
        dto.setSummary(s.getSummary());
        dto.setCreatedAt(s.getCreatedAt().toLocalDateTime());
        return dto;
    }

    // Inner class for market sentiment overview
    public static class MarketSentimentOverview {
        private final long positiveCount;
        private final long negativeCount;
        private final long neutralCount;
        private final double sentimentScore;
        private final long totalArticles;

        public MarketSentimentOverview(long positiveCount, long negativeCount, long neutralCount,
                                     double sentimentScore, long totalArticles) {
            this.positiveCount = positiveCount;
            this.negativeCount = negativeCount;
            this.neutralCount = neutralCount;
            this.sentimentScore = sentimentScore;
            this.totalArticles = totalArticles;
        }

        // Getters
        public long getPositiveCount() { return positiveCount; }
        public long getNegativeCount() { return negativeCount; }
        public long getNeutralCount() { return neutralCount; }
        public double getSentimentScore() { return sentimentScore; }
        public long getTotalArticles() { return totalArticles; }
        public String getOverallSentiment() {
            if (sentimentScore > 0.1) return "BULLISH";
            if (sentimentScore < -0.1) return "BEARISH";
            return "NEUTRAL";
        }
    }
}
