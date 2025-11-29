package com.example.back_end.modules.dashboard.inventorydashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RecentInventoryMovementDTO {

    private LocalDate date;
    private String productName;
    private String categoryName;
    private String locationType;
    private String refType;
    private BigDecimal quantityChange;
}