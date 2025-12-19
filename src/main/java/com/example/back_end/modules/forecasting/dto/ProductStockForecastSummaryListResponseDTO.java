package com.example.back_end.modules.forecasting.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductStockForecastSummaryListResponseDTO {

    private long total;   // إجمالي السجلات المطابقة للفلاتر (بدون limit/offset)
    private int page;     // رقم الصفحة الحالية
    private int size;     // حجم الصفحة
    private List<ProductStockForecastSummaryListItemDTO> items;
}
