package com.example.back_end.modules.forecasting.service;

import com.example.back_end.modules.forecasting.dto.*;
import com.example.back_end.modules.forecasting.repository.ForecastingProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastBatchService {

    private final ForecastingProductRepository forecastingProductRepository;
    private final ForecastingService forecastingService;

    /**
     * تشغيل التنبؤ بشكل Batch لكل المنتجات المؤهَّلة
     * (التي لديها على الأقل minPoints يوم مبيعات في الفترة المحددة).
     */
    public BatchForecastResponseDTO runBatchForEligibleProducts(int horizonDays,
                                                                LocalDate fromDate,
                                                                LocalDate toDate,
                                                                int minPoints) {

        // 1) جلب المنتجات المؤهَّلة
        List<Long> productIds =
                forecastingProductRepository.findEligibleProductIds(fromDate, toDate, minPoints);

        BatchForecastResponseDTO result = new BatchForecastResponseDTO();
        result.setTotalProducts(productIds.size());

        List<ProductForecastBatchItemDTO> details = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        log.info("Starting batch forecast: {} eligible products, horizonDays={}, period=[{}..{}], minPoints={}",
                productIds.size(), horizonDays, fromDate, toDate, minPoints);

        // 2) تشغيل التنبؤ لكل منتج مؤهَّل
        for (Long productId : productIds) {
            ProductForecastBatchItemDTO item = new ProductForecastBatchItemDTO();
            item.setProductId(productId);

            try {
                ForecastResponseDTO response = forecastingService.generateForecastForProduct(
                        productId,
                        horizonDays,
                        fromDate,
                        toDate
                );

                if (response == null || response.getForecast() == null || response.getForecast().isEmpty()) {
                    item.setStatus("SKIPPED_NO_DATA");
                    item.setMessage("No sales or forecast data available for this product in the given period.");
                    skippedCount++;
                    log.warn("Batch forecast: product {} skipped - no data / empty forecast.", productId);
                } else {
                    item.setStatus("SUCCESS");
                    item.setMessage("Forecast generated and stored successfully.");
                    item.setForecastPoints(response.getForecast().size());
                    successCount++;
                    log.info("Batch forecast: product {} success with {} forecast points.",
                            productId, response.getForecast().size());
                }

            } catch (WebClientResponseException e) {
                String body = e.getResponseBodyAsString();
                if (e.getStatusCode().value() == 400 &&
                        body != null &&
                        body.contains("Not enough data points")) {

                    item.setStatus("SKIPPED_NOT_ENOUGH_DATA");
                    item.setMessage("Not enough data points to build forecast (from FastAPI).");
                    skippedCount++;
                    log.warn("Batch forecast: product {} skipped - not enough data points (400 from FastAPI).",
                            productId);

                } else {
                    item.setStatus("ERROR");
                    item.setMessage("Error calling forecasting service: " + e.getStatusCode());
                    errorCount++;
                    log.error("Batch forecast: product {} error from FastAPI, status={}, body={}",
                            productId, e.getStatusCode(), body);
                }

            } catch (Exception e) {
                item.setStatus("ERROR");
                item.setMessage("Unexpected error: " + e.getMessage());
                errorCount++;
                log.error("Batch forecast: product {} unexpected error.", productId, e);
            }

            details.add(item);
        }

        result.setSuccessCount(successCount);
        result.setSkippedCount(skippedCount);
        result.setErrorCount(errorCount);
        result.setDetails(details);

        log.info("Batch forecast finished: total={}, success={}, skipped={}, errors={}",
                result.getTotalProducts(), successCount, skippedCount, errorCount);

        return result;
    }
}
