package com.example.back_end.modules.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDTO {
    private BigDecimal totalSales;
    private Long totalOrders;
    private BigDecimal averageOrderValue;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalTaxAmount;
    private String mostPopularProduct;
    private BigDecimal mostPopularSold;
    private BigDecimal mostPopularRevenue;
    private LocalDate fromDate;
    private LocalDate toDate;
}
