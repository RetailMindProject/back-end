package com.example.back_end.modules.offer.repository;

import com.example.back_end.modules.offer.entity.OrderOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOfferRepository extends JpaRepository<OrderOffer, Long> {

    @Modifying
    @Query("DELETE FROM OrderOffer oo WHERE oo.offer.id = :offerId")
    void deleteByOfferId(@Param("offerId") Long offerId);
}