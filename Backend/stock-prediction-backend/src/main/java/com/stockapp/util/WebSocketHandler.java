package com.stockapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockapp.service.StockService;
import com.stockapp.dto.StockDataDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final StockService stockService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public WebSocketHandler(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String ticker = message.getPayload();
            StockDataDTO stockData = stockService.getStockData(ticker, LocalDate.now());
            String json = objectMapper.writeValueAsString(stockData);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    public void broadcastStockUpdate(String ticker) throws IOException {
        StockDataDTO stockData = stockService.getStockData(ticker, LocalDate.now());
        String json = objectMapper.writeValueAsString(stockData);
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }
}