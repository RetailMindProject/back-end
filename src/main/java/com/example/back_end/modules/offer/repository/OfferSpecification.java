package com.example.back_end.modules.offer.repository;

import com.example.back_end.modules.offer.entity.Offer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification class for advanced filtering of Offers
 * Usage in OfferService with findAll(Specification<Offer> spec, Pageable pageable)
 */
public class OfferSpecification {

    /**
     * Filter by offer type
     */
    public static Specification<Offer> hasOfferType(Offer.OfferType offerType) {
        return (root, query, criteriaBuilder) ->
                offerType == null ? null : criteriaBuilder.equal(root.get("offerType"), offerType);
    }

    /**
     * Filter by discount type
     */
    public static Specification<Offer> hasDiscountType(Offer.DiscountType discountType) {
        return (root, query, criteriaBuilder) ->
                discountType == null ? null : criteriaBuilder.equal(root.get("discountType"), discountType);
    }

    /**
     * Filter by active status
     */
    public static Specification<Offer> isActive(Boolean isActive) {
        return (root, query, criteriaBuilder) ->
                isActive == null ? null : criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    /**
     * Filter by date range (offers that are active between start and end date)
     */
    public static Specification<Offer> isActiveBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }

            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter offers that are currently active (now between start and end date)
     */
    public static Specification<Offer> isCurrentlyActive() {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime now = LocalDateTime.now();
            return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("isActive"), true),
                    criteriaBuilder.lessThanOrEqualTo(root.get("startAt"), now),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("endAt"), now)
            );
        };
    }

    /**
     * Search by title or code (case-insensitive)
     */
    public static Specification<Offer> searchByTitleOrCode(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return null;
            }

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), searchPattern)
            );
        };
    }

    /**
     * Filter by discount value range
     */
    public static Specification<Offer> hasDiscountValueBetween(Double minValue, Double maxValue) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minValue != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("discountValue"), minValue));
            }

            if (maxValue != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("discountValue"), maxValue));
            }

            return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by created date range
     */
    public static Specification<Offer> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter offers created by specific user
     */
    public static Specification<Offer> createdBy(Long userId) {
        return (root, query, criteriaBuilder) ->
                userId == null ? null : criteriaBuilder.equal(root.get("createdBy").get("id"), userId);
    }

    /**
     * Combine multiple specifications
     * Example usage:
     *
     * Specification<Offer> spec = OfferSpecification.combineFilters(
     *     OfferSpecification.isActive(true),
     *     OfferSpecification.hasOfferType(Offer.OfferType.PRODUCT),
     *     OfferSpecification.searchByTitleOrCode("summer")
     * );
     *
     * Page<Offer> offers = offerRepository.findAll(spec, pageable);
     */
    @SafeVarargs
    public static Specification<Offer> combineFilters(Specification<Offer>... specifications) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Specification<Offer> spec : specifications) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }

            return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}