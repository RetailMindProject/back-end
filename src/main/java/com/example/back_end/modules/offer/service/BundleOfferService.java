package com.example.back_end.modules.offer.service;
import com.example.back_end.modules.offer.dto.BundleApplicationResult;
import com.example.back_end.modules.offer.entity.Offer;
import com.example.back_end.modules.offer.entity.OfferBundle;
import com.example.back_end.modules.offer.repository.OfferBundleRepository;
import com.example.back_end.modules.offer.repository.OfferRepository;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting and applying BUNDLE offers
 * Bundle offers have HIGHEST priority - they lock products from other item-level offers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BundleOfferService {

    private final OfferRepository offerRepository;
    private final OfferBundleRepository offerBundleRepository;
    private final OfferCalculator offerCalculator;

    /**
     * Detect and apply bundle offers to order items
     *
     * This method:
     * 1. Finds all active bundle offers
     * 2. Checks which bundles are satisfied by current order items
     * 3. Applies the best bundle offer
     * 4. Distributes discount across bundle items
     *
     * @param orderItems All items in the order
     * @return BundleApplicationResult with applied bundle info
     */
    @Transactional(readOnly = true)
    public BundleApplicationResult detectAndApplyBundles(List<OrderItem> orderItems) {
        BundleApplicationResult result = BundleApplicationResult.empty();

        if (orderItems == null || orderItems.isEmpty()) {
            result.setBundleApplied(false);
            return result;
        }

        LocalDateTime now = LocalDateTime.now();

        // Get all active bundle offers
        List<Offer> activeBundleOffers = offerRepository.findActiveBundleOffers(now);

        if (activeBundleOffers.isEmpty()) {
            log.debug("No active BUNDLE offers found");
            result.setBundleApplied(false);
            return result;
        }

        // Find the best applicable bundle
        Offer bestBundle = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        Map<Long, BigDecimal> bestDistribution = null;

        for (Offer bundleOffer : activeBundleOffers) {
            // Check if this bundle is satisfied
            BundleCheckResult checkResult = isBundleSatisfied(bundleOffer, orderItems);

            if (checkResult.isSatisfied()) {
                // Calculate discount
                BigDecimal bundleTotal = checkResult.getBundleTotal();
                BigDecimal discount = offerCalculator.calculateDiscount(bundleOffer, bundleTotal);

                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    bestBundle = bundleOffer;
                    bestDistribution = distributeDiscount(
                            checkResult.getBundleItems(),
                            discount,
                            bundleTotal
                    );

                    log.debug("Found better BUNDLE offer: {} with discount ${}",
                            bundleOffer.getCode(), discount);
                }
            }
        }

        if (bestBundle != null) {
            // Apply bundle
            result.setBundleApplied(true);
            result.setOfferId(bestBundle.getId());
            result.setOfferCode(bestBundle.getCode());
            result.setOfferTitle(bestBundle.getTitle());
            result.setTotalDiscount(bestDiscount);
            result.setItemDiscounts(bestDistribution);

            log.info("Applied BUNDLE offer '{}' (ID: {}): total discount ${}",
                    bestBundle.getTitle(), bestBundle.getId(), bestDiscount);
        } else {
            result.setBundleApplied(false);
        }

        return result;
    }

    /**
     * Check if a bundle offer is satisfied by current order items
     */
    private BundleCheckResult isBundleSatisfied(Offer bundleOffer, List<OrderItem> orderItems) {
        BundleCheckResult result = BundleCheckResult.unsatisfied();

        // Get required products for this bundle
        List<OfferBundle> requiredItems = offerBundleRepository.findByOfferId(bundleOffer.getId());

        if (requiredItems.isEmpty()) {
            result.setSatisfied(false);
            return result;
        }

        // Create a map of available quantities in order
        Map<Long, BigDecimal> availableQuantities = orderItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getProduct().getId(),
                        OrderItem::getQuantity,
                        BigDecimal::add
                ));

        // Check if all required products are present with sufficient quantities
        Map<Long, OrderItem> bundleItemsMap = new HashMap<>();
        BigDecimal bundleTotal = BigDecimal.ZERO;

        for (OfferBundle required : requiredItems) {
            Long productId = required.getProduct().getId();
            BigDecimal requiredQty = required.getRequiredQty();

            // Find this product in order items
            Optional<OrderItem> orderItemOpt = orderItems.stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst();

            if (orderItemOpt.isEmpty()) {
                // Required product not in order
                result.setSatisfied(false);
                return result;
            }

            OrderItem orderItem = orderItemOpt.get();
            BigDecimal availableQty = orderItem.getQuantity();

            if (availableQty.compareTo(requiredQty) < 0) {
                // Insufficient quantity
                log.debug("Bundle not satisfied: product {} has qty {} but requires {}",
                        productId, availableQty, requiredQty);
                result.setSatisfied(false);
                return result;
            }

            // Calculate this item's contribution to bundle total
            // (use required quantity, not all available)
            BigDecimal itemTotal = orderItem.getUnitPrice().multiply(requiredQty);
            bundleTotal = bundleTotal.add(itemTotal);

            bundleItemsMap.put(orderItem.getId(), orderItem);
        }

        // Bundle is satisfied!
        result.setSatisfied(true);
        result.setBundleItems(bundleItemsMap);
        result.setBundleTotal(bundleTotal);

        log.debug("BUNDLE satisfied: offer={}, total=${}, items={}",
                bundleOffer.getCode(), bundleTotal, bundleItemsMap.size());

        return result;
    }

    /**
     * Distribute bundle discount across bundle items proportionally
     */
    private Map<Long, BigDecimal> distributeDiscount(
            Map<Long, OrderItem> bundleItems,
            BigDecimal totalDiscount,
            BigDecimal bundleTotal) {

        Map<Long, BigDecimal> distribution = new HashMap<>();

        for (Map.Entry<Long, OrderItem> entry : bundleItems.entrySet()) {
            Long itemId = entry.getKey();
            OrderItem item = entry.getValue();

            // Calculate item's line total (before discount)
            BigDecimal itemLineTotal = item.getUnitPrice().multiply(item.getQuantity());

            // Proportional discount for this item
            BigDecimal itemDiscount = totalDiscount
                    .multiply(itemLineTotal)
                    .divide(bundleTotal, 2, RoundingMode.HALF_UP);

            distribution.put(itemId, itemDiscount);

            log.debug("Bundle item {}: lineTotal=${}, discount=${}",
                    itemId, itemLineTotal, itemDiscount);
        }

        return distribution;
    }



}