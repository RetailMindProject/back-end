package com.example.back_end.modules.offer.repository;

import com.example.back_end.modules.offer.entity.OfferProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferProductRepository extends JpaRepository<OfferProduct, OfferProduct.OfferProductId> {

    @Modifying
    @Query("DELETE FROM OfferProduct op WHERE op.offer.id = :offerId")
    void deleteByOfferId(@Param("offerId") Long offerId);

    @Query("SELECT op FROM OfferProduct op " +
           "LEFT JOIN FETCH op.product " +
           "WHERE op.offer.id = :offerId")
    java.util.List<OfferProduct> findByOfferId(@Param("offerId") Long offerId);
}