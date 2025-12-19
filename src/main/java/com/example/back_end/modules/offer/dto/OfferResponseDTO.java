package com.example.back_end.modules.offer.dto;

import com.example.back_end.modules.offer.entity.Offer;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponseDTO {

    private Long id;
    private String code;
    private String title;
    private String description;
    private Offer.OfferType offerType;
    private Offer.DiscountType discountType;
    private BigDecimal discountValue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private String createdByName;

    // Product offer details
    private List<ProductInfoDTO> products;

    // Category offer details
    private List<CategoryInfoDTO> categories;

    // Order offer details
    private BigDecimal minOrderAmount;
    private Boolean applyOnce;

    // Bundle offer details
    private List<BundleItemInfoDTO> bundleItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfoDTO {
        private Long id;
        private String sku;
        private String name;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfoDTO {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BundleItemInfoDTO {
        private Long productId;
        private String sku;
        private String name;
        private BigDecimal requiredQty;
        private BigDecimal price;
    }
}