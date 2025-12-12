package com.example.back_end.modules.catalog.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.back_end.modules.catalog.product.dto.ProductCreateDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductSimpleDTO;
import com.example.back_end.modules.catalog.product.dto.ProductUpdateDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    
    // CRUD Operations
    ProductResponseDTO create(ProductCreateDTO dto);
    ProductResponseDTO getById(Long id);
    Page<ProductResponseDTO> search(String q, String brand, Boolean isActive,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    Pageable pageable);
    ProductResponseDTO update(Long id, ProductUpdateDTO dto);
    void delete(Long id);
    
    // POS Operations
    List<ProductResponseDTO> getAllActiveProducts();
    ProductSimpleDTO getProductBySku(String sku);
    List<ProductResponseDTO> getProductsByCategory(Long categoryId);
    Page<ProductResponseDTO> getProductsByCategoryPaginated(Long categoryId, Pageable pageable);
    List<ProductSimpleDTO> quickSearch(String searchTerm);
}