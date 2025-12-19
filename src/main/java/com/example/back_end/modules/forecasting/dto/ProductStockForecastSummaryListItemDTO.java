package com.example.back_end.modules.forecasting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductStockForecastSummaryListItemDTO {

    private Long productId;
    private String sku;
    private String name;
    private String brand;

    private BigDecimal currentStock;
    private BigDecimal avgDailyDemand;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedStockoutDate;

    private BigDecimal recommendedReorderQty;
}
