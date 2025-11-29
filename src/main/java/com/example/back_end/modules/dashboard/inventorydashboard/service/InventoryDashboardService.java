package com.example.back_end.modules.dashboard.inventorydashboard.service;

import com.example.back_end.modules.dashboard.inventorydashboard.dto.*;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.stock.repository.InventoryMovementRepository;
import com.example.back_end.modules.stock.repository.projection.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryDashboardService {

    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;

    @Cacheable("inventorySummary")
    public InventorySummaryDTO getSummary() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        BigDecimal totalIn = movementRepository.sumInSince(oneWeekAgo);
        BigDecimal totalOut = movementRepository.sumOutSince(oneWeekAgo);
        Long movementCount = movementRepository.countMovementsSince(oneWeekAgo);

        Long mostMovedProductId = movementRepository.findMostMovedProductIdSince(oneWeekAgo);
        String mostMovedProductName = null;

        if (mostMovedProductId != null) {
            Product product = productRepository.findById(mostMovedProductId).orElse(null);
            if (product != null) {
                mostMovedProductName = product.getName();
            }
        }

        return InventorySummaryDTO.builder()
                .totalInThisWeek(totalIn)
                .totalOutThisWeek(totalOut)
                .movementCount(movementCount)
                .mostMovedProduct(mostMovedProductName == null ? "N/A" : mostMovedProductName)
                .build();
    }

    @Cacheable("recentInventoryMovements")
    public List<RecentInventoryMovementDTO> getRecentMovements() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        int limit = 20;

        List<InventoryRecentMovementProjection> rows =
                movementRepository.findRecentMovements(from, limit);

        return rows.stream()
                .map(row -> RecentInventoryMovementDTO.builder()
                        .date(row.getMovedAt().toLocalDate())
                        .productName(row.getProductName())
                        .categoryName(row.getCategoryName())
                        .locationType(row.getLocationType())
                        .refType(row.getRefType())
                        .quantityChange(row.getQuantityChange())
                        .build())
                .toList();
    }

    // 🔹 Weekly Movement Trend (Line chart)
    @Cacheable("weeklyInventoryTrend")
    public List<WeeklyInventoryMovementDTO> getWeeklyMovementTrend() {
        // نريد آخر 7 أيام (من اليوم - 6 إلى اليوم)
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        List<InventoryWeeklyMovementProjection> rows =
                movementRepository.findWeeklyMovementSince(fromDateTime);

        // نحول النتائج إلى Map بـ key = التاريخ
        Map<LocalDate, InventoryWeeklyMovementProjection> byDate = new HashMap<>();
        for (InventoryWeeklyMovementProjection row : rows) {
            byDate.put(row.getMovementDate(), row);
        }

        // نبني قائمة ثابتة 7 أيام، مع تعبئة صفر إذا ما كان في حركات في ذلك اليوم
        List<WeeklyInventoryMovementDTO> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = fromDate.plusDays(i);
            InventoryWeeklyMovementProjection row = byDate.get(d);

            BigDecimal totalIn = (row != null && row.getTotalIn() != null)
                    ? row.getTotalIn()
                    : BigDecimal.ZERO;

            BigDecimal totalOut = (row != null && row.getTotalOut() != null)
                    ? row.getTotalOut()
                    : BigDecimal.ZERO;

            result.add(
                    WeeklyInventoryMovementDTO.builder()
                            .date(d)
                            .totalIn(totalIn)
                            .totalOut(totalOut)
                            .build()
            );
        }

        return result;
    }
    @Cacheable("weeklyCategoryMovement")
    public List<CategoryMovementDTO> getWeeklyCategoryMovement() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        List<InventoryCategoryMovementProjection> rows =
                movementRepository.findCategoryMovementLastWeek(oneWeekAgo);

        return rows.stream()
                .map(row -> CategoryMovementDTO.builder()
                        .categoryName(row.getCategoryName())
                        .totalIn(row.getTotalIn())
                        .totalOut(row.getTotalOut())
                        .build())
                .toList();
    }

    @Cacheable("weeklyCategorySalesPie")
    public List<CategorySalesPieDTO> getCategorySalesPie() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        List<InventoryCategorySalesProjection> rows =
                movementRepository.findCategorySalesLastWeek(oneWeekAgo);

        return rows.stream()
                .map(row -> CategorySalesPieDTO.builder()
                        .categoryName(row.getCategoryName())
                        .salesQty(row.getTotalSalesQty())
                        .build())
                .toList();
    }
    @Cacheable("topMovedProducts")
    public List<TopMovedProductDTO> getTopMovedProducts() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        int limit = 5; // مثلاً top 5

        List<InventoryTopProductProjection> rows =
                movementRepository.findTopMovedProductsLastWeek(oneWeekAgo, limit);

        return rows.stream()
                .map(row -> TopMovedProductDTO.builder()
                        .productId(row.getProductId())
                        .productName(row.getProductName())
                        .categoryName(row.getCategoryName())
                        .totalMovementQty(row.getTotalMovementQty())
                        .build())
                .toList();
    }
}
