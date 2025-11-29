package com.example.back_end.modules.products.service;

import com.example.back_end.modules.products.DTO.ProductCreateDTO;
import com.example.back_end.modules.products.DTO.ProductUpdateDTO;
import com.example.back_end.modules.products.DTO.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
