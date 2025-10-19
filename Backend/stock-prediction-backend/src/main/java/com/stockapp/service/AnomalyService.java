package com.stockapp.service;

import com.stockapp.dto.AnomalyDTO;
import com.stockapp.model.Anomaly;
import com.stockapp.repository.AnomalyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;

    public List<AnomalyDTO> getAnomalies(String ticker, LocalDate date) {
        List<Anomaly> anomalies = anomalyRepository.findByTickerAndDate(ticker, Date.valueOf(date));
        return anomalies.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<AnomalyDTO> getAnomalyHistory(String ticker) {
        List<Anomaly> anomalies = anomalyRepository.findByTickerOrderByDateDesc(ticker);
        return anomalies.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private AnomalyDTO mapToDTO(Anomaly anomaly) {
        AnomalyDTO dto = new AnomalyDTO();
        dto.setSymbol(anomaly.getTicker());
        dto.setAnomalyType(anomaly.getAnomalyType().name());
        dto.setValue(anomaly.getSeverity() != null ? anomaly.getSeverity() : 0);
        dto.setTimestamp(LocalDateTime.of(anomaly.getDate().toLocalDate(), LocalDateTime.now().toLocalTime()));
        dto.setDescription(anomaly.getDescription());
        return dto;
    }
}