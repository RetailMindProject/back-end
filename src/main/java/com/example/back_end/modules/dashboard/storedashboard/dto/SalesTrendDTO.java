package com.example.back_end.modules.dashboard.storedashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesTrendDTO {
    private String day;           // e.g., "Mon", "2025-10-21"
    private BigDecimal revenue;
    private Long orders;
}

