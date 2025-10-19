package com.stockapp.controller;

import com.stockapp.dto.AnomalyDTO;
import com.stockapp.service.AnomalyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyService anomalyService;

    @Autowired
    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getAnomalies(@PathVariable String symbol) {
        List<AnomalyDTO> anomalies = anomalyService.getAnomalyHistory(symbol);
        return ResponseEntity.ok(anomalies);
    }
}