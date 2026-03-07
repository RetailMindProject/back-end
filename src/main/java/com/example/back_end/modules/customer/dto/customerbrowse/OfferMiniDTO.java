package com.example.back_end.modules.customer.dto.customerbrowse;

import com.example.back_end.modules.offer.entity.Offer;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OfferMiniDTO(
        Long id,
        String title,
        Offer.DiscountType discountType,
        BigDecimal discountValue
) {}

