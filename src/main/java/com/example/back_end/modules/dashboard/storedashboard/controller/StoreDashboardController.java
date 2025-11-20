package com.example.back_end.modules.dashboard.storedashboard.controller;

import com.example.back_end.modules.dashboard.storedashboard.dto.*;
import com.example.back_end.modules.dashboard.storedashboard.service.StoreDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/store")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoreDashboardController {

    private final StoreDashboardService storeDashboardService;

    /**
     * Get summary metrics: total sales, total orders, recent daily, most popular product
     * GET /api/dashboard/store/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<StoreSummaryDTO> getSummary() {
        return ResponseEntity.ok(storeDashboardService.getSummary());
    }

    /**
     * Get 7-day sales trend (line chart data): day, revenue, orders
     * GET /api/dashboard/store/sales-trend
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<List<SalesTrendDTO>> getSalesTrend() {
        return ResponseEntity.ok(storeDashboardService.getSalesTrend());
    }

    /**
     * Get category product counts (pie chart data)
     * GET /api/dashboard/store/category-counts
     */
    @GetMapping("/category-counts")
    public ResponseEntity<List<CategoryCountDTO>> getCategoryCounts() {
        return ResponseEntity.ok(storeDashboardService.getCategoryCounts());
    }

    /**
     * Get top products by quantity sold
     * GET /api/dashboard/store/top-products
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts() {
        return ResponseEntity.ok(storeDashboardService.getTopProducts());
    }

    /**
     * Get recent daily sales (table data): date, amount, orders
     * GET /api/dashboard/store/recent-daily
     */
    @GetMapping("/recent-daily")
    public ResponseEntity<List<RecentDailyDTO>> getRecentDaily() {
        return ResponseEntity.ok(storeDashboardService.getRecentDaily());
    }
}

