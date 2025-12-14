package com.example.back_end.modules.catalog.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.back_end.modules.catalog.product.dto.AddProductMediaDTO;
import com.example.back_end.modules.catalog.product.dto.ProductCreateDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductUpdateDTO;
import com.example.back_end.modules.catalog.product.dto.UpdateProductMediaDTO;

import java.math.BigDecimal;

public interface ProductService {
    ProductResponseDTO create(ProductCreateDTO dto);
    ProductResponseDTO getById(Long id);
    Page<ProductResponseDTO> search(String q, Pageable pageable);
    Page<ProductResponseDTO> filter(String brand, Boolean isActive,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    String sku, Pageable pageable);
    ProductResponseDTO update(Long id, ProductUpdateDTO dto);
    void delete(Long id);
    ProductResponseDTO addImage(Long productId, AddProductMediaDTO dto);
    ProductResponseDTO updateImage(Long productId, Long mediaId, UpdateProductMediaDTO dto);
    void removeImage(Long productId, Long mediaId);
    ProductResponseDTO setPrimaryImage(Long productId, Long mediaId);
}
