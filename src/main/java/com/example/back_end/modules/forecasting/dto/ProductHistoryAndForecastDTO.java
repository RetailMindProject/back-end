package com.example.back_end.modules.forecasting.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductHistoryAndForecastDTO {

    private Long productId;
    private List<SalesHistoryPointDTO> history;
    private List<ForecastPointDTO> forecast;
}
