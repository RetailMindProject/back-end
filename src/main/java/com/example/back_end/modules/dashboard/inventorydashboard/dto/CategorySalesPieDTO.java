package com.example.back_end.modules.dashboard.inventorydashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CategorySalesPieDTO {

    private String categoryName;
    private BigDecimal salesQty;
}
