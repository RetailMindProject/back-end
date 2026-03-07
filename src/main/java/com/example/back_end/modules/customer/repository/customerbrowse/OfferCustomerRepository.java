package com.example.back_end.modules.customer.repository.customerbrowse;

import com.example.back_end.modules.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OfferCustomerRepository extends JpaRepository<Offer, Long> {

    interface ProductOfferRow extends com.example.back_end.modules.customer.service.customerbrowse.OfferResolverService.OfferRow {
        Long getProductId();

        Long getOfferId();

        String getTitle();

        String getDiscountType();

        java.math.BigDecimal getDiscountValue();

        LocalDateTime getEndAt();
    }

    @Query(value = """
            SELECT
                op.product_id AS productId,
                o.id AS offerId,
                o.title AS title,
                o.discount_type AS discountType,
                o.discount_value AS discountValue,
                o.end_at AS endAt
            FROM offer_products op
            JOIN offers o ON o.id = op.offer_id
            WHERE op.product_id IN (:productIds)
              AND o.is_active = true
              AND :now BETWEEN o.start_at AND o.end_at
              AND o.offer_type = 'PRODUCT'
            """, nativeQuery = true)
    List<ProductOfferRow> findActiveProductOffersForProducts(@Param("productIds") Collection<Long> productIds,
                                                           @Param("now") LocalDateTime now);

    interface CategoryOfferRow extends com.example.back_end.modules.customer.service.customerbrowse.OfferResolverService.OfferRow {
        Long getProductId();

        Long getOfferId();

        String getTitle();

        String getDiscountType();

        java.math.BigDecimal getDiscountValue();

        LocalDateTime getEndAt();
    }

    @Query(value = """
            SELECT
                pc.product_id AS productId,
                o.id AS offerId,
                o.title AS title,
                o.discount_type AS discountType,
                o.discount_value AS discountValue,
                o.end_at AS endAt
            FROM product_categories pc
            JOIN offer_categories oc ON oc.category_id = pc.category_id
            JOIN offers o ON o.id = oc.offer_id
            WHERE pc.product_id IN (:productIds)
              AND o.is_active = true
              AND :now BETWEEN o.start_at AND o.end_at
              AND o.offer_type = 'CATEGORY'
            """, nativeQuery = true)
    List<CategoryOfferRow> findActiveCategoryOffersForProducts(@Param("productIds") Collection<Long> productIds,
                                                             @Param("now") LocalDateTime now);

    interface OfferDetailsRow {
        Long getId();

        String getTitle();

        String getDiscountType();

        java.math.BigDecimal getDiscountValue();

        String getOfferType();

        LocalDateTime getStartAt();

        LocalDateTime getEndAt();
    }

    @Query(value = """
            SELECT
                o.id AS id,
                o.title AS title,
                o.discount_type AS discountType,
                o.discount_value AS discountValue,
                o.offer_type AS offerType,
                o.start_at AS startAt,
                o.end_at AS endAt
            FROM offers o
            WHERE o.is_active = true
              AND :now BETWEEN o.start_at AND o.end_at
              AND o.offer_type IN ('PRODUCT','CATEGORY')
              AND (
                   (o.offer_type = 'PRODUCT' AND EXISTS (
                        SELECT 1 FROM offer_products op WHERE op.offer_id = o.id AND op.product_id = :productId
                   ))
                OR (o.offer_type = 'CATEGORY' AND EXISTS (
                        SELECT 1
                        FROM offer_categories oc
                        JOIN product_categories pc ON pc.category_id = oc.category_id
                        WHERE oc.offer_id = o.id AND pc.product_id = :productId
                   ))
              )
            ORDER BY o.offer_type DESC, o.discount_value DESC, o.end_at ASC
            """, nativeQuery = true)
    List<OfferDetailsRow> findActiveOffersForProduct(@Param("productId") Long productId,
                                                   @Param("now") LocalDateTime now);
}
