package com.example.back_end.modules.offer.repository;

import com.example.back_end.modules.offer.entity.OfferCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferCategoryRepository extends JpaRepository<OfferCategory, OfferCategory.OfferCategoryId> {

    @Modifying
    @Query("DELETE FROM OfferCategory oc WHERE oc.offer.id = :offerId")
    void deleteByOfferId(@Param("offerId") Long offerId);
}