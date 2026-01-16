package com.example.back_end.modules.catalog.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"id"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "product_search_terms",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_product_search_terms_product_term",
            columnNames = {"product_id", "term"}
        )
    },
    indexes = {
        @Index(name = "idx_product_search_terms_product_id", columnList = "product_id")
    }
)
public class ProductSearchTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "term", nullable = false, columnDefinition = "TEXT")
    private String term;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

