package com.stockapp.repository;

import com.stockapp.model.User;
import com.stockapp.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUser(User user);

    boolean existsByUserAndTicker(User user, String ticker);

    List<Watchlist> findByUserAndTicker(User user, String ticker);
}