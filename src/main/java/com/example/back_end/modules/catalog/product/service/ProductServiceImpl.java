package com.example.back_end.modules.catalog.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.back_end.modules.catalog.category.dto.CategorySimpleDTO;
import com.example.back_end.modules.catalog.product.dto.*;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductCategory;
import com.example.back_end.modules.catalog.product.mapper.ProductMapper;
import com.example.back_end.modules.catalog.product.repository.ProductCategoryRepository;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    public ProductResponseDTO create(ProductCreateDTO dto) {
        if (dto.getSku() != null && repository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
        }
        Product saved = repository.save(ProductMapper.toEntity(dto));
        return toProductResponseWithCategories(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getById(Long id) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return toProductResponseWithCategories(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> search(String q, String brand, Boolean isActive,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           Pageable pageable) {
        return repository.search(q, brand, isActive, minPrice, maxPrice, pageable)
                .map(this::toProductResponseWithCategories);
    }

    @Override
    public ProductResponseDTO update(Long id, ProductUpdateDTO dto) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        if (dto.getSku() != null && !dto.getSku().equals(p.getSku()) && repository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.getSku());
        }
        ProductMapper.updateEntity(dto, p);
        return toProductResponseWithCategories(repository.save(p));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Product not found: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllActiveProducts() {
        List<Product> products = repository.findAllActiveProducts();
        return products.stream()
                .map(this::toProductResponseWithCategories)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSimpleDTO getProductBySku(String sku) {
        Product product = repository.findBySku(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with SKU: " + sku));
        
        if (!product.getIsActive()) {
            throw new IllegalStateException("Product is inactive: " + sku);
        }
        
        return ProductMapper.toSimpleDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        List<Product> products = repository.findProductsByCategoryId(categoryId);
        return products.stream()
                .map(this::toProductResponseWithCategories)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByCategoryPaginated(Long categoryId, Pageable pageable) {
        Page<Product> products = repository.findProductsByCategoryIdPaginated(categoryId, pageable);
        return products.map(this::toProductResponseWithCategories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSimpleDTO> quickSearch(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }
        
        List<Product> products = repository.quickSearch(searchTerm);
        return products.stream()
                .map(ProductMapper::toSimpleDTO)
                .collect(Collectors.toList());
    }

    private ProductResponseDTO toProductResponseWithCategories(Product product) {
        List<ProductCategory> productCategories = productCategoryRepository.findByProductId(product.getId());
        
        List<CategorySimpleDTO> categories = productCategories.stream()
                .map(pc -> CategorySimpleDTO.builder()
                        .id(pc.getCategory().getId())
                        .name(pc.getCategory().getName())
                        .build())
                .collect(Collectors.toList());
        
        return ProductMapper.toResponse(product, categories);
    }
}