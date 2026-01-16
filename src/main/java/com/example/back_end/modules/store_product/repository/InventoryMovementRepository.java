package com.example.back_end.modules.store_product.repository;

import com.example.back_end.modules.stock.entity.InventoryMovement;
import com.example.back_end.modules.stock.repository.projection.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    // إجمالي الدخول خلال فترة (WAREHOUSE فقط)
    @Query(value = """
        SELECT COALESCE(SUM(
                   CASE 
                       WHEN qty_change > 0 THEN qty_change 
                       ELSE 0 
                   END
               ), 0)
        FROM inventory_movements
        WHERE moved_at >= :from
          AND location_type = 'WAREHOUSE'
        """, nativeQuery = true)
    BigDecimal sumInSince(@Param("from") LocalDateTime from);

    // إجمالي الخروج خلال فترة (WAREHOUSE فقط)
    @Query(value = """
        SELECT COALESCE(ABS(SUM(
                   CASE 
                       WHEN qty_change < 0 THEN qty_change 
                       ELSE 0 
                   END
               )), 0)
        FROM inventory_movements
        WHERE moved_at >= :from
          AND location_type = 'WAREHOUSE'
        """, nativeQuery = true)
    BigDecimal sumOutSince(@Param("from") LocalDateTime from);

    // عدد الحركات في WAREHOUSE فقط
    @Query(value = """
        SELECT COUNT(*)
        FROM inventory_movements
        WHERE moved_at >= :from
          AND location_type = 'WAREHOUSE'
        """, nativeQuery = true)
    Long countMovementsSince(@Param("from") LocalDateTime from);

    // حركات حديثة (لجدول Recent Movements)
    @Query(value = """
        SELECT 
            im.moved_at      AS movedAt,
            p.name           AS productName,
            COALESCE(c.name, '') AS categoryName,
            im.location_type AS locationType,
            im.ref_type      AS refType,
            im.qty_change    AS quantityChange
        FROM inventory_movements im
        LEFT JOIN products p 
            ON p.id = im.product_id
        LEFT JOIN product_categories pc
            ON pc.product_id = p.id
        LEFT JOIN categories c
            ON c.id = pc.category_id
        WHERE im.moved_at >= :from
        ORDER BY im.moved_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<InventoryRecentMovementProjection> findRecentMovements(
            @Param("from") LocalDateTime from,
            @Param("limit") int limit
    );

    // أكثر منتج حركة في آخر فترة (WAREHOUSE فقط)
    @Query(value = """
        SELECT product_id
        FROM inventory_movements
        WHERE product_id IS NOT NULL
          AND location_type = 'WAREHOUSE'
          AND moved_at >= :from
        GROUP BY product_id
        ORDER BY SUM(ABS(qty_change)) DESC
        LIMIT 1
        """, nativeQuery = true)
    Long findMostMovedProductIdSince(@Param("from") LocalDateTime from);

    // Weekly Movement Trend (per day) في المخزن فقط
    @Query(value = """
        SELECT 
            im.moved_at::date AS movementDate,
            COALESCE(SUM(CASE WHEN qty_change > 0 THEN qty_change ELSE 0 END), 0)  AS totalIn,
            COALESCE(ABS(SUM(CASE WHEN qty_change < 0 THEN qty_change ELSE 0 END)), 0) AS totalOut
        FROM inventory_movements im
        WHERE im.moved_at >= :from
          AND im.location_type = 'WAREHOUSE'
        GROUP BY im.moved_at::date
        ORDER BY im.moved_at::date
        """, nativeQuery = true)
    List<InventoryWeeklyMovementProjection> findWeeklyMovementSince(@Param("from") LocalDateTime from);

    @Query(value = """
    SELECT 
        c.name AS categoryName,
        COALESCE(SUM(CASE 
            WHEN im.qty_change > 0 THEN im.qty_change 
            ELSE 0 
        END), 0) AS totalIn,
        COALESCE(SUM(CASE 
            WHEN im.qty_change < 0 THEN ABS(im.qty_change) 
            ELSE 0 
        END), 0) AS totalOut
    FROM inventory_movements im
    JOIN products p 
        ON p.id = im.product_id
    JOIN product_categories pc
        ON pc.product_id = p.id
    JOIN categories c
        ON c.id = pc.category_id
    WHERE im.moved_at >= :from
      AND im.location_type = 'WAREHOUSE'
    GROUP BY c.name
    ORDER BY c.name
    """, nativeQuery = true)
    List<InventoryCategoryMovementProjection> findCategoryMovementLastWeek(
            @Param("from") LocalDateTime from
    );

    @Query(value = """
    SELECT 
        c.name AS categoryName,
        SUM(ABS(im.qty_change)) AS totalSalesQty
    FROM inventory_movements im
    JOIN products p 
        ON p.id = im.product_id
    JOIN product_categories pc
        ON pc.product_id = p.id
    JOIN categories c
        ON c.id = pc.category_id
    WHERE im.moved_at >= :from
      AND im.location_type = 'STORE'
      AND im.ref_type = 'SALE'
    GROUP BY c.name
    ORDER BY totalSalesQty DESC
    """, nativeQuery = true)
    List<InventoryCategorySalesProjection> findCategorySalesLastWeek(
            @Param("from") LocalDateTime from
    );

    @Query(value = """
    SELECT 
        p.id AS productId,
        p.name AS productName,
        COALESCE(c.name, '') AS categoryName,
        SUM(ABS(im.qty_change)) AS totalMovementQty
    FROM inventory_movements im
    JOIN products p 
        ON p.id = im.product_id
    LEFT JOIN product_categories pc
        ON pc.product_id = p.id
    LEFT JOIN categories c
        ON c.id = pc.category_id
    WHERE im.moved_at >= :from
      AND im.location_type = 'WAREHOUSE'
    GROUP BY p.id, p.name, c.name
    ORDER BY SUM(ABS(im.qty_change)) DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<InventoryTopProductProjection> findTopMovedProductsLastWeek(
            @Param("from") LocalDateTime from,
            @Param("limit") int limit
    );

    // Find all movements for a specific product
    List<InventoryMovement> findByProductId(Long productId);

    // Find all movements for a specific product with batches eagerly fetched (for calculating wasted/transferred quantities)
    @Query("SELECT DISTINCT m FROM InventoryMovement m " +
           "LEFT JOIN FETCH m.batches " +
           "WHERE m.product.id = :productId")
    List<InventoryMovement> findByProductIdWithBatches(@Param("productId") Long productId);

    // Get waste history with filters
    @Query(value = """
        SELECT 
            im.id as movementId,
            im.product_id as productId,
            p.name as productName,
            p.sku as sku,
            ib.id as batchId,
            ib.expiration_date as expirationDate,
            ABS(im.qty_change) as quantity,
            im.note as wasteReason,
            im.note as note,
            im.unit_cost as unitCost,
            ABS(im.qty_change) * COALESCE(im.unit_cost, 0) as totalCost,
            im.location_type as locationType,
            im.moved_at as wastedAt
        FROM inventory_movements im
        JOIN products p ON p.id = im.product_id
        LEFT JOIN inventory_movement_batches imb ON imb.inventory_movement_id = im.id
        LEFT JOIN inventory_batches ib ON ib.id = imb.batch_id
        WHERE im.ref_type = 'WASTED'
        AND (CAST(:productId AS BIGINT) IS NULL OR im.product_id = CAST(:productId AS BIGINT))
        AND (CAST(:batchId AS BIGINT) IS NULL OR ib.id = CAST(:batchId AS BIGINT))
        AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR im.moved_at >= CAST(:fromDate AS TIMESTAMPTZ))
        AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR im.moved_at <= CAST(:toDate AS TIMESTAMPTZ))
        AND (CAST(:wasteReason AS TEXT) IS NULL OR im.note ILIKE CONCAT('%', CAST(:wasteReason AS TEXT), '%'))
        ORDER BY im.moved_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM inventory_movements im
        LEFT JOIN inventory_movement_batches imb ON imb.inventory_movement_id = im.id
        LEFT JOIN inventory_batches ib ON ib.id = imb.batch_id
        WHERE im.ref_type = 'WASTED'
        AND (CAST(:productId AS BIGINT) IS NULL OR im.product_id = CAST(:productId AS BIGINT))
        AND (CAST(:batchId AS BIGINT) IS NULL OR ib.id = CAST(:batchId AS BIGINT))
        AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR im.moved_at >= CAST(:fromDate AS TIMESTAMPTZ))
        AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR im.moved_at <= CAST(:toDate AS TIMESTAMPTZ))
        AND (CAST(:wasteReason AS TEXT) IS NULL OR im.note ILIKE CONCAT('%', CAST(:wasteReason AS TEXT), '%'))
        """,
        nativeQuery = true)
    Page<WasteHistoryProjection> findWasteHistory(
        @Param("productId") Long productId,
        @Param("batchId") Long batchId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        @Param("wasteReason") String wasteReason,
        Pageable pageable
    );
}
