package com.example.back_end.modules.forecasting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimePointRequestDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ds;          // نفس اسم حقل FastAPI

    private double y;              // نفس الاسم

    @JsonProperty("promo_any_flag")
    private Integer promoAnyFlag;  // FastAPI يتوقع promo_any_flag

    @JsonProperty("avg_discount_pct")
    private Double avgDiscountPct; // FastAPI يتوقع avg_discount_pct
}
