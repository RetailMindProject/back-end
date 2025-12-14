package com.example.back_end.modules.store_product.repository;

import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.mapper.StoreProductMapper.StockProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, Long> {

    // Simple search by name or SKU
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
            ORDER BY p.name ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM stock_snapshot s
            JOIN products p ON p.id = s.product_id
            WHERE (:q IS NULL 
                   OR p.name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                   OR p.sku  ILIKE CONCAT('%', CAST(:q AS TEXT), '%'))
              AND s.store_qty > 0
            """,
            nativeQuery = true)
    Page<StockProjection> searchStore(@Param("q") String q, Pageable pageable);

    // Advanced filter with sorting
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
            WHERE (:brand IS NULL OR p.brand ILIKE CAST(:brand AS TEXT))
              AND (:isActive IS NULL OR p.is_active = :isActive)
              AND (:minPrice IS NULL OR p.default_price >= :minPrice)
              AND (:maxPrice IS NULL OR p.default_price <= :maxPrice)
              AND (:sku IS NULL OR p.sku ILIKE CONCAT('%', CAST(:sku AS TEXT), '%'))
              AND s.store_qty > 0
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM stock_snapshot s
            JOIN products p ON p.id = s.product_id
            WHERE (:brand IS NULL OR p.brand ILIKE CAST(:brand AS TEXT))
              AND (:isActive IS NULL OR p.is_active = :isActive)
              AND (:minPrice IS NULL OR p.default_price >= :minPrice)
              AND (:maxPrice IS NULL OR p.default_price <= :maxPrice)
              AND (:sku IS NULL OR p.sku ILIKE CONCAT('%', CAST(:sku AS TEXT), '%'))
              AND s.store_qty > 0
            """,
            nativeQuery = true)
    Page<StockProjection> filterStore(@Param("brand") String brand,
                                      @Param("isActive") Boolean isActive,
                                      @Param("minPrice") BigDecimal minPrice,
                                      @Param("maxPrice") BigDecimal maxPrice,
                                      @Param("sku") String sku,
                                      Pageable pageable);

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
