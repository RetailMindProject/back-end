package com.example.back_end.modules.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendingProductDTO {
    private Long productId;
    private String name;
    private String categoryName;
    private BigDecimal score;
}

