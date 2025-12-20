package com.example.back_end.modules.offer.entity;

import com.example.back_end.modules.catalog.product.entity.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "offer_bundles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "required_qty", precision = 10, scale = 2, nullable = false)
    private BigDecimal requiredQty = BigDecimal.ONE;
}