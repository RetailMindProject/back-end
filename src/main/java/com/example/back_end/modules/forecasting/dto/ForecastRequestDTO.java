package com.example.back_end.modules.forecasting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ForecastRequestDTO {

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("horizon_days")
    private int horizonDays;

    private List<String> regressors; // لو في FastAPI اسمها regressors خليه زي ما هو

    private List<TimePointRequestDTO> series; // نفس الاسم
}
