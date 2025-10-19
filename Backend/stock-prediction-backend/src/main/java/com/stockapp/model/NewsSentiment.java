package com.stockapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "news_sentiment", indexes = {
        @Index(name = "idx_ticker_date", columnList = "ticker, date")
})
@Data
@NoArgsConstructor
public class NewsSentiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Column(name = "date", nullable = false)
    private Date date;

    @Column(name = "sentiment", nullable = false)
    private float sentiment;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType = EventType.Neutral;

    @Column(name = "headline")
    private String headline;

    @Column(name = "impact_score")
    private Float impactScore;

    @Column(name = "source", length = 100)
    private String source;

    // Additional enriched fields
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "confidence")
    private Float confidence;

    // Comma-separated keywords for simplicity
    @Column(name = "keywords", length = 1000)
    private String keywords;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    public enum EventType {
        Neutral, ProductLaunch, Earnings, Lawsuit, Regulatory, Other
    }

    // Convenience helpers (not persisted)
    public List<String> getKeywordList() {
        return keywords == null || keywords.isBlank() ? List.of()
                : Arrays.stream(keywords.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void setKeywordList(List<String> list) {
        this.keywords = (list == null || list.isEmpty()) ? null : String.join(",", list);
    }
}