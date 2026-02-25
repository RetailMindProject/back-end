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
public class TimeSeriesDTO {
    private LocalDate date;
    private BigDecimal revenue;
    private Long orders;
    private BigDecimal averageOrderValue;
}
