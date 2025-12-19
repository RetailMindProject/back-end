package com.example.back_end.modules.forecasting.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchForecastResponseDTO {

    /**
     * عدد المنتجات التي حاولنا تشغيل التنبؤ لها (المؤهَّلة من where clause).
     */
    private int totalProducts;

    /**
     * عدد المنتجات التي نجح لها التنبؤ وتخزين النتائج.
     */
    private int successCount;

    /**
     * عدد المنتجات التي تم تخطيها (لا يوجد بيانات / بيانات غير كافية).
     */
    private int skippedCount;

    /**
     * عدد المنتجات التي حصل فيها خطأ.
     */
    private int errorCount;

    /**
     * تفاصيل لكل منتج على حدة.
     */
    private List<ProductForecastBatchItemDTO> details;
}
