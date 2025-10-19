package com.stockapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsSentimentDTO {
    private Long id;
    private String symbol;
    private String title; // maps headline
    private String content;
    private String url;
    private LocalDateTime publishedAt;
    private String source;
    private double sentiment;
    private double confidence;
    private String[] keywords;
    private String summary;
    private LocalDateTime createdAt;
}