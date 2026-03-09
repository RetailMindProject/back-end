package com.example.back_end.modules.catalog.product.mapper;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.product.dto.ProductCreateDTO;
import com.example.back_end.modules.catalog.product.dto.ProductImageDTO;
import com.example.back_end.modules.catalog.product.dto.ProductImageMiniDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductSimpleDTO;
import com.example.back_end.modules.catalog.product.dto.ProductUpdateDTO;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import com.example.back_end.modules.store_product.entity.StockSnapshot;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

        ProductImageMiniDTO primaryImageMini = entity.getProductMedia().stream()
                .filter(pm -> pm.getMedia() != null)
                .filter(ProductMedia::getIsPrimary)
                .findFirst()
                .map(pm -> ProductImageMiniDTO.builder()
                        .url(toFrontendPath(pm.getMedia().getUrl()))
                        .altText(pm.getMedia().getAltText())
                        .build())
                .orElse(null);

        // Keep the old field for backward compatibility (but normalized as path as well)
        String primaryImageUrl = primaryImageMini == null ? null : primaryImageMini.getUrl();

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
                .image(primaryImageMini)
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
                .map(ProductMapper::categoryToDto)
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
                .url(toFrontendPath(productMedia.getMedia().getUrl()))
                .mimeType(productMedia.getMedia().getMimeType())
                .title(productMedia.getMedia().getTitle())
                .altText(productMedia.getMedia().getAltText())
                .sortOrder(productMedia.getSortOrder())
                .isPrimary(productMedia.getIsPrimary())
                .build();
    }

    private static String toFrontendPath(String url) {
        if (url == null || url.isBlank()) return url;

        // If already a path like /picture/Tea.jpg
        if (url.startsWith("/")) return url;

        // If full URL, keep only the path part.
        // Examples:
        // - http://host/picture/Tea.jpg -> /picture/Tea.jpg
        // - https://host:8080/uploads/picture/Tea.jpg -> /uploads/picture/Tea.jpg
        int idx = url.indexOf("//");
        if (idx >= 0) {
            int firstSlashAfterHost = url.indexOf('/', idx + 2);
            if (firstSlashAfterHost >= 0) {
                return url.substring(firstSlashAfterHost);
            }
        }

        // Otherwise treat it as relative path without leading slash
        return "/" + url;
    }

    /**
     * Convert to ProductSimpleDTO (lightweight for POS cart)
     */
    public static ProductSimpleDTO toSimpleDTO(Product entity) {
        if (entity == null) return null;

        ProductImageMiniDTO primaryImageMini = entity.getProductMedia().stream()
                .filter(pm -> pm.getMedia() != null)
                .filter(ProductMedia::getIsPrimary)
                .findFirst()
                .map(pm -> ProductImageMiniDTO.builder()
                        .url(toFrontendPath(pm.getMedia().getUrl()))
                        .altText(pm.getMedia().getAltText())
                        .build())
                .orElse(null);

        return ProductSimpleDTO.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .image(primaryImageMini)
                .defaultPrice(entity.getDefaultPrice())
                .taxRate(entity.getTaxRate())
                .unit(entity.getUnit())
                .build();
    }

    /**
     * Helper method to convert Category to CategoryDTO
     */
    private static CategoryDTO categoryToDto(Category entity) {
        if (entity == null) return null;
        return CategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .parentName(entity.getParent() != null ? entity.getParent().getName() : null)
                .build();
    }
}