package com.example.back_end.modules.store_product.repository;

import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.mapper.StoreProductMapper.StockProjection;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, Long> {

    @Query(value = """
            SELECT 
              p.id              AS productId,
              p.sku             AS sku,
              p.name            AS name,
              s.store_qty       AS storeQty,
              s.warehouse_qty   AS warehouseQty,
              s.last_updated_at AS lastUpdatedAt,
              p.default_price   AS defaultPrice,
              p.default_cost    AS defaultCost
            FROM stock_snapshot s
            JOIN products p ON p.id = s.product_id
            WHERE (:q IS NULL 
                   OR p.name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                   OR p.sku  ILIKE CONCAT('%', CAST(:q AS TEXT), '%'))
              AND s.store_qty > 0
            ORDER BY p.name
            """,
            nativeQuery = true)
    List<StockProjection> searchStore(@Param("q") String q);
}
