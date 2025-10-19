package com.stockapp.service;

import com.stockapp.dto.StockDataDTO;
import com.stockapp.exception.ResourceNotFoundException;
import com.stockapp.model.User;
import com.stockapp.model.Watchlist;
import com.stockapp.repository.UserRepository;
import com.stockapp.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    public List<StockDataDTO> getUserWatchlist(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        List<Watchlist> watchlist = watchlistRepository.findByUser(user);
        return watchlist.stream()
                .map(w -> {
                    try {
                        return stockService.getStockData(w.getTicker(), java.time.LocalDate.now());
                    } catch (ResourceNotFoundException e) {
                        // If no data exists for today, get the latest available data
                        List<StockDataDTO> history = stockService.getStockDataHistory(w.getTicker());
                        return history.isEmpty() ? null : history.get(0);
                    }
                })
                .filter(Objects::nonNull) // Remove null entries
                .collect(Collectors.toList());
    }

    public void addToWatchlist(String username, String ticker) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        List<Watchlist> existing = watchlistRepository.findByUserAndTicker(user, ticker);
        if (existing.isEmpty()) {
            Watchlist watchlist = new Watchlist();
            watchlist.setUser(user);
            watchlist.setTicker(ticker);
            watchlistRepository.save(watchlist);
        }
    }

    public void removeFromWatchlist(String username, String ticker) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        List<Watchlist> watchlistEntries = watchlistRepository.findByUserAndTicker(user, ticker);
        if (watchlistEntries.isEmpty()) {
            throw new ResourceNotFoundException("Ticker " + ticker + " not found in watchlist for user: " + username);
        }
        watchlistRepository.deleteAll(watchlistEntries);
    }
}