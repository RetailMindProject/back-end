package com.example.back_end.modules.forecasting.dto;

import lombok.Data;

@Data
public class ProductForecastBatchItemDTO {

    private Long productId;

    private String status;

    private String message;

    private Integer forecastPoints;
}
