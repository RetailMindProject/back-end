package com.example.back_end.modules.offer.service;

import com.example.back_end.modules.offer.dto.OfferApplicationResult;
import com.example.back_end.modules.offer.entity.Offer;
import com.example.back_end.modules.offer.entity.OrderOffer;
import com.example.back_end.modules.offer.repository.OfferRepository;
import com.example.back_end.modules.offer.repository.OrderOfferRepository;
import com.example.back_end.modules.sales.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Engine for applying ORDER-level offers
 * Handles threshold-based discounts on entire order
 *
 * This is different from ProductOfferService:
 * - ProductOfferService: applies discounts to individual order items
 * - OfferEngine: applies discounts to entire order based on subtotal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferEngine {

    private final OrderOfferRepository orderOfferRepository;
    private final OfferCalculator offerCalculator; // 🔥 نستخدم الـ Calculator الموجود!

    /**
     * Find and apply the best ORDER offer to the order
     * This is called after calculating subtotal from items
     *
     * @param order The order to apply offer to
     * @param subtotal The subtotal after item-level discounts
     * @return OfferApplicationResult with applied offer info
     */
    public OfferApplicationResult applyOrderOffer(Order order, BigDecimal subtotal) {
        OfferApplicationResult result = new OfferApplicationResult();

        // Find best applicable order offer
        Optional<OrderOffer> bestOffer = findBestOrderOffer(subtotal);

        if (bestOffer.isPresent()) {
            OrderOffer orderOffer = bestOffer.get();
            Offer offer = orderOffer.getOffer();

            // 🔥 استخدم OfferCalculator لحساب الخصم
            BigDecimal discount = offerCalculator.calculateDiscount(offer, subtotal);

            // Apply to order
            order.setDiscountAmount(discount);

            // Build result
            result.setOfferApplied(true);
            result.setOfferId(offer.getId());
            result.setOfferCode(offer.getCode());
            result.setOfferTitle(offer.getTitle());
            result.setDiscountAmount(discount);
            result.setOriginalSubtotal(subtotal);
            result.setSubtotalAfterDiscount(subtotal.subtract(discount));

            log.info("Applied ORDER offer '{}' (ID: {}) to order {}: discount ${}",
                    offer.getTitle(), offer.getId(), order.getId(), discount);
        } else {
            // No offer applicable - clear any existing discount
            order.setDiscountAmount(BigDecimal.ZERO);

            result.setOfferApplied(false);
            result.setDiscountAmount(BigDecimal.ZERO);
            result.setOriginalSubtotal(subtotal);
            result.setSubtotalAfterDiscount(subtotal);

            log.debug("No applicable ORDER offer found for order {} (subtotal: ${})",
                    order.getId(), subtotal);
        }

        return result;
    }

    /**
     * Find the best ORDER offer applicable to the given subtotal
     * Returns the offer with highest discount
     */
    private Optional<OrderOffer> findBestOrderOffer(BigDecimal subtotal) {
        // Get all order offers
        List<OrderOffer> orderOffers = orderOfferRepository.findAll();

        OrderOffer bestOffer = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (OrderOffer orderOffer : orderOffers) {
            Offer offer = orderOffer.getOffer();

            // 🔥 استخدم OfferCalculator للتحقق
            if (!offerCalculator.isDiscountApplicable(offer, subtotal)) {
                continue;
            }

            // Check if subtotal meets minimum requirement
            if (orderOffer.getMinOrderAmount() != null) {
                if (!offerCalculator.meetsMinimumAmount(subtotal, orderOffer.getMinOrderAmount())) {
                    log.debug("Order offer {} not applicable: subtotal ${} < minimum ${}",
                            offer.getCode(), subtotal, orderOffer.getMinOrderAmount());
                    continue;
                }
            }

            // 🔥 استخدم OfferCalculator لحساب الخصم
            BigDecimal discount = offerCalculator.calculateDiscount(offer, subtotal);

            // Keep the best offer
            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                bestOffer = orderOffer;

                log.debug("Found better ORDER offer: {} with discount ${}",
                        offer.getCode(), discount);
            }
        }

        return Optional.ofNullable(bestOffer);
    }
}