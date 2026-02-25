package com.example.back_end.modules.reports.service;

import com.example.back_end.modules.cashier.repository.CashierUserRepository;
import com.example.back_end.modules.dashboard.storedashboard.projection.StoreTopProductProjection;
import com.example.back_end.modules.dashboard.storedashboard.projection.StoreWeeklySalesProjection;
import com.example.back_end.modules.register.entity.User;
import com.example.back_end.modules.reports.dto.*;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;
    private final CashierUserRepository cashierUserRepository;

    /**
     * Get sales summary/KPIs for a date range
     */
    public ReportSummaryDTO getSummary(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime to = toDate != null ? toDate.atTime(23, 59, 59) : LocalDateTime.now();
        
        // Use 'to' for discount and tax calculations

        BigDecimal totalSales = orderRepository.sumSalesSince(from);
        Long totalOrders = orderRepository.countOrdersSince(from);

        // Calculate average order value
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0 && totalSales != null) {
            averageOrderValue = totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        // Get discount and tax totals using date range
        BigDecimal totalDiscountAmount = calculateTotalDiscount(from, to);
        BigDecimal totalTaxAmount = calculateTotalTax(from, to);

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

        return ReportSummaryDTO.builder()
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .totalDiscountAmount(totalDiscountAmount)
                .totalTaxAmount(totalTaxAmount)
                .mostPopularProduct(mostPopularProduct)
                .mostPopularSold(mostPopularSold)
                .mostPopularRevenue(mostPopularRevenue)
                .fromDate(fromDate != null ? fromDate : from.toLocalDate())
                .toDate(toDate != null ? toDate : to.toLocalDate())
                .build();
    }

    /**
     * Get time series data for a date range
     */
    public List<TimeSeriesDTO> getTimeSeries(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        // Note: toDate parameter is accepted for API consistency but current query only uses 'from'
        // Future enhancement: filter results by toDate in memory or update query

        List<StoreWeeklySalesProjection> rows = orderRepository.findWeeklySalesTrend(from);

        return rows.stream()
                .map(row -> {
                    BigDecimal avgOrderValue = BigDecimal.ZERO;
                    if (row.getOrders() > 0 && row.getRevenue() != null) {
                        avgOrderValue = row.getRevenue().divide(
                                BigDecimal.valueOf(row.getOrders()), 2, RoundingMode.HALF_UP);
                    }

                    return TimeSeriesDTO.builder()
                            .date(row.getSaleDate())
                            .revenue(row.getRevenue())
                            .orders(row.getOrders())
                            .averageOrderValue(avgOrderValue)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get filter metadata (cashiers, statuses, date ranges)
     */
    public ReportMetaDTO getMeta() {
        // Get all cashiers
        List<User> cashiers = cashierUserRepository.findActiveCashiers();
        List<ReportMetaDTO.CashierOption> cashierOptions = cashiers.stream()
                .map(cashier -> {
                    String name = (cashier.getFirstName() != null ? cashier.getFirstName() : "") +
                            (cashier.getLastName() != null ? " " + cashier.getLastName() : "");
                    return ReportMetaDTO.CashierOption.builder()
                            .id(cashier.getId().longValue())
                            .name(name.trim())
                            .build();
                })
                .collect(Collectors.toList());

        // Get available statuses
        List<String> statuses = List.of("PAID", "CANCELLED", "DRAFT", "HOLD");

        // Get date range from orders (we'll need to add a query for this)
        LocalDate earliestDate = getEarliestOrderDate();
        LocalDate latestDate = getLatestOrderDate();

        return ReportMetaDTO.builder()
                .cashiers(cashierOptions)
                .statuses(statuses)
                .earliestOrderDate(earliestDate)
                .latestOrderDate(latestDate)
                .build();
    }

    /**
     * Export order report data (returns the same structure as order report)
     * This will be handled by the controller to format as CSV/Excel
     */
    public List<com.example.back_end.modules.sales.order.dto.OrderDTO.OrderReportItem> getExportData(
            LocalDate fromDate,
            LocalDate toDate,
            String cashierName,
            Long cashierId,
            String status,
            Integer limit,
            Integer offset
    ) {
        // Reuse the order report service logic
        // This will be called from controller which has access to OrderService
        return new ArrayList<>();
    }

    // Helper methods

    private BigDecimal calculateTotalDiscount(LocalDateTime from, LocalDateTime to) {
        BigDecimal result = orderRepository.sumDiscountSince(from, to);
        return result != null ? result : BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalTax(LocalDateTime from, LocalDateTime to) {
        BigDecimal result = orderRepository.sumTaxSince(from, to);
        return result != null ? result : BigDecimal.ZERO;
    }

    private LocalDate getEarliestOrderDate() {
        LocalDate result = orderRepository.getEarliestOrderDate();
        return result != null ? result : LocalDate.now().minusDays(30);
    }

    private LocalDate getLatestOrderDate() {
        LocalDate result = orderRepository.getLatestOrderDate();
        return result != null ? result : LocalDate.now();
    }
}
