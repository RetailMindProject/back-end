package com.example.back_end.modules.stock.entity;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.example.back_end.modules.stock.enums.InventoryRefType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // product_id INT REFERENCES products(id)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 'WAREHOUSE' or 'STORE'
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20, nullable = false)
    private InventoryLocationType locationType;

    // 'PURCHASE','SALE','RETURN','TRANSFER','ADJUSTMENT'
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 20, nullable = false)
    private InventoryRefType refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "qty_change", precision = 10, scale = 2, nullable = false)
    private BigDecimal qtyChange;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "inventoryMovement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InventoryMovementBatch> batches = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (movedAt == null) {
            movedAt = Instant.now();
        }
    }
}
