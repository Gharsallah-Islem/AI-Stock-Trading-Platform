package com.stockapp.service;

import com.stockapp.dto.CorrelationDTO;
import com.stockapp.exception.ResourceNotFoundException;
import com.stockapp.model.Correlation;
import com.stockapp.repository.CorrelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CorrelationService {

    private final CorrelationRepository correlationRepository;

    public CorrelationDTO getCorrelation(String ticker1, String ticker2, LocalDate date) {
        Correlation correlation = correlationRepository.findByTicker1AndTicker2AndDate(ticker1, ticker2, Date.valueOf(date))
                .orElseThrow(() -> new ResourceNotFoundException("Correlation not found for tickers: " + ticker1 + ", " + ticker2 + " on date: " + date));
        return mapToDTO(correlation);
    }

    public List<CorrelationDTO> getCorrelationHistory(String ticker) {
        List<Correlation> correlations = correlationRepository.findByTickerOrderByDateDesc(ticker);
        return correlations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }


    private CorrelationDTO mapToDTO(Correlation correlation) {
        CorrelationDTO dto = new CorrelationDTO();
        dto.setSymbol(correlation.getTicker1());
        dto.setCorrelatedSymbol(correlation.getTicker2());
        dto.setCorrelationCoefficient(correlation.getCorrelation());
        dto.setTimeframe(correlation.getDate().toString());
        return dto;
    }
}