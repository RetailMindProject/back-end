package com.example.back_end.modules.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOfferEnrichmentDTO {
    private Long productId;
    private String name;
    private Long offerId;
    private String offerTitle;
    private String offerCode;
    private String offerType;
    private String discountType;
    private BigDecimal discountValue;
    private Integer effectiveDiscountPercentage;
    private BigDecimal originalPrice;
    private BigDecimal priceAfterDiscount;
}

