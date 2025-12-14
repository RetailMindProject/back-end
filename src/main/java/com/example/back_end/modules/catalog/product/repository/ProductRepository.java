package com.example.back_end.modules.catalog.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.back_end.modules.catalog.product.entity.Product;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    // Simple search by name or SKU
    @Query(value = """
           SELECT p.* FROM products p
           WHERE (:q IS NULL OR p.name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                           OR p.sku ILIKE CONCAT('%', CAST(:q AS TEXT), '%'))
           ORDER BY p.name ASC
           """,
           countQuery = """
           SELECT COUNT(*) FROM products p
           WHERE (:q IS NULL OR p.name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                           OR p.sku ILIKE CONCAT('%', CAST(:q AS TEXT), '%'))
           """,
           nativeQuery = true)
    Page<Product> search(@Param("q") String q, Pageable pageable);

    // Advanced filter with sorting
    @Query(value = """
           SELECT p.* FROM products p
           WHERE (:brand IS NULL OR p.brand ILIKE CAST(:brand AS TEXT))
             AND (:isActive IS NULL OR p.is_active = :isActive)
             AND (:minPrice IS NULL OR p.default_price >= :minPrice)
             AND (:maxPrice IS NULL OR p.default_price <= :maxPrice)
             AND (:sku IS NULL OR p.sku ILIKE CONCAT('%', CAST(:sku AS TEXT), '%'))
           """,
           countQuery = """
           SELECT COUNT(*) FROM products p
           WHERE (:brand IS NULL OR p.brand ILIKE CAST(:brand AS TEXT))
             AND (:isActive IS NULL OR p.is_active = :isActive)
             AND (:minPrice IS NULL OR p.default_price >= :minPrice)
             AND (:maxPrice IS NULL OR p.default_price <= :maxPrice)
             AND (:sku IS NULL OR p.sku ILIKE CONCAT('%', CAST(:sku AS TEXT), '%'))
           """,
           nativeQuery = true)
    Page<Product> filter(@Param("brand") String brand,
                         @Param("isActive") Boolean isActive,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("sku") String sku,
                         Pageable pageable);
}
