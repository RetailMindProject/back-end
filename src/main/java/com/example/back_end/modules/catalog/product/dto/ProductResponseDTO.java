package com.example.back_end.modules.catalog.product.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Long id;
    private String sku;
    private String name;
    private String brand;
    private String description;
    private BigDecimal defaultCost;
    private BigDecimal defaultPrice;
    private BigDecimal taxRate;
    private String unit;
    private BigDecimal wholesalePrice;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Mini primary image representation expected by frontend: product.image.url
     */
    private ProductImageMiniDTO image;

    private String primaryImageUrl;
    private List<ProductImageDTO> images;
    private BigDecimal storeQty;
    private BigDecimal warehouseQty;
    private List<com.example.back_end.modules.catalog.category.dto.CategoryDTO> categories;
}
