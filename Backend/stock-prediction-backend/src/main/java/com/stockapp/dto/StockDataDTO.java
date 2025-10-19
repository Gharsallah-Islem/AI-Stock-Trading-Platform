package com.stockapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockDataDTO {

    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z\\^]{1,6}$", message = "Symbol must be 1-6 uppercase letters or include ^ for indices")
    private String symbol;

    // Alternative name for backward compatibility
    public String getTicker() {
        return symbol;
    }

    public void setTicker(String ticker) {
        this.symbol = ticker;
    }

    @NotNull(message = "Date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "Open price is required")
    @DecimalMin(value = "0.01", message = "Open price must be greater than 0")
    private Double open;

    @NotNull(message = "High price is required")
    @DecimalMin(value = "0.01", message = "High price must be greater than 0")
    private Double high;

    @NotNull(message = "Low price is required")
    @DecimalMin(value = "0.01", message = "Low price must be greater than 0")
    private Double low;

    @NotNull(message = "Close price is required")
    @DecimalMin(value = "0.01", message = "Close price must be greater than 0")
    private Double close;

    // Backward compatibility for closePrice
    public BigDecimal getClosePrice() {
        return close != null ? BigDecimal.valueOf(close) : null;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.close = closePrice != null ? closePrice.doubleValue() : null;
    }

    public void setClosePrice(Double closePrice) {
        this.close = closePrice;
    }

    @NotNull(message = "Volume is required")
    @Min(value = 0, message = "Volume cannot be negative")
    private Long volume;

    @DecimalMin(value = "0.0", message = "RSI must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "RSI must be between 0 and 100")
    private BigDecimal rsi;

    @DecimalMin(value = "0.0", message = "MACD cannot be negative")
    private BigDecimal macd;

    @DecimalMin(value = "0.0", message = "SMA 50 cannot be negative")
    private BigDecimal sma50;

    @DecimalMin(value = "0.0", message = "EMA 20 cannot be negative")
    private BigDecimal ema20;

    @DecimalMin(value = "0.0", message = "BB Upper cannot be negative")
    private BigDecimal bbUpper;

    @DecimalMin(value = "0.0", message = "BB Lower cannot be negative")
    private BigDecimal bbLower;

    @DecimalMin(value = "0.0", message = "ATR cannot be negative")
    private BigDecimal atr;

    @DecimalMin(value = "0.0", message = "Stochastic cannot be negative")
    @DecimalMax(value = "100.0", message = "Stochastic must be between 0 and 100")
    private BigDecimal stochastic;

    @DecimalMin(value = "0.0", message = "VIX cannot be negative")
    private BigDecimal vix;

    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    private BigDecimal interestRate;

    @Min(value = 0, message = "OBV cannot be negative")
    private Long obv;
}
