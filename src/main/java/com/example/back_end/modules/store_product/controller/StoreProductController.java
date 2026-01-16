package com.example.back_end.modules.store_product.controller;

import com.example.back_end.modules.store_product.dto.AdjustQuantityDTO;
import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;
import com.example.back_end.modules.store_product.dto.WasteRequestDTO;
import com.example.back_end.modules.store_product.dto.WasteResponseDTO;
import com.example.back_end.modules.store_product.dto.WasteHistoryDTO;
import com.example.back_end.modules.store_product.service.StoreProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/store-products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoreProductController {

    private final StoreProductService service;

    // Add products to inventory (PURCHASE)
    @PostMapping("/add-to-inventory")
    public ResponseEntity<StoreProductResponseDTO> addToInventory(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.addToInventory(dto));
    }

    // Transfer WAREHOUSE → STORE
    @PostMapping("/transfer-to-store")
    public ResponseEntity<StoreProductResponseDTO> transferToStore(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.transferFromInventoryToStore(dto));
    }

    // Transfer STORE → WAREHOUSE
    @PostMapping("/transfer-to-inventory")
    public ResponseEntity<StoreProductResponseDTO> transferToInventory(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.transferFromStoreToInventory(dto));
    }

    // Remove/Delete from store (STORE → WAREHOUSE transfer)
    @DeleteMapping("/remove-from-store")
    public ResponseEntity<StoreProductResponseDTO> removeFromStore(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.removeFromStore(dto));
    }

    // Increase quantity in store (ADJUSTMENT)
    @PostMapping("/store/increase")
    public ResponseEntity<StoreProductResponseDTO> increaseStoreQuantity(
            @Valid @RequestBody AdjustQuantityDTO dto) {

        return ResponseEntity.ok(service.increaseStoreQuantity(dto));
    }

    // Decrease quantity in store (ADJUSTMENT)
    @PostMapping("/store/decrease")
    public ResponseEntity<StoreProductResponseDTO> decreaseStoreQuantity(
            @Valid @RequestBody AdjustQuantityDTO dto) {

        return ResponseEntity.ok(service.decreaseStoreQuantity(dto));
    }

    // Increase quantity in warehouse (ADJUSTMENT)
    @PostMapping("/warehouse/increase")
    public ResponseEntity<StoreProductResponseDTO> increaseWarehouseQuantity(
            @Valid @RequestBody AdjustQuantityDTO dto) {

        return ResponseEntity.ok(service.increaseWarehouseQuantity(dto));
    }

    // Decrease quantity in warehouse (ADJUSTMENT) - creates movement record
    @PostMapping("/warehouse/decrease")
    public ResponseEntity<StoreProductResponseDTO> decreaseWarehouseQuantity(
            @Valid @RequestBody AdjustQuantityDTO dto) {

        return ResponseEntity.ok(service.decreaseWarehouseQuantity(dto));
    }

    // Simple search by name or SKU
    @GetMapping("/search")
    public ResponseEntity<Page<StoreProductResponseDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(service.search(q, pageable));
    }

    // Advanced filter with sorting
    @GetMapping("/filter")
    public ResponseEntity<Page<StoreProductResponseDTO>> filter(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sku,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        String[] sp = sort.split(",", 2);
        Sort.Direction dir = (sp.length > 1 && "desc".equalsIgnoreCase(sp[1])) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sp[0]));
        return ResponseEntity.ok(service.filter(brand, isActive, minPrice, maxPrice, sku, pageable));
    }

    // Get wasted products (products that have waste movements)
    @GetMapping("/waste/products")
    public ResponseEntity<Page<StoreProductResponseDTO>> getWastedProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(service.getWastedProducts(q, pageable));
    }

    // Get stock for a specific product
    @GetMapping("/{productId}")
    public ResponseEntity<StoreProductResponseDTO> getByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(service.getByProductId(productId));
    }

    // Get products with existing inventory (for re-stocking window)
    @GetMapping("/with-inventory")
    public ResponseEntity<Page<StoreProductResponseDTO>> getProductsWithInventory(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(service.getProductsWithInventory(q, pageable));
    }

    // Get batches (expiration dates) for a product
    @GetMapping("/{productId}/batches")
    public ResponseEntity<java.util.List<com.example.back_end.modules.store_product.dto.ProductBatchDTO>> getBatchesForProduct(
            @PathVariable Long productId) {

        return ResponseEntity.ok(service.getBatchesForProduct(productId));
    }

    // Re-stock existing product (add quantity with optional expiration date)
    @PostMapping("/restock")
    public ResponseEntity<StoreProductResponseDTO> restock(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.restock(dto));
    }

    // Record waste (decrease quantity with reason)
    @PostMapping("/waste")
    public ResponseEntity<WasteResponseDTO> recordWaste(
            @Valid @RequestBody WasteRequestDTO dto) {

        return ResponseEntity.ok(service.recordWaste(dto));
    }

    // Get waste history
    @GetMapping("/waste-history")
    public ResponseEntity<Page<WasteHistoryDTO>> getWasteHistory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(required = false) String wasteReason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "wastedAt"));
        return ResponseEntity.ok(service.getWasteHistory(productId, batchId, fromDate, toDate, wasteReason, pageable));
    }   
}
