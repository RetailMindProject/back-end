package com.example.backend.modules.catalog.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.backend.modules.catalog.product.dto.ProductCreateDTO;
import com.example.backend.modules.catalog.product.dto.ProductResponseDTO;
import com.example.backend.modules.catalog.product.dto.ProductUpdateDTO;

import java.math.BigDecimal;

public interface ProductService {
    ProductResponseDTO create(ProductCreateDTO dto);
    ProductResponseDTO getById(Long id);
    Page<ProductResponseDTO> search(String q, String brand, Boolean isActive,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    Pageable pageable);
    ProductResponseDTO update(Long id, ProductUpdateDTO dto);
    void delete(Long id);
}
