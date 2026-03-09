package com.example.back_end.modules.customer.dto.customerbrowse;

import com.example.back_end.modules.offer.entity.Offer;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OfferDTO(
        Long id,
        String title,
        Offer.DiscountType discountType,
        BigDecimal discountValue,
        Offer.OfferType offerType,
        LocalDateTime startAt,
        LocalDateTime endAt
) {}

