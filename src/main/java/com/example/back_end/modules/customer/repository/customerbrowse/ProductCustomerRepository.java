package com.example.back_end.modules.customer.repository.customerbrowse;

import com.example.back_end.modules.catalog.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductCustomerRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.isActive = true
              AND (:categoryId IS NULL OR EXISTS (
                    SELECT 1 FROM p.categories c WHERE c.id = :categoryId
              ))
              AND (:minPrice IS NULL OR p.defaultPrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.defaultPrice <= :maxPrice)
              AND (
                    :q IS NULL OR :q = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (
                    :offersOnly = FALSE
                    OR EXISTS (
                        SELECT 1
                        FROM Offer o
                        JOIN o.offerProducts op
                        WHERE op.product.id = p.id
                          AND o.offerType = com.example.back_end.modules.offer.entity.Offer.OfferType.PRODUCT
                          AND o.isActive = true
                          AND :now BETWEEN o.startAt AND o.endAt
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM Offer o2
                        JOIN o2.offerCategories oc
                        JOIN p.categories pc
                        WHERE oc.category.id = pc.id
                          AND o2.offerType = com.example.back_end.modules.offer.entity.Offer.OfferType.CATEGORY
                          AND o2.isActive = true
                          AND :now BETWEEN o2.startAt AND o2.endAt
                    )
              )
            """)
    Page<Product> searchActiveProducts(@Param("categoryId") Long categoryId,
                                      @Param("q") String q,
                                      @Param("minPrice") java.math.BigDecimal minPrice,
                                      @Param("maxPrice") java.math.BigDecimal maxPrice,
                                      @Param("offersOnly") boolean offersOnly,
                                      @Param("now") java.time.LocalDateTime now,
                                      Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isActive = true")
    Optional<Product> findActiveById(@Param("id") Long id);
}
