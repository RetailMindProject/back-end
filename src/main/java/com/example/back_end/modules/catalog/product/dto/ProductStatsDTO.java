package com.example.back_end.modules.catalog.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatsDTO {
    private Long totalProducts;
    private Long activeProducts;
    private Long inactiveProducts;
    private Long lowStock;
    private Long outOfStock;
    private Long inStock;
    private BigDecimal totalValue;
}
