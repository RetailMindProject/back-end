package com.example.back_end.modules.dashboard.storedashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StoreSummaryDTO {
    private BigDecimal totalSales;       // Last N days total
    private Long totalOrders;            // Last N days count
    private BigDecimal recentDailyAmount; // Most recent day amount
    private String recentDate;           // Most recent day date
    private String mostPopularProduct;   // Product name
    private BigDecimal mostPopularSold;  // Quantity
    private BigDecimal mostPopularRevenue; // Revenue
}

