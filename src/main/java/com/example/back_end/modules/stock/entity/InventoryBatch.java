package com.example.back_end.modules.stock.entity;

import com.example.back_end.modules.catalog.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "inventory_batches",
        indexes = {
                @Index(name = "idx_inventory_batches_product_id", columnList = "product_id"),
                @Index(name = "idx_inventory_batches_expiration_date", columnList = "expiration_date"),
                @Index(name = "idx_inventory_batches_product_exp", columnList = "product_id,expiration_date")
        }
)
public class InventoryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InventoryMovementBatch> movementBatches = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

