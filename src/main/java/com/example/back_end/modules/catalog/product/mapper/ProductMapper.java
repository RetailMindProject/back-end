package com.example.back_end.modules.catalog.product.mapper;

import com.example.back_end.modules.catalog.category.dto.CategorySimpleDTO;
import com.example.back_end.modules.catalog.product.dto.ProductCreateDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductSimpleDTO;
import com.example.back_end.modules.catalog.product.dto.ProductUpdateDTO;
import com.example.back_end.modules.catalog.product.entity.Product;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {}

    public static Product toEntity(ProductCreateDTO dto) {
        if (dto == null) return null;
        return Product.builder()
                .sku(dto.getSku())
                .name(dto.getName())
                .brand(dto.getBrand())
                .description(dto.getDescription())
                .defaultCost(dto.getDefaultCost())
                .defaultPrice(dto.getDefaultPrice())
                .taxRate(dto.getTaxRate())
                .unit(dto.getUnit())
                .wholesalePrice(dto.getWholesalePrice())
                .isActive(dto.getIsActive() == null ? Boolean.TRUE : dto.getIsActive())
                .build();
    }

    public static void updateEntity(ProductUpdateDTO dto, Product entity) {
        if (dto == null || entity == null) return;
        if (dto.getSku() != null) entity.setSku(dto.getSku());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getBrand() != null) entity.setBrand(dto.getBrand());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getDefaultCost() != null) entity.setDefaultCost(dto.getDefaultCost());
        if (dto.getDefaultPrice() != null) entity.setDefaultPrice(dto.getDefaultPrice());
        if (dto.getTaxRate() != null) entity.setTaxRate(dto.getTaxRate());
        if (dto.getUnit() != null) entity.setUnit(dto.getUnit());
        if (dto.getWholesalePrice() != null) entity.setWholesalePrice(dto.getWholesalePrice());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
    }

    public static ProductResponseDTO toResponse(Product entity) {
        if (entity == null) return null;
        return ProductResponseDTO.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .brand(entity.getBrand())
                .description(entity.getDescription())
                .defaultCost(entity.getDefaultCost())
                .defaultPrice(entity.getDefaultPrice())
                .taxRate(entity.getTaxRate())
                .unit(entity.getUnit())
                .wholesalePrice(entity.getWholesalePrice())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert to ProductResponseDTO with categories
     */
    public static ProductResponseDTO toResponse(Product entity, List<CategorySimpleDTO> categories) {
        if (entity == null) return null;
        return ProductResponseDTO.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .brand(entity.getBrand())
                .description(entity.getDescription())
                .defaultCost(entity.getDefaultCost())
                .defaultPrice(entity.getDefaultPrice())
                .taxRate(entity.getTaxRate())
                .unit(entity.getUnit())
                .wholesalePrice(entity.getWholesalePrice())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .categories(categories)
                .build();
    }

    /**
     * Convert to ProductSimpleDTO (lightweight for POS cart)
     */
    public static ProductSimpleDTO toSimpleDTO(Product entity) {
        if (entity == null) return null;
        return ProductSimpleDTO.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .defaultPrice(entity.getDefaultPrice())
                .taxRate(entity.getTaxRate())
                .unit(entity.getUnit())
                .build();
    }
}