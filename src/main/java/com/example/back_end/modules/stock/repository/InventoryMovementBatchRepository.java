package com.example.back_end.modules.stock.repository;

import com.example.back_end.modules.stock.entity.InventoryMovementBatch;
import com.example.back_end.modules.stock.entity.InventoryMovementBatchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryMovementBatchRepository extends JpaRepository<InventoryMovementBatch, InventoryMovementBatchId> {

    List<InventoryMovementBatch> findByBatch_Id(Long batchId);

    List<InventoryMovementBatch> findByInventoryMovement_Id(Long inventoryMovementId);

    // Calculate total wasted quantity for a batch (direct SQL query to avoid lazy loading issues)
    @Query(value = """
        SELECT COALESCE(SUM(imb.qty), 0)
        FROM inventory_movement_batches imb
        JOIN inventory_movements im ON im.id = imb.inventory_movement_id
        WHERE imb.batch_id = :batchId
        AND im.ref_type = 'WASTED'
        """, nativeQuery = true)
    BigDecimal sumWastedQuantityByBatchId(@Param("batchId") Long batchId);

    // Calculate total transferred out quantity for a batch (direct SQL query to avoid lazy loading issues)
    @Query(value = """
        SELECT COALESCE(SUM(imb.qty), 0)
        FROM inventory_movement_batches imb
        JOIN inventory_movements im ON im.id = imb.inventory_movement_id
        WHERE imb.batch_id = :batchId
        AND im.ref_type = 'TRANSFER'
        AND im.location_type = 'WAREHOUSE'
        AND im.qty_change < 0
        """, nativeQuery = true)
    BigDecimal sumTransferredOutQuantityByBatchId(@Param("batchId") Long batchId);

    // Calculate total purchased/added quantity for a batch (only positive movements like PURCHASE)
    @Query(value = """
        SELECT COALESCE(SUM(imb.qty), 0)
        FROM inventory_movement_batches imb
        JOIN inventory_movements im ON im.id = imb.inventory_movement_id
        WHERE imb.batch_id = :batchId
        AND (im.ref_type = 'PURCHASE' OR (im.ref_type = 'TRANSFER' AND im.location_type = 'WAREHOUSE' AND im.qty_change > 0))
        """, nativeQuery = true)
    BigDecimal sumPurchasedQuantityByBatchId(@Param("batchId") Long batchId);
}

