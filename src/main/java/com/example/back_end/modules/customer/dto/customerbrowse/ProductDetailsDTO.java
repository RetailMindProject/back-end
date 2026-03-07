package com.example.back_end.modules.customer.dto.customerbrowse;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProductDetailsDTO(
        Long id,
        String sku,
        String name,
        String brand,
        String description,
        BigDecimal defaultPrice,
        BigDecimal taxRate,
        String unit,
        List<CategoryDTO> categories,
        List<ProductImageDTO> images,
        List<OfferDTO> offers
) {}

