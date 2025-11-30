package com.example.back_end.modules.catalog.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductUpdateDTO {

    @Size(max = 64)
    private String sku;

    @Size(max = 120)
    private String name;

    @Size(max = 80)
    private String brand;

    @Size(max = 500)
    private String description;

    @PositiveOrZero
    private BigDecimal defaultCost;

    @PositiveOrZero
    private BigDecimal defaultPrice;

    @PositiveOrZero
    @Digits(integer = 3, fraction = 2)
    private BigDecimal taxRate;

    @Size(max = 20)
    private String unit;

    @PositiveOrZero
    private BigDecimal wholesalePrice;

    private Boolean isActive;

    // Option 1: Replace all images (backward compatible)
    private java.util.Set<Long> mediaIds;

    // Option 2: Granular control (add, remove, update)
    private java.util.Set<Long> mediaIdsToAdd;
    private java.util.Set<Long> mediaIdsToRemove;
    private java.util.Set<com.example.back_end.modules.catalog.product.dto.ProductImageUpdateDTO> imagesToUpdate;
}
