package com.example.back_end.modules.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCandidateDTO {
    private Long productId;
    private String sku;
    private String name;
    private String categoryName;
    private String type;
    private BigDecimal currentPrice;
    private BigDecimal totalQty;
}
