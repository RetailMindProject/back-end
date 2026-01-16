package com.example.back_end.modules.offer.service;

import com.example.back_end.modules.offer.entity.Offer;
import com.example.back_end.modules.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service for applying CATEGORY offers to order items
 * Applied when no PRODUCT offer is available (lower priority)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryOfferService {

    private final OfferRepository offerRepository;
    private final OfferCalculator offerCalculator;

    /**
     * Find and return the best active CATEGORY offer for a product
     *
     * This checks if the product belongs to a category with active offers
     *
     * @param productId The product ID
     * @param unitPrice The unit price of the product
     * @param quantity The quantity being ordered
     * @return Optional containing the best offer, or empty if no offers found
     */
    @Transactional(readOnly = true)
    public Optional<Offer> findBestCategoryOffer(Long productId, BigDecimal unitPrice, BigDecimal quantity) {
        LocalDateTime now = LocalDateTime.now();

        // Find all active CATEGORY offers for categories this product belongs to
        List<Offer> activeOffers = offerRepository.findActiveCategoryOffersForProduct(productId, now);

        if (activeOffers.isEmpty()) {
            log.debug("No active CATEGORY offers found for product ID: {}", productId);
            return Optional.empty();
        }

        // Calculate total line amount before discount
        BigDecimal lineAmount = unitPrice.multiply(quantity);

        // Find the offer with the highest discount value
        Optional<Offer> bestOffer = activeOffers.stream()
                .max(Comparator.comparing(offer -> {
                    BigDecimal discount = offerCalculator.calculateDiscount(offer, lineAmount);
                    return discount;
                }));

        if (bestOffer.isPresent()) {
            BigDecimal discount = offerCalculator.calculateDiscount(bestOffer.get(), lineAmount);
            log.info("Found best CATEGORY offer for product {}: offer ID={}, discount={}",
                    productId, bestOffer.get().getId(), discount);
        }

        return bestOffer;
    }

    /**
     * Calculate discount amount for a category offer
     *
     * @param offer The offer to apply
     * @param unitPrice The unit price
     * @param quantity The quantity
     * @return The discount amount
     */
    public BigDecimal calculateCategoryOfferDiscount(Offer offer, BigDecimal unitPrice, BigDecimal quantity) {
        if (offer == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal lineAmount = unitPrice.multiply(quantity);
        return offerCalculator.calculateDiscount(offer, lineAmount);
    }
}