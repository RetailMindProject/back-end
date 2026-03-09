package com.example.back_end.modules.customer.service.customerbrowse;

import com.example.back_end.modules.customer.dto.customerbrowse.OfferMiniDTO;
import com.example.back_end.modules.customer.repository.customerbrowse.OfferCustomerRepository;
import com.example.back_end.modules.offer.entity.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OfferResolverService {

    private final OfferCustomerRepository offerCustomerRepository;

    /**
     * Resolves best offer per product.
     * Rules:
     * - PRODUCT offers override CATEGORY offers
     * - Higher discount_value wins
     * - tie-breaker: earliest end_at
     */
    public Map<Long, OfferMiniDTO> resolveBestOffers(Collection<Long> productIds, LocalDateTime now) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OfferCustomerRepository.ProductOfferRow> productOffers = offerCustomerRepository
                .findActiveProductOffersForProducts(productIds, now);
        List<OfferCustomerRepository.CategoryOfferRow> categoryOffers = offerCustomerRepository
                .findActiveCategoryOffersForProducts(productIds, now);

        Map<Long, BestOfferCandidate> bestProductOffer = pickBest(productOffers);
        Map<Long, BestOfferCandidate> bestCategoryOffer = pickBest(categoryOffers);

        Map<Long, OfferMiniDTO> result = new HashMap<>();
        for (Long productId : productIds) {
            BestOfferCandidate chosen = bestProductOffer.get(productId);
            if (chosen == null) {
                chosen = bestCategoryOffer.get(productId);
            }
            if (chosen != null) {
                result.put(productId, OfferMiniDTO.builder()
                        .id(chosen.offerId)
                        .title(chosen.title)
                        .discountType(Offer.DiscountType.valueOf(chosen.discountType))
                        .discountValue(chosen.discountValue)
                        .build());
            }
        }
        return result;
    }

    private static Map<Long, BestOfferCandidate> pickBest(List<? extends OfferRow> rows) {
        Map<Long, BestOfferCandidate> best = new HashMap<>();
        for (OfferRow r : rows) {
            BestOfferCandidate candidate = new BestOfferCandidate(
                    r.getProductId(),
                    r.getOfferId(),
                    r.getTitle(),
                    r.getDiscountType(),
                    r.getDiscountValue(),
                    r.getEndAt()
            );
            BestOfferCandidate current = best.get(r.getProductId());
            if (current == null || candidate.isBetterThan(current)) {
                best.put(r.getProductId(), candidate);
            }
        }
        return best;
    }

    /**
     * Common shape for offer projection rows (PRODUCT and CATEGORY offers).
     */
    public interface OfferRow {
        Long getProductId();

        Long getOfferId();

        String getTitle();

        String getDiscountType();

        BigDecimal getDiscountValue();

        LocalDateTime getEndAt();
    }

    private record BestOfferCandidate(
            Long productId,
            Long offerId,
            String title,
            String discountType,
            BigDecimal discountValue,
            LocalDateTime endAt
    ) {
        boolean isBetterThan(BestOfferCandidate other) {
            int cmp = this.discountValue.compareTo(other.discountValue);
            if (cmp != 0) return cmp > 0;
            return this.endAt.isBefore(other.endAt);
        }
    }
}
