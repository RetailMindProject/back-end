package com.example.back_end.modules.stock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "inventory_movement_batches",
        indexes = {
                @Index(name = "idx_inventory_movement_batches_movement", columnList = "inventory_movement_id"),
                @Index(name = "idx_inventory_movement_batches_batch", columnList = "batch_id")
        }
)
public class InventoryMovementBatch {

    @EmbeddedId
    private InventoryMovementBatchId id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("batchId")
    @JoinColumn(name = "batch_id", nullable = false)
    private InventoryBatch batch;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("inventoryMovementId")
    @JoinColumn(name = "inventory_movement_id", nullable = false)
    private InventoryMovement inventoryMovement;

    @Column(name = "qty", precision = 10, scale = 2, nullable = false)
    private BigDecimal qty;
}

