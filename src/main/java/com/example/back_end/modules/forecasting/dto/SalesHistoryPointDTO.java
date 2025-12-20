package com.example.back_end.modules.forecasting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SalesHistoryPointDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ds;

    private double y;
}
