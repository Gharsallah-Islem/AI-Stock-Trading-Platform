package com.stockapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsDTO {
    private String title;
    private String content;
    private String source;
    private String author;
    private String url;
    private LocalDateTime publishedAt;
    private String sentiment; // POSITIVE, NEGATIVE, NEUTRAL
    private String category; // MARKET, EARNINGS, TECH, POLICY, etc.
    private Double relevanceScore;
    private String imageUrl;
    private Integer engagementScore;
}
