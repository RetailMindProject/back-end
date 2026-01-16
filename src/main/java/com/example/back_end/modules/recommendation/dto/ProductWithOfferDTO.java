package com.example.back_end.modules.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithOfferDTO {
    private Long productId;
    private String name;
    private String sku;
    private BigDecimal currentPrice;
    private Long offerId;
    private String offerCode;
    private String offerTitle;
    private String offerType;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal priceAfterDiscount;
    private Integer discountPercentage;
}

