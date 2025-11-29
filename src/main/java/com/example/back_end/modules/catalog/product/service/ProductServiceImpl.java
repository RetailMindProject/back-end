package com.example.back_end.modules.catalog.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.back_end.modules.catalog.product.dto.*;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.mapper.ProductMapper;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    @Override
    public ProductResponseDTO create(ProductCreateDTO dto) {
        if (dto.getSku() != null && repository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
        }
        Product saved = repository.save(ProductMapper.toEntity(dto));
        return ProductMapper.toResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    public ProductResponseDTO getById(Long id) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return ProductMapper.toResponse(p);
    }

    @Override @Transactional(readOnly = true)
    public Page<ProductResponseDTO> search(String q, String brand, Boolean isActive,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           Pageable pageable) {
        return repository.search(q, brand, isActive, minPrice, maxPrice, pageable)
                .map(ProductMapper::toResponse);
    }

    @Override
    public ProductResponseDTO update(Long id, ProductUpdateDTO dto) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        if (dto.getSku() != null && !dto.getSku().equals(p.getSku()) && repository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
        }
        ProductMapper.updateEntity(dto, p);
        return ProductMapper.toResponse(repository.save(p));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Product not found: " + id);
        }
        repository.deleteById(id);
    }
}
