package com.example.back_end.modules.dashboard.inventorydashboard.controller;

import com.example.back_end.modules.dashboard.inventorydashboard.dto.*;
import com.example.back_end.modules.dashboard.inventorydashboard.service.InventoryDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryDashboardController {

    private final InventoryDashboardService inventoryDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<InventorySummaryDTO> getSummary() {
        return ResponseEntity.ok(inventoryDashboardService.getSummary());
    }

    @GetMapping("/recent-movements")
    public ResponseEntity<List<RecentInventoryMovementDTO>> getRecentMovements() {
        return ResponseEntity.ok(inventoryDashboardService.getRecentMovements());
    }

    @GetMapping("/category-movement")
    public ResponseEntity<List<CategoryMovementDTO>> getCategoryMovement() {
        return ResponseEntity.ok(inventoryDashboardService.getWeeklyCategoryMovement());
    }

    @GetMapping("/category-sales-pie")
    public ResponseEntity<List<CategorySalesPieDTO>> getCategorySalesPie() {
        return ResponseEntity.ok(inventoryDashboardService.getCategorySalesPie());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopMovedProductDTO>> getTopMovedProducts() {
        return ResponseEntity.ok(inventoryDashboardService.getTopMovedProducts());
    }

    @GetMapping("/weekly-trend")
    public ResponseEntity<List<WeeklyInventoryMovementDTO>> getWeeklyTrend() {
        return ResponseEntity.ok(inventoryDashboardService.getWeeklyMovementTrend());
    }
}
