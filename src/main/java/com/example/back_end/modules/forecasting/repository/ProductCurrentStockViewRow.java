package com.example.back_end.modules.forecasting.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductCurrentStockViewRow {

    private Long productId;
    private String sku;
    private String name;
    private String brand;

    private BigDecimal storeQty;
    private BigDecimal warehouseQty;
    private BigDecimal totalQty;

    private LocalDateTime lastUpdatedAt;
}
