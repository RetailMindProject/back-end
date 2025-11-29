package com.example.back_end.modules.dashboard.inventorydashboard.dto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventorySummaryDTO {

    private BigDecimal totalInThisWeek;

    private BigDecimal totalOutThisWeek;

    private Long movementCount;

    private String mostMovedProduct;
}
