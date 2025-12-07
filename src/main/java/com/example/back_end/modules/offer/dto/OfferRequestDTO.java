package com.example.back_end.modules.offer.dto;

import com.example.back_end.modules.offer.entity.Offer;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class OfferRequestDTO {

    @Size(max = 40, message = "Code must not exceed 40 characters")
    private String code;

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must not exceed 120 characters")
    private String title;

    private String description;

    @NotNull(message = "Offer type is required")
    private Offer.OfferType offerType;

    @NotNull(message = "Discount type is required")
    private Offer.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    private Boolean isActive = true;

    // For PRODUCT type
    private List<Long> productIds;

    // For CATEGORY type
    private List<Long> categoryIds;

    // For ORDER type
    @DecimalMin(value = "0.0", message = "Minimum order amount must be positive")
    private BigDecimal minOrderAmount;

    private Boolean applyOnce;

    // For BUNDLE type
    @Valid
    private List<BundleItemDTO> bundleItems;

    // Validation method
    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startAt != null && endAt != null) {
            return endAt.isAfter(startAt);
        }
        return true;
    }

    @AssertTrue(message = "Product IDs are required for PRODUCT offer type")
    public boolean isValidProductOffer() {
        if (offerType == Offer.OfferType.PRODUCT) {
            return productIds != null && !productIds.isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Category IDs are required for CATEGORY offer type")
    public boolean isValidCategoryOffer() {
        if (offerType == Offer.OfferType.CATEGORY) {
            return categoryIds != null && !categoryIds.isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Minimum order amount is required for ORDER offer type")
    public boolean isValidOrderOffer() {
        if (offerType == Offer.OfferType.ORDER) {
            return minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }

    @AssertTrue(message = "Bundle items are required for BUNDLE offer type (minimum 2 products)")
    public boolean isValidBundleOffer() {
        if (offerType == Offer.OfferType.BUNDLE) {
            return bundleItems != null && bundleItems.size() >= 2;
        }
        return true;
    }

    @AssertTrue(message = "Percentage discount must be between 0 and 100")
    public boolean isValidPercentageDiscount() {
        if (discountType == Offer.DiscountType.PERCENTAGE && discountValue != null) {
            return discountValue.compareTo(BigDecimal.ZERO) > 0
                    && discountValue.compareTo(new BigDecimal("100")) <= 100;
        }
        return true;
    }
}