package com.example.back_end.modules.offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of bundle offer detection and application
 * Contains information about which bundle was applied and discount distribution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleApplicationResult {

    /**
     * Whether a bundle offer was successfully applied
     */
    private Boolean bundleApplied;

    /**
     * ID of the applied bundle offer
     */
    private Long offerId;

    /**
     * Code of the applied bundle offer
     */
    private String offerCode;

    /**
     * Title of the applied bundle offer
     */
    private String offerTitle;

    /**
     * Total discount amount from the bundle
     */
    private BigDecimal totalDiscount;

    /**
     * Map of item discounts: itemId -> discount amount
     * Shows how the total bundle discount is distributed across bundle items
     */
    private Map<Long, BigDecimal> itemDiscounts;

    /**
     * Initialize with default values
     */
    public static BundleApplicationResult empty() {
        BundleApplicationResult result = new BundleApplicationResult();
        result.setBundleApplied(false);
        result.setTotalDiscount(BigDecimal.ZERO);
        result.setItemDiscounts(new HashMap<>());
        return result;
    }
}