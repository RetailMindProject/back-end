package com.example.back_end.modules.stock.entity;

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
public class InventoryMovementBatchId implements Serializable {

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "inventory_movement_id")
    private Long inventoryMovementId;
}

