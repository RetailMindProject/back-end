package com.example.back_end.modules.forecasting.service;

import com.example.back_end.modules.forecasting.dto.ForecastPointDTO;
import com.example.back_end.modules.forecasting.dto.ProductStockForecastSummaryDTO;
import com.example.back_end.modules.forecasting.dto.ProductStockForecastSummaryListItemDTO;
import com.example.back_end.modules.forecasting.dto.ProductStockForecastSummaryListResponseDTO;
import com.example.back_end.modules.forecasting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockForecastSummaryService {

    private final ProductCurrentStockRepository currentStockRepository;
    private final ProductDailyForecastRepository forecastRepository;
    private final ProductStockForecastSummaryRepository summaryRepository;
    private final ProductStockForecastSummaryViewRepository summaryViewRepository;

    /**
     * حساب وتخزين ملخّص المخزون + التنبؤ لمنتج واحد.
     */
    public ProductStockForecastSummaryDTO rebuildSummaryForProduct(Long productId,
                                                                   int horizonDays) {
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(horizonDays - 1);

        // 1) جلب المخزون الحالي
        ProductCurrentStockViewRow stockRow =
                currentStockRepository.findByProductId(productId);

        BigDecimal currentStock = BigDecimal.ZERO;
        if (stockRow != null && stockRow.getTotalQty() != null) {
            currentStock = stockRow.getTotalQty();
        }

        // 2) جلب التنبؤ من جدول product_daily_forecast
        List<ForecastPointDTO> forecastPoints =
                forecastRepository.findForecastForProduct(productId, today, toDate);

        if (forecastPoints == null || forecastPoints.isEmpty()) {
            log.warn("No forecast data in product_daily_forecast for productId={} between {} and {}",
                    productId, today, toDate);

            // نخزّن ملخّص بدون stockout (null) و avg=0
            ProductStockForecastSummaryRow row = new ProductStockForecastSummaryRow();
            row.setProductId(productId);
            row.setCurrentStock(currentStock);
            row.setAvgDailyDemand(BigDecimal.ZERO);
            row.setExpectedStockoutDate(null);
            row.setRecommendedReorderQty(BigDecimal.ZERO);

            summaryRepository.upsertSummary(row);

            return mapToDto(row);
        }

        // 3) حساب متوسط الطلب اليومي من yhat
        double sum = 0.0;
        int count = 0;

        for (ForecastPointDTO p : forecastPoints) {
            double v = p.getYhat();
            if (v < 0) {
                v = 0; // نتجاهل القيم السالبة
            }
            sum += v;
            count++;
        }

        BigDecimal avgDailyDemand =
                (count > 0)
                        ? BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        // 4) حساب تاريخ نفاد المخزون المتوقع
        double remaining = currentStock != null ? currentStock.doubleValue() : 0.0;
        LocalDate stockoutDate = null;

        for (ForecastPointDTO p : forecastPoints) {
            double demand = p.getYhat();
            if (demand < 0) {
                demand = 0;
            }
            remaining -= demand;
            if (remaining <= 0) {
                stockoutDate = p.getDs();
                break;
            }
        }

        // 5) حساب الكمية المقترحة لإعادة الطلب
        int targetCoverageDays = 14; // ممكن نخلّيها configurable لاحقاً
        BigDecimal targetStock =
                avgDailyDemand.multiply(BigDecimal.valueOf(targetCoverageDays));

        BigDecimal recommendedReorderQty =
                targetStock.subtract(currentStock != null ? currentStock : BigDecimal.ZERO);

        if (recommendedReorderQty.compareTo(BigDecimal.ZERO) < 0) {
            recommendedReorderQty = BigDecimal.ZERO;
        }

        // 6) التخزين في الجدول
        ProductStockForecastSummaryRow row = new ProductStockForecastSummaryRow();
        row.setProductId(productId);
        row.setCurrentStock(currentStock);
        row.setAvgDailyDemand(avgDailyDemand);
        row.setExpectedStockoutDate(stockoutDate);
        row.setRecommendedReorderQty(recommendedReorderQty);

        summaryRepository.upsertSummary(row);

        return mapToDto(row);
    }

    public ProductStockForecastSummaryDTO getSummaryForProduct(Long productId) {
        ProductStockForecastSummaryRow row =
                summaryRepository.findByProductId(productId);

        if (row == null) {
            return null;
        }

        return mapToDto(row);
    }

    /**
     * 🔹 Batch: إعادة بناء الملخّص لكل المنتجات (أو المنتجات ذات مخزون معين)
     *
     * @param horizonDays   عدد الأيام التي نستخدمها من التنبؤ (مثلاً 30)
     * @param minCurrentStock أقل مخزون كشرط لاختيار المنتج (مثلاً 1 أو 5)
     * @return عدد المنتجات التي تمت معالجتها
     */
    public int rebuildSummaryForProductsBatch(int horizonDays,
                                              BigDecimal minCurrentStock) {

        if (minCurrentStock == null) {
            minCurrentStock = BigDecimal.ZERO;
        }

        // نجيب كل المنتجات من ال View مع فلتر على المخزون
        List<ProductCurrentStockViewRow> products =
                currentStockRepository.findAllWithMinTotalQty(minCurrentStock);

        int processedCount = 0;

        for (ProductCurrentStockViewRow p : products) {
            Long productId = p.getProductId();
            try {
                rebuildSummaryForProduct(productId, horizonDays);
                processedCount++;
            } catch (Exception ex) {
                // ما نوقف الـ Batch لو منتج واحد عمل مشكلة
                log.error("Error rebuilding stock forecast summary for productId={}", productId, ex);
            }
        }

        log.info("Batch stock forecast summary completed. horizonDays={}, minCurrentStock={}, processedCount={}",
                horizonDays, minCurrentStock, processedCount);

        return processedCount;
    }

    /**
     * نسخة مختصرة: إعادة بناء الملخص لكل المنتجات بدون فلتر مخزون
     */
    public int rebuildSummaryForAllProducts(int horizonDays) {
        return rebuildSummaryForProductsBatch(horizonDays, BigDecimal.ZERO);
    }

    private ProductStockForecastSummaryDTO mapToDto(ProductStockForecastSummaryRow row) {
        ProductStockForecastSummaryDTO dto = new ProductStockForecastSummaryDTO();
        dto.setProductId(row.getProductId());
        dto.setCurrentStock(row.getCurrentStock());
        dto.setAvgDailyDemand(row.getAvgDailyDemand());
        dto.setExpectedStockoutDate(row.getExpectedStockoutDate());
        dto.setRecommendedReorderQty(row.getRecommendedReorderQty());
        return dto;
    }

    public ProductStockForecastSummaryListResponseDTO listSummaries(
            Integer atRiskWithinDays,
            boolean onlyAtRisk,
            boolean onlyWithReorder,
            int page,
            int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        LocalDate today = LocalDate.now();
        int days = (atRiskWithinDays != null && atRiskWithinDays > 0) ? atRiskWithinDays : 30;
        LocalDate thresholdDate = today.plusDays(days);

        int offset = page * size;

        long total = summaryViewRepository.count(
                onlyAtRisk,
                thresholdDate,
                onlyWithReorder
        );

        if (total == 0) {
            ProductStockForecastSummaryListResponseDTO empty = new ProductStockForecastSummaryListResponseDTO();
            empty.setTotal(0);
            empty.setPage(page);
            empty.setSize(size);
            empty.setItems(List.of());
            return empty;
        }

        List<ProductStockForecastSummaryViewRepository.ProductStockForecastSummaryViewRow> rows =
                summaryViewRepository.findPage(
                        onlyAtRisk,
                        thresholdDate,
                        onlyWithReorder,
                        size,
                        offset
                );

        List<ProductStockForecastSummaryListItemDTO> items = rows.stream().map(row -> {
            ProductStockForecastSummaryListItemDTO dto = new ProductStockForecastSummaryListItemDTO();
            dto.setProductId(row.getProductId());
            dto.setSku(row.getSku());
            dto.setName(row.getName());
            dto.setBrand(row.getBrand());
            dto.setCurrentStock(row.getCurrentStock());
            dto.setAvgDailyDemand(row.getAvgDailyDemand());
            dto.setExpectedStockoutDate(row.getExpectedStockoutDate());
            dto.setRecommendedReorderQty(row.getRecommendedReorderQty());
            return dto;
        }).collect(Collectors.toList());

        ProductStockForecastSummaryListResponseDTO response = new ProductStockForecastSummaryListResponseDTO();
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setItems(items);

        return response;
    }
}
