package com.example.back_end.modules.offer.entity;

import com.example.back_end.modules.catalog.product.entity.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "offer_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"offer", "product"})
@EqualsAndHashCode(exclude = {"offer", "product"})
public class OfferProduct {

    @EmbeddedId
    private OfferProductId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("offerId")
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferProductId implements java.io.Serializable {
        @Column(name = "offer_id")
        private Long offerId;

        @Column(name = "product_id")
        private Long productId;
    }
}