package com.example.back_end.modules.offer.repository;

import com.example.back_end.modules.offer.entity.OfferBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferBundleRepository extends JpaRepository<OfferBundle, Long> {

    /**
     * Find all bundle items (products) for a specific offer
     * Used to check which products are required for a bundle
     *
     * @param offerId The offer ID
     * @return List of OfferBundle items with required products and quantities
     */
    @Query("SELECT ob FROM OfferBundle ob WHERE ob.offer.id = :offerId")
    List<OfferBundle> findByOfferId(@Param("offerId") Long offerId);

    /**
     * Delete all bundle items for a specific offer
     * Used when deleting or updating a bundle offer
     *
     * @param offerId The offer ID
     */
    @Modifying
    @Query("DELETE FROM OfferBundle ob WHERE ob.offer.id = :offerId")
    void deleteByOfferId(@Param("offerId") Long offerId);
}