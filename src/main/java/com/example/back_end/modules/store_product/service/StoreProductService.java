package com.example.back_end.modules.store_product.service;

import com.example.back_end.modules.store_product.dto.AdjustQuantityDTO;
import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

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
}
