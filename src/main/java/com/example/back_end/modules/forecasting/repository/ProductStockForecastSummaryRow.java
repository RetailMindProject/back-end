package com.example.back_end.modules.forecasting.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductStockForecastSummaryRow {

    private Long productId;
    private BigDecimal currentStock;
    private BigDecimal avgDailyDemand;
    private LocalDate expectedStockoutDate;
    private BigDecimal recommendedReorderQty;
    private LocalDateTime generatedAt;
}
