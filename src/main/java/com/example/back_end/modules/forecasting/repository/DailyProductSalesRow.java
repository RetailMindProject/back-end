package com.example.back_end.modules.forecasting.repository;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyProductSalesRow {

    private LocalDate salesDate;
    private BigDecimal totalQtySold;
    private Integer promoAnyFlag;
    private BigDecimal avgDiscountPct;
}
