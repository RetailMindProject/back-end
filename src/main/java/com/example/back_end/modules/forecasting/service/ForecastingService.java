package com.example.back_end.modules.forecasting.service;

import com.example.back_end.modules.forecasting.dto.*;
import com.example.back_end.modules.forecasting.repository.DailyProductSalesRow;
import com.example.back_end.modules.forecasting.repository.ForecastingDataRepository;
import com.example.back_end.modules.forecasting.repository.ProductDailyForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastingService {

    private final ForecastingDataRepository dataRepository;
    private final ProductDailyForecastRepository forecastRepository;
    private final WebClient forecastingWebClient;

    /**
     * تشغيل التنبؤ لمنتج معيّن لفترة معينة
     */
    public ForecastResponseDTO generateForecastForProduct(Long productId,
                                                          int horizonDays,
                                                          LocalDate fromDate,
                                                          LocalDate toDate) {

        // 1) جلب السلسلة الزمنية من الـ view
        List<DailyProductSalesRow> rows =
                dataRepository.findDailySalesForProduct(productId, fromDate, toDate);

        if (rows.isEmpty()) {
            log.warn("No sales data found for productId={} between {} and {}", productId, fromDate, toDate);
            // ممكن ترجع null أو ترمي Exception حسب ما تفضّل
            return null;
        }

        // 2) تحويلها إلى TimePointRequestDTO
        List<TimePointRequestDTO> series = new ArrayList<>();
        for (DailyProductSalesRow row : rows) {
            double qty = toDouble(row.getTotalQtySold());
            Integer promoAnyFlag = row.getPromoAnyFlag();
            Double avgDiscountPct = row.getAvgDiscountPct() != null
                    ? row.getAvgDiscountPct().doubleValue()
                    : null;

            series.add(new TimePointRequestDTO(
                    row.getSalesDate(),
                    qty,
                    promoAnyFlag,
                    avgDiscountPct
            ));
        }

        // 3) بناء ForecastRequestDTO
        ForecastRequestDTO requestDTO = new ForecastRequestDTO();
        requestDTO.setProductId(productId);
        requestDTO.setHorizonDays(horizonDays);
        // نستخدم هذين الـ regressors كبداية
        requestDTO.setRegressors(List.of("promo_any_flag", "avg_discount_pct"));
        requestDTO.setSeries(series);

        // 4) استدعاء FastAPI
        ForecastResponseDTO response;
        try {
            response = forecastingWebClient.post()
                    .uri("/api/v1/forecast/product")
                    .bodyValue(requestDTO)
                    .retrieve()
                    .bodyToMono(ForecastResponseDTO.class)
                    .block(); // ممكن لاحقاً تحوّلها لـ reactive بالكامل
            response.setProductId(productId);
        } catch (WebClientResponseException e) {
            log.error("Error calling forecasting service: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error calling forecasting service", e);
            throw e;
        }

        if (response == null || response.getForecast() == null) {
            log.warn("Empty forecast response for productId={}", productId);
            return response;
        }

        // 5) تخزين النتائج في جدول product_daily_forecast
        forecastRepository.upsertForecast(productId, response.getForecast());

        return response;
    }

    public ForecastResponseDTO getStoredForecastForProduct(Long productId,
                                                           LocalDate fromDate,
                                                           LocalDate toDate) {
        List<ForecastPointDTO> points =
                forecastRepository.findForecastForProduct(productId, fromDate, toDate);

        if (points == null || points.isEmpty()) {
            log.warn("No stored forecast found for productId={} between {} and {}",
                    productId, fromDate, toDate);
            return null;
        }

        ForecastResponseDTO dto = new ForecastResponseDTO();
        dto.setProductId(productId);
        dto.setForecast(points);
        return dto;
    }


    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
    public ProductHistoryAndForecastDTO getHistoryAndForecastForProduct(
            Long productId,
            LocalDate historyFrom,
            LocalDate historyTo,
            LocalDate forecastFrom,
            LocalDate forecastTo
    ) {
        // 1) history من الـ view
        List<DailyProductSalesRow> historyRows =
                dataRepository.findDailySalesForProduct(productId, historyFrom, historyTo);

        List<SalesHistoryPointDTO> historyPoints = new ArrayList<>();
        if (historyRows != null) {
            for (DailyProductSalesRow row : historyRows) {
                double qty = toDouble(row.getTotalQtySold());
                if (qty < 0) {
                    qty = 0;
                }
                SalesHistoryPointDTO hp = new SalesHistoryPointDTO();
                hp.setDs(row.getSalesDate());
                hp.setY(qty);
                historyPoints.add(hp);
            }
        }

        // 2) forecast من جدول product_daily_forecast
        List<ForecastPointDTO> forecastPoints =
                forecastRepository.findForecastForProduct(productId, forecastFrom, forecastTo);

        // لو ما في لا history ولا forecast -> نرجّع null
        boolean historyEmpty = (historyPoints == null || historyPoints.isEmpty());
        boolean forecastEmpty = (forecastPoints == null || forecastPoints.isEmpty());

        if (historyEmpty && forecastEmpty) {
            log.warn("No history nor forecast for productId={} (history {} -> {}, forecast {} -> {})",
                    productId, historyFrom, historyTo, forecastFrom, forecastTo);
            return null;
        }

        ProductHistoryAndForecastDTO dto = new ProductHistoryAndForecastDTO();
        dto.setProductId(productId);
        dto.setHistory(historyPoints != null ? historyPoints : List.of());
        dto.setForecast(forecastPoints != null ? forecastPoints : List.of());

        return dto;
    }
}
