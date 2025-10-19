package com.stockapp.service;

import com.stockapp.dto.PortfolioDTO;
import com.stockapp.dto.PortfolioDTO.PortfolioEntryDTO;
import com.stockapp.exception.ResourceNotFoundException;
import com.stockapp.model.User;
import com.stockapp.model.VirtualPortfolio;
import com.stockapp.repository.UserRepository;
import com.stockapp.repository.VirtualPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final VirtualPortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    public PortfolioDTO getPortfolio(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        List<VirtualPortfolio> portfolioEntries = portfolioRepository.findByUser(user);
        return mapToDTO(user, portfolioEntries);
    }

    public PortfolioDTO addPortfolioEntry(String username, PortfolioEntryDTO entryDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        VirtualPortfolio portfolio = new VirtualPortfolio();
        portfolio.setUser(user);
        portfolio.setTicker(entryDTO.getSymbol());
        portfolio.setShares((int) entryDTO.getQuantity());
        portfolio.setBuyPrice((float) entryDTO.getAveragePrice());
        portfolio.setCurrentValue((float) entryDTO.getCurrentPrice());
        portfolio.setTradeDate(java.sql.Date.valueOf(LocalDate.now()));
        portfolio.setBalance((float) (entryDTO.getCurrentPrice() * entryDTO.getQuantity()));
        portfolio.setTradeType(VirtualPortfolio.TradeType.Buy);
        portfolio.setScore(0);
        portfolioRepository.save(portfolio);
        List<VirtualPortfolio> portfolioEntries = portfolioRepository.findByUser(user);
        return mapToDTO(user, portfolioEntries);
    }

    public void removeFromPortfolio(String username, String ticker) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        List<VirtualPortfolio> portfolioEntries = portfolioRepository.findByUserAndTicker(user, ticker);
        if (portfolioEntries.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No portfolio entries found for ticker: " + ticker + " for user: " + username);
        }
        portfolioRepository.deleteAll(portfolioEntries);
    }

    private PortfolioDTO mapToDTO(User user, List<VirtualPortfolio> portfolioEntries) {
        PortfolioDTO dto = new PortfolioDTO();
        dto.setUsername(user.getUsername());
        dto.setHoldings(portfolioEntries.stream().map(this::mapToEntryDTO).collect(Collectors.toList()));
        dto.setTotalValue(portfolioEntries.stream()
                .mapToDouble(p -> p.getCurrentValue() * p.getShares())
                .sum());
        return dto;
    }

    private PortfolioEntryDTO mapToEntryDTO(VirtualPortfolio portfolio) {
        PortfolioEntryDTO entryDTO = new PortfolioEntryDTO();
        entryDTO.setSymbol(portfolio.getTicker());
        entryDTO.setQuantity(portfolio.getShares());
        entryDTO.setAveragePrice(portfolio.getBuyPrice());
        // Use buyPrice as currentPrice if currentValue is null
        entryDTO.setCurrentPrice(
                portfolio.getCurrentValue() != null ? portfolio.getCurrentValue() : portfolio.getBuyPrice());
        return entryDTO;
    }
}