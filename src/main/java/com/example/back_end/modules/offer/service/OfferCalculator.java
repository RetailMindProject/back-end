package com.example.back_end.modules.offer.service;

import com.example.back_end.modules.offer.entity.Offer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for calculating offer discounts
 */
@Component
@Slf4j
public class OfferCalculator {

    /**
     * Calculate discount amount based on offer type and original amount
     *
     * @param offer The offer to apply
     * @param originalAmount The original amount before discount
     * @return The discount amount (always positive)
     */
    public BigDecimal calculateDiscount(Offer offer, BigDecimal originalAmount) {
        if (offer == null || originalAmount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount;

        switch (offer.getDiscountType()) {
            case PERCENTAGE:
                discountAmount = calculatePercentageDiscount(originalAmount, offer.getDiscountValue());
                break;
            case FIXED_AMOUNT:
                discountAmount = offer.getDiscountValue();
                break;
            default:
                discountAmount = BigDecimal.ZERO;
        }

        // Ensure discount doesn't exceed original amount
        if (discountAmount.compareTo(originalAmount) > 0) {
            log.warn("Discount amount ({}) exceeds original amount ({}). Capping at original amount.",
                    discountAmount, originalAmount);
            return originalAmount;
        }

        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the final amount after applying discount
     *
     * @param originalAmount The original amount
     * @param discountAmount The discount amount to subtract
     * @return The final amount after discount
     */
    public BigDecimal calculateFinalAmount(BigDecimal originalAmount, BigDecimal discountAmount) {
        if (originalAmount == null) {
            return BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            return originalAmount;
        }

        BigDecimal finalAmount = originalAmount.subtract(discountAmount);

        // Ensure final amount is not negative
        return finalAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate percentage discount
     *
     * @param amount The original amount
     * @param percentage The discount percentage (e.g., 20 for 20%)
     * @return The discount amount
     */
    private BigDecimal calculatePercentageDiscount(BigDecimal amount, BigDecimal percentage) {
        return amount
                .multiply(percentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the effective discount rate (as percentage)
     *
     * @param originalAmount Original amount
     * @param discountAmount Discount amount
     * @return The discount rate as percentage
     */
    public BigDecimal calculateDiscountRate(BigDecimal originalAmount, BigDecimal discountAmount) {
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            return BigDecimal.ZERO;
        }

        return discountAmount
                .multiply(new BigDecimal("100"))
                .divide(originalAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * Check if order meets minimum amount requirement
     *
     * @param orderTotal Total order amount
     * @param minAmount Minimum required amount
     * @return true if order meets minimum, false otherwise
     */
    public boolean meetsMinimumAmount(BigDecimal orderTotal, BigDecimal minAmount) {
        if (orderTotal == null || minAmount == null) {
            return false;
        }
        return orderTotal.compareTo(minAmount) >= 0;
    }

    /**
     * Calculate how much more is needed to reach minimum amount
     *
     * @param currentAmount Current order amount
     * @param minAmount Minimum required amount
     * @return Amount needed to reach minimum (0 if already met)
     */
    public BigDecimal calculateAmountNeededForMinimum(BigDecimal currentAmount, BigDecimal minAmount) {
        if (currentAmount == null || minAmount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal needed = minAmount.subtract(currentAmount);
        return needed.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Format discount for display
     *
     * @param offer The offer
     * @return Formatted string (e.g., "20%" or "$50.00")
     */
    public String formatDiscount(Offer offer) {
        if (offer == null) {
            return "";
        }

        switch (offer.getDiscountType()) {
            case PERCENTAGE:
                return offer.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            case FIXED_AMOUNT:
                return "$" + offer.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
            default:
                return "";
        }
    }

    /**
     * Calculate savings percentage
     *
     * @param originalPrice Original price
     * @param finalPrice Final price after discount
     * @return Savings as percentage
     */
    public String calculateSavingsPercentage(BigDecimal originalPrice, BigDecimal finalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return "0%";
        }

        BigDecimal savings = originalPrice.subtract(finalPrice != null ? finalPrice : BigDecimal.ZERO);
        BigDecimal savingsPercent = savings
                .multiply(new BigDecimal("100"))
                .divide(originalPrice, 0, RoundingMode.HALF_UP);

        return savingsPercent.stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * Compare two offers to determine which gives better discount
     *
     * @param offer1 First offer
     * @param offer2 Second offer
     * @param amount Amount to calculate on
     * @return -1 if offer1 is better, 1 if offer2 is better, 0 if equal
     */
    public int compareOffers(Offer offer1, Offer offer2, BigDecimal amount) {
        BigDecimal discount1 = calculateDiscount(offer1, amount);
        BigDecimal discount2 = calculateDiscount(offer2, amount);

        return discount2.compareTo(discount1); // Higher discount is better
    }

    /**
     * Validate if discount is applicable
     *
     * @param offer The offer
     * @param amount The amount to apply discount on
     * @return true if discount can be applied, false otherwise
     */
    public boolean isDiscountApplicable(Offer offer, BigDecimal amount) {
        if (offer == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Check if offer is active
        if (!offer.getIsActive()) {
            return false;
        }

        // Check date range
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(offer.getStartAt()) || now.isAfter(offer.getEndAt())) {
            return false;
        }

        return true;
    }
}