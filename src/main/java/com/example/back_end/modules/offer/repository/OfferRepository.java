package com.example.back_end.modules.offer.repository;

import com.example.back_end.modules.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

    Optional<Offer> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT o FROM Offer o WHERE o.isActive = true " +
            "AND o.startAt <= :currentDate AND o.endAt >= :currentDate")
    List<Offer> findActiveOffers(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT o FROM Offer o WHERE o.offerType = :offerType " +
            "AND o.isActive = true AND o.startAt <= :currentDate AND o.endAt >= :currentDate")
    List<Offer> findActiveOffersByType(
            @Param("offerType") Offer.OfferType offerType,
            @Param("currentDate") LocalDateTime currentDate
    );

    @Query("SELECT DISTINCT o FROM Offer o " +
            "LEFT JOIN FETCH o.offerProducts op " +
            "LEFT JOIN FETCH op.product " +
            "WHERE o.id = :id")
    Optional<Offer> findByIdWithProducts(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Offer o " +
            "LEFT JOIN FETCH o.offerCategories oc " +
            "LEFT JOIN FETCH oc.category " +
            "WHERE o.id = :id")
    Optional<Offer> findByIdWithCategories(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Offer o " +
            "LEFT JOIN FETCH o.offerBundles ob " +
            "LEFT JOIN FETCH ob.product " +
            "WHERE o.id = :id")
    Optional<Offer> findByIdWithBundles(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Offer o " +
            "LEFT JOIN FETCH o.orderOffer " +
            "WHERE o.id = :id")
    Optional<Offer> findByIdWithOrderOffer(@Param("id") Long id);

    /**
     * Find active PRODUCT offers for a specific product
     * Used for automatic offer application when adding products to order
     */
    @Query("SELECT DISTINCT o FROM Offer o " +
            "INNER JOIN o.offerProducts op " +
            "WHERE op.product.id = :productId " +
            "AND o.offerType = 'PRODUCT' " +
            "AND o.isActive = true " +
            "AND o.startAt <= :currentDate " +
            "AND o.endAt >= :currentDate")
    List<Offer> findActiveProductOffersForProduct(
            @Param("productId") Long productId,
            @Param("currentDate") LocalDateTime currentDate
    );


    @Query("""
        SELECT DISTINCT o FROM Offer o
        JOIN OfferCategory oc ON o.id = oc.offer.id
        JOIN ProductCategory pc ON oc.category.id = pc.category.id
        WHERE pc.product.id = :productId
          AND o.offerType = 'CATEGORY'
          AND o.isActive = true
          AND :now BETWEEN o.startAt AND o.endAt
        ORDER BY o.discountValue DESC
    """)
    List<Offer> findActiveCategoryOffersForProduct(
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now
    );

    /**
     * Find active BUNDLE offers
     */
    @Query("""
    SELECT DISTINCT o FROM Offer o
    WHERE o.offerType = 'BUNDLE'
      AND o.isActive = true
      AND :now BETWEEN o.startAt AND o.endAt
    ORDER BY o.discountValue DESC
""")
    List<Offer> findActiveBundleOffers(@Param("now") LocalDateTime now);

}