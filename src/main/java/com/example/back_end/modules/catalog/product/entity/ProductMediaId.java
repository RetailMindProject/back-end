package com.example.back_end.modules.catalog.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductMediaId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "media_id")
    private Long mediaId;
}

