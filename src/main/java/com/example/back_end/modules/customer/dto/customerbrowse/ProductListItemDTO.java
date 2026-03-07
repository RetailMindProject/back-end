package com.example.back_end.modules.customer.dto.customerbrowse;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductListItemDTO(
        Long id,
        String sku,
        String name,
        String brand,
        BigDecimal defaultPrice,
        BigDecimal taxRate,
        String primaryImageUrl,
        OfferMiniDTO offer
) {}

