package com.example.back_end.modules.store_product.repository;

import com.example.back_end.modules.store_product.entity.InventoryMovement;
import com.example.back_end.modules.stock.repository.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
}
