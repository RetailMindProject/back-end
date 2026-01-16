package com.example.back_end.modules.offer.service;

import com.example.back_end.modules.sales.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal result of bundle satisfaction check
 * Used by BundleOfferService to determine if order items satisfy bundle requirements
 *
 * Package-private - not exposed outside the service layer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BundleCheckResult {

    /**
     * Whether the bundle requirements are satisfied
     */
    private boolean satisfied;

    /**
     * Map of order items that are part of the bundle: itemId -> OrderItem
     */
    private Map<Long, OrderItem> bundleItems;

    /**
     * Total value of bundle items (before discount)
     */
    private BigDecimal bundleTotal;

    /**
     * Create an unsatisfied result
     */
    public static BundleCheckResult unsatisfied() {
        BundleCheckResult result = new BundleCheckResult();
        result.setSatisfied(false);
        result.setBundleItems(new HashMap<>());
        result.setBundleTotal(BigDecimal.ZERO);
        return result;
    }
}