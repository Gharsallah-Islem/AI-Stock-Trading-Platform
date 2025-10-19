package com.stockapp.repository;

import com.stockapp.model.User;
import com.stockapp.model.VirtualPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VirtualPortfolioRepository extends JpaRepository<VirtualPortfolio, Long> {
    List<VirtualPortfolio> findByUser(User user);

    @Query("SELECT p FROM VirtualPortfolio p WHERE p.user = :user AND p.ticker = :ticker")
    List<VirtualPortfolio> findByUserAndTicker(User user, String ticker);
}