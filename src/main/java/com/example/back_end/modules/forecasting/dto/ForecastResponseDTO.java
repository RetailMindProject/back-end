package com.example.back_end.modules.forecasting.dto;

import lombok.Data;

import java.util.List;

@Data
public class ForecastResponseDTO {

    private Long productId;
    private List<ForecastPointDTO> forecast;
}

