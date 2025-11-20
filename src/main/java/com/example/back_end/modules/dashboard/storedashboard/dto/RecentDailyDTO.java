package com.example.back_end.modules.dashboard.storedashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RecentDailyDTO {
    private String date;          // "2025-10-21"
    private BigDecimal amount;
    private Long orders;
}

