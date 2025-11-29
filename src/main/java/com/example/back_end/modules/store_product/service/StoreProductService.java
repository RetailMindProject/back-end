package com.example.back_end.modules.store_product.service;

import com.example.back_end.modules.store_product.dto.StoreProductResponseDTO;
import com.example.back_end.modules.store_product.dto.StoreTransferRequestDTO;

import java.util.List;

public interface StoreProductService {

    StoreProductResponseDTO transferFromInventoryToStore(StoreTransferRequestDTO dto);

    StoreProductResponseDTO transferFromStoreToInventory(StoreTransferRequestDTO dto);

    List<StoreProductResponseDTO> search(String q);

    StoreProductResponseDTO getByProductId(Long productId);
}
