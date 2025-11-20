package com.example.back_end.modules.dashboard.storedashboard.service;

import com.example.back_end.modules.dashboard.storedashboard.dto.*;
import com.example.back_end.modules.dashboard.storedashboard.projection.*;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StoreDashboardService {

    private final OrderRepository orderRepository;
    private static final int DEFAULT_DAYS = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Cacheable("storeSummary")
    public StoreSummaryDTO getSummary() {
        LocalDateTime from = LocalDateTime.now().minusDays(DEFAULT_DAYS);

        BigDecimal totalSales = orderRepository.sumSalesSince(from);
        Long totalOrders = orderRepository.countOrdersSince(from);

        // Get daily sales to find most recent day
        List<StoreDailySalesProjection> dailySales = orderRepository.findDailySalesSince(from);

        BigDecimal recentDailyAmount = BigDecimal.ZERO;
        String recentDate = LocalDate.now().format(DATE_FORMATTER);

        if (!dailySales.isEmpty()) {
            StoreDailySalesProjection mostRecent = dailySales.get(dailySales.size() - 1);
            recentDailyAmount = mostRecent.getTotalAmount();
            recentDate = mostRecent.getSaleDate().format(DATE_FORMATTER);
        }

        // Get most popular product
        StoreTopProductProjection mostPopular = orderRepository.findMostPopularProduct(from);

        String mostPopularProduct = "N/A";
        BigDecimal mostPopularSold = BigDecimal.ZERO;
        BigDecimal mostPopularRevenue = BigDecimal.ZERO;

        if (mostPopular != null) {
            mostPopularProduct = mostPopular.getProductName();
            mostPopularSold = mostPopular.getSold();
            mostPopularRevenue = mostPopular.getRevenue();
        }

        return StoreSummaryDTO.builder()
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .recentDailyAmount(recentDailyAmount)
                .recentDate(recentDate)
                .mostPopularProduct(mostPopularProduct)
                .mostPopularSold(mostPopularSold)
                .mostPopularRevenue(mostPopularRevenue)
                .build();
    }

    @Cacheable("storeSalesTrend")
    public List<SalesTrendDTO> getSalesTrend() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6); // Last 7 days
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        List<StoreWeeklySalesProjection> rows = orderRepository.findWeeklySalesTrend(fromDateTime);

        // Map by date
        Map<LocalDate, StoreWeeklySalesProjection> byDate = new HashMap<>();
        for (StoreWeeklySalesProjection row : rows) {
            byDate.put(row.getSaleDate(), row);
        }

        // Build 7-day trend with zeros for missing days
        List<SalesTrendDTO> result = new ArrayList<>();
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < 7; i++) {
            LocalDate d = fromDate.plusDays(i);
            StoreWeeklySalesProjection row = byDate.get(d);

            BigDecimal revenue = (row != null && row.getRevenue() != null)
                    ? row.getRevenue()
                    : BigDecimal.ZERO;

            Long orders = (row != null && row.getOrders() != null)
                    ? row.getOrders()
                    : 0L;

            // Use day of week name
            String dayName = dayNames[d.getDayOfWeek().getValue() - 1];

            result.add(SalesTrendDTO.builder()
                    .day(dayName)
                    .revenue(revenue)
                    .orders(orders)
                    .build());
        }

        return result;
    }

    @Cacheable("storeCategoryCounts")
    public List<CategoryCountDTO> getCategoryCounts() {
        LocalDateTime from = LocalDateTime.now().minusDays(DEFAULT_DAYS);

        List<StoreCategoryCountProjection> rows = orderRepository.findCategoryProductCounts(from);

        return rows.stream()
                .map(row -> CategoryCountDTO.builder()
                        .name(row.getCategoryName())
                        .value(row.getProductCount())
                        .build())
                .toList();
    }

    @Cacheable("storeTopProducts")
    public List<TopProductDTO> getTopProducts() {
        LocalDateTime from = LocalDateTime.now().minusDays(DEFAULT_DAYS);
        int limit = 5;

        List<StoreTopProductProjection> rows = orderRepository.findTopProducts(from, limit);

        return rows.stream()
                .map(row -> TopProductDTO.builder()
                        .productId(row.getProductId())
                        .name(row.getProductName())
                        .sku(row.getSku())
                        .sold(row.getSold())
                        .revenue(row.getRevenue())
                        .build())
                .toList();
    }

    @Cacheable("storeRecentDaily")
    public List<RecentDailyDTO> getRecentDaily() {
        LocalDateTime from = LocalDateTime.now().minusDays(DEFAULT_DAYS);

        List<StoreDailySalesProjection> rows = orderRepository.findDailySalesSince(from);

        return rows.stream()
                .map(row -> RecentDailyDTO.builder()
                        .date(row.getSaleDate().format(DATE_FORMATTER))
                        .amount(row.getTotalAmount())
                        .orders(row.getOrderCount())
                        .build())
                .toList();
    }
}

