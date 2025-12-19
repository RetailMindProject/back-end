package com.example.back_end.modules.forecasting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ForecastPointDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ds;

    private double yhat;

    @JsonProperty("yhat_lower")
    private Double yhatLower;

    @JsonProperty("yhat_upper")
    private Double yhatUpper;
}
