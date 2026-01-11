package com.example.back_end.modules.recommendation.controller;

import com.example.back_end.modules.recommendation.dto.CustomerDTO;
import com.example.back_end.modules.recommendation.dto.ProductCandidateDTO;
import com.example.back_end.modules.recommendation.dto.ProductCatalogDTO;
import com.example.back_end.modules.recommendation.dto.ProductOfferEnrichmentDTO;
import com.example.back_end.modules.recommendation.dto.PurchaseEventDTO;
import com.example.back_end.modules.recommendation.dto.TrendingProductDTO;
import com.example.back_end.modules.recommendation.dto.UserHistoryItemDTO;
import com.example.back_end.modules.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reco")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/users/{customerId}/history")
    public ResponseEntity<List<UserHistoryItemDTO>> getUserHistory(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        List<UserHistoryItemDTO> result = recommendationService.getUserHistory(customerId, safeLimit);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/candidates")
    public ResponseEntity<List<ProductCandidateDTO>> getProductCandidates(
            @RequestParam(defaultValue = "500") int limit,
            @RequestParam(defaultValue = "true") boolean inStockOnly
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        List<ProductCandidateDTO> result = recommendationService.getProductCandidates(safeLimit, inStockOnly);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingProductDTO>> getTrending(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int safeDays = Math.max(days, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        List<TrendingProductDTO> result = recommendationService.getTrending(safeDays, safeLimit);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/training/purchase-events")
    public ResponseEntity<List<PurchaseEventDTO>> getPurchaseEventsForTraining(
            @RequestParam(defaultValue = "10000") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100000);
        int safeOffset = Math.max(offset, 0);
        List<PurchaseEventDTO> result = recommendationService.getPurchaseEventsForTraining(safeLimit, safeOffset);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/training/product-catalog")
    public ResponseEntity<List<ProductCatalogDTO>> getProductCatalogForTraining(
            @RequestParam(defaultValue = "10000") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100000);
        int safeOffset = Math.max(offset, 0);
        List<ProductCatalogDTO> result = recommendationService.getProductCatalogForTraining(safeLimit, safeOffset);
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/training/customers")
    public ResponseEntity<List<CustomerDTO>> getCustomersForTraining(
            @RequestParam(defaultValue = "10000") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100000);
        int safeOffset = Math.max(offset, 0);
        List<CustomerDTO> result = recommendationService.getCustomersForTraining(safeLimit, safeOffset);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/offers/active-by-products")
    public ResponseEntity<List<ProductOfferEnrichmentDTO>> getActiveOffersByProducts(
            @RequestBody List<Long> productIds
    ) {
        if (productIds == null) {
            return ResponseEntity.ok(List.of());
        }
        int safeLimitProductIds = Math.min(productIds.size(), 2000);
        List<Long> safeProductIds = productIds.subList(0, safeLimitProductIds);
        List<ProductOfferEnrichmentDTO> result = recommendationService.getActiveOffersByProductIds(safeProductIds);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/offers/active-products")
    public ResponseEntity<List<ProductOfferEnrichmentDTO>> getActiveOfferProducts(
            @RequestParam(defaultValue = "500") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        int safeOffset = Math.max(offset, 0);
        List<ProductOfferEnrichmentDTO> result = recommendationService.getActiveOffersPaginated(safeLimit, safeOffset);
        return ResponseEntity.ok(result);
    }
}
