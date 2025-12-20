package com.example.back_end.modules.forecasting.controller;

import com.example.back_end.modules.forecasting.dto.*;
import com.example.back_end.modules.forecasting.service.ForecastBatchService;
import com.example.back_end.modules.forecasting.service.ForecastingService;
import com.example.back_end.modules.forecasting.service.StockForecastSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/forecasting")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForecastingController {

    private final ForecastingService forecastingService;
    private final ForecastBatchService forecastBatchService;
    private final StockForecastSummaryService stockForecastSummaryService;

    @PostMapping("/products/{productId}/run")
    public ResponseEntity<ForecastResponseDTO> runForecast(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "30") int horizonDays,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        LocalDate today = LocalDate.now();
        if (toDate == null) {
            toDate = today;
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(4600);
        }

        ForecastResponseDTO response = forecastingService.generateForecastForProduct(
                productId,
                horizonDays,
                fromDate,
                toDate
        );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ForecastResponseDTO> getStoredForecast(
            @PathVariable Long productId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        LocalDate today = LocalDate.now();

        if (fromDate == null) {
            fromDate = today;
        }
        if (toDate == null) {
            toDate = today.plusDays(30);
        }

        ForecastResponseDTO response =
                forecastingService.getStoredForecastForProduct(productId, fromDate, toDate);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    // 🔹🔹 Endpoint جديد: Batch Forecast لكل المنتجات المؤهَّلة 🔹🔹
    @PostMapping("/products/run-batch")
    public ResponseEntity<BatchForecastResponseDTO> runBatchForecast(
            @RequestParam(defaultValue = "30") int horizonDays,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "5") int minPoints
    ) {
        LocalDate today = LocalDate.now();
        if (toDate == null) {
            toDate = today;
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(4600);
        }

        BatchForecastResponseDTO result =
                forecastBatchService.runBatchForEligibleProducts(
                        horizonDays,
                        fromDate,
                        toDate,
                        minPoints
                );

        if (result.getTotalProducts() == 0) {
            // لا يوجد أي منتج مؤهَّل في هذه الفترة
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(result);
    }
    @PostMapping("/products/{productId}/stock-summary/rebuild")
    public ResponseEntity<ProductStockForecastSummaryDTO> rebuildStockSummaryForProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "30") int horizonDays
    ) {
        ProductStockForecastSummaryDTO dto =
                stockForecastSummaryService.rebuildSummaryForProduct(productId, horizonDays);

        if (dto == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(dto);
    }

    // 🔹 2) قراءة summary جاهز من الجدول (للدashboard مثلاً)
    @GetMapping("/products/{productId}/stock-summary")
    public ResponseEntity<ProductStockForecastSummaryDTO> getStockSummaryForProduct(
            @PathVariable Long productId
    ) {
        ProductStockForecastSummaryDTO dto =
                stockForecastSummaryService.getSummaryForProduct(productId);

        if (dto == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(dto);
    }
    @PostMapping("/stock-summary/rebuild-batch")
    public ResponseEntity<?> rebuildStockSummaryBatch(
            @RequestParam(defaultValue = "30") int horizonDays,
            @RequestParam(required = false) BigDecimal minCurrentStock
    ) {
        int processedCount = stockForecastSummaryService.rebuildSummaryForProductsBatch(
                horizonDays,
                minCurrentStock != null ? minCurrentStock : BigDecimal.ZERO
        );

        // نرجع JSON بسيط فيه عدد المنتجات التي تم معالجتها
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("processedCount", processedCount);
        body.put("horizonDays", horizonDays);
        body.put("minCurrentStock", minCurrentStock != null ? minCurrentStock : BigDecimal.ZERO);

        return ResponseEntity.ok(body);
    }

    // (اختياري) نسخة مختصرة بدون minCurrentStock
    @PostMapping("/stock-summary/rebuild-all")
    public ResponseEntity<?> rebuildStockSummaryForAll(
            @RequestParam(defaultValue = "30") int horizonDays
    ) {
        int processedCount = stockForecastSummaryService.rebuildSummaryForAllProducts(horizonDays);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("processedCount", processedCount);
        body.put("horizonDays", horizonDays);

        return ResponseEntity.ok(body);
    }
    @GetMapping("/stock-summary")
    public ResponseEntity<ProductStockForecastSummaryListResponseDTO> listStockSummaries(
            @RequestParam(required = false, defaultValue = "30") Integer atRiskWithinDays,
            @RequestParam(required = false, defaultValue = "false") boolean onlyAtRisk,
            @RequestParam(required = false, defaultValue = "false") boolean onlyWithReorder,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        ProductStockForecastSummaryListResponseDTO dto =
                stockForecastSummaryService.listSummaries(
                        atRiskWithinDays,
                        onlyAtRisk,
                        onlyWithReorder,
                        page,
                        size
                );

        if (dto.getTotal() == 0) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(dto);
    }
    @GetMapping("/products/{productId}/history-and-forecast")
    public ResponseEntity<ProductHistoryAndForecastDTO> getHistoryAndForecast(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "180") int historyDays,
            @RequestParam(defaultValue = "30") int horizonDays
    ) {
        LocalDate today = LocalDate.now();

        if (historyDays <= 0) {
            historyDays = 180;
        }
        if (horizonDays <= 0) {
            horizonDays = 30;
        }

        // history: آخر historyDays يوم قبل اليوم
        LocalDate historyFrom = today.minusDays(historyDays);
        LocalDate historyTo = today.minusDays(1);

        // forecast: من اليوم وحتى horizonDays قادم
        LocalDate forecastFrom = today;
        LocalDate forecastTo = today.plusDays(horizonDays - 1);

        ProductHistoryAndForecastDTO dto =
                forecastingService.getHistoryAndForecastForProduct(
                        productId,
                        historyFrom,
                        historyTo,
                        forecastFrom,
                        forecastTo
                );

        if (dto == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(dto);
    }
}
