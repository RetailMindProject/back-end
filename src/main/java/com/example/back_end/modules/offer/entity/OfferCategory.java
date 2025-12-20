package com.example.back_end.modules.offer.entity;

import com.example.back_end.modules.catalog.category.entity.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "offer_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferCategory {

    @EmbeddedId
    private OfferCategoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("offerId")
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferCategoryId implements java.io.Serializable {
        @Column(name = "offer_id")
        private Long offerId;

        @Column(name = "category_id")
        private Long categoryId;
    }
}