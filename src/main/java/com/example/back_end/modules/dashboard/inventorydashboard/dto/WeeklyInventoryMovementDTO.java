package com.example.back_end.modules.dashboard.inventorydashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class WeeklyInventoryMovementDTO {

    private LocalDate date;
    private BigDecimal totalIn;
    private BigDecimal totalOut;
}
