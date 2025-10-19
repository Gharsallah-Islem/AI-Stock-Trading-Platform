package com.stockapp.controller;

import com.stockapp.dto.CorrelationDTO;
import com.stockapp.service.CorrelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/correlations")
public class CorrelationController {

    private final CorrelationService correlationService;

    @Autowired
    public CorrelationController(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getCorrelations(@PathVariable String symbol) {
        List<CorrelationDTO> correlations = correlationService.getCorrelationHistory(symbol);
        return ResponseEntity.ok(correlations);
    }
}