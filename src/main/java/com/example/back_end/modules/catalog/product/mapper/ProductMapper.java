package com.example.back_end.modules.catalog.product.mapper;

import com.example.back_end.modules.catalog.product.dto.ProductCreateDTO;
import com.example.back_end.modules.catalog.product.dto.ProductImageDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductUpdateDTO;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.catalog.category.mapper.CategoryMapper;
import com.example.back_end.modules.catalog.category.entity.Category;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

public final class ProductMapper {

    private ProductMapper() {}

    public static Product toEntity(ProductCreateDTO dto) {
        if (dto == null) return null;
        return Product.builder()
                .sku(dto.getSku() != null ? dto.getSku().trim() : null)
                .name(dto.getName() != null ? dto.getName().trim() : null)
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
        if (dto.getSku() != null) entity.setSku(dto.getSku().trim());
        if (dto.getName() != null) entity.setName(dto.getName().trim());
        if (dto.getBrand() != null) entity.setBrand(dto.getBrand().trim());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription().trim());
        if (dto.getDefaultCost() != null) entity.setDefaultCost(dto.getDefaultCost());
        if (dto.getDefaultPrice() != null) entity.setDefaultPrice(dto.getDefaultPrice());
        if (dto.getTaxRate() != null) entity.setTaxRate(dto.getTaxRate());
        if (dto.getUnit() != null) entity.setUnit(dto.getUnit());
        if (dto.getWholesalePrice() != null) entity.setWholesalePrice(dto.getWholesalePrice());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
    }

    public static ProductResponseDTO toResponse(Product entity) {
        if (entity == null) return null;

        // Get primary image URL
        String primaryImageUrl = entity.getProductMedia().stream()
                .filter(pm -> pm.getMedia() != null)
                .filter(ProductMedia::getIsPrimary)
                .findFirst()
                .map(pm -> pm.getMedia().getUrl())
                .orElse(null);

        // Map all images
        List<ProductImageDTO> images = entity.getProductMedia().stream()
                .filter(pm -> pm.getMedia() != null)
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(ProductMapper::toImageDTO)
                .filter(img -> img != null)
                .collect(Collectors.toList());

        // expand categories to include parents for display
        Set<Category> expandedCategories = new LinkedHashSet<>();
        for (Category c : entity.getCategories()) {
            expandedCategories.add(c);
            if (c.getParent() != null) {
                expandedCategories.add(c.getParent());
            }
        }

        ProductResponseDTO dto = ProductResponseDTO.builder()
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
                .primaryImageUrl(primaryImageUrl)
                .images(images)
                .storeQty(null)
                .warehouseQty(null)
                .build();

        dto.setCategories(expandedCategories.stream()
                .sorted((a, b) -> {
                    if (a.getName() == null && b.getName() == null) return 0;
                    if (a.getName() == null) return 1;
                    if (b.getName() == null) return -1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList()));

        return dto;
    }

    public static ProductResponseDTO toResponse(Product entity, StockSnapshot snapshot) {
        if (entity == null) return null;

        ProductResponseDTO dto = toResponse(entity);
        
        // Add quantities from snapshot
        if (snapshot != null) {
            dto.setStoreQty(snapshot.getStoreQty() != null ? snapshot.getStoreQty() : BigDecimal.ZERO);
            dto.setWarehouseQty(snapshot.getWarehouseQty() != null ? snapshot.getWarehouseQty() : BigDecimal.ZERO);
        } else {
            dto.setStoreQty(BigDecimal.ZERO);
            dto.setWarehouseQty(BigDecimal.ZERO);
        }

        return dto;
    }

    public static ProductImageDTO toImageDTO(ProductMedia productMedia) {
        if (productMedia == null || productMedia.getMedia() == null) return null;

        return ProductImageDTO.builder()
                .mediaId(productMedia.getMedia().getId())
                .url(productMedia.getMedia().getUrl())
                .mimeType(productMedia.getMedia().getMimeType())
                .title(productMedia.getMedia().getTitle())
                .altText(productMedia.getMedia().getAltText())
                .sortOrder(productMedia.getSortOrder())
                .isPrimary(productMedia.getIsPrimary())
                .build();
    }
}
