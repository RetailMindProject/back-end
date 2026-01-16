package com.example.back_end.modules.store_product.service;

import com.example.back_end.modules.store_product.dto.AdjustQuantityDTO;
import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;

public interface StoreProductService {

    StoreProductResponseDTO addToInventory(StoreTransferRequestDTO dto);

    StoreProductResponseDTO transferFromInventoryToStore(StoreTransferRequestDTO dto);

    StoreProductResponseDTO transferFromStoreToInventory(StoreTransferRequestDTO dto);

    StoreProductResponseDTO removeFromStore(StoreTransferRequestDTO dto);

    StoreProductResponseDTO increaseStoreQuantity(AdjustQuantityDTO dto);

    StoreProductResponseDTO decreaseStoreQuantity(AdjustQuantityDTO dto);

    StoreProductResponseDTO increaseWarehouseQuantity(AdjustQuantityDTO dto);

    StoreProductResponseDTO decreaseWarehouseQuantity(AdjustQuantityDTO dto);

    Page<StoreProductResponseDTO> search(String q, Pageable pageable);

    Page<StoreProductResponseDTO> filter(String brand, Boolean isActive,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String sku, Pageable pageable);

    StoreProductResponseDTO getByProductId(Long productId);

    // Get products with existing inventory for re-stocking window
    Page<StoreProductResponseDTO> getProductsWithInventory(String q, Pageable pageable);

    // Get batches (expiration dates) for a product with their total quantities
    java.util.List<com.example.back_end.modules.store_product.dto.ProductBatchDTO> getBatchesForProduct(Long productId);

    // Re-stock existing product (add quantity with optional expiration date)
    StoreProductResponseDTO restock(StoreTransferRequestDTO dto);

    // Record waste (decrease quantity with reason)
    com.example.back_end.modules.store_product.dto.WasteResponseDTO recordWaste(
            com.example.back_end.modules.store_product.dto.WasteRequestDTO dto);

    // Get waste history
    Page<com.example.back_end.modules.store_product.dto.WasteHistoryDTO> getWasteHistory(
            Long productId, Long batchId, Instant fromDate, Instant toDate, String wasteReason, Pageable pageable);

    // Get wasted products (products that have waste movements)
    Page<StoreProductResponseDTO> getWastedProducts(String q, Pageable pageable);
}
