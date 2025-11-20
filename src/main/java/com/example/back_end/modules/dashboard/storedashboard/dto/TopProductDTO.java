package com.example.back_end.modules.dashboard.storedashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopProductDTO {
    private Long productId;
    private String name;
    private String sku;
    private BigDecimal sold;
    private BigDecimal revenue;
}

