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

    @Query(value = """
           SELECT * FROM products p
           WHERE (:q IS NULL OR p.name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                           OR p.brand ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                           OR p.sku ILIKE CONCAT('%', CAST(:q AS TEXT), '%'))
             AND (:brand IS NULL OR p.brand ILIKE CAST(:brand AS TEXT))
             AND (:isActive IS NULL OR p.is_active = :isActive)
             AND (:minPrice IS NULL OR p.default_price >= :minPrice)
             AND (:maxPrice IS NULL OR p.default_price <= :maxPrice)
           ORDER BY p.created_at DESC
           """,
           countQuery = """
           SELECT COUNT(*) FROM products p
           WHERE (:q IS NULL OR p.name ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                           OR p.brand ILIKE CONCAT('%', CAST(:q AS TEXT), '%')
                           OR p.sku ILIKE CONCAT('%', CAST(:q AS TEXT), '%'))
             AND (:brand IS NULL OR p.brand ILIKE CAST(:brand AS TEXT))
             AND (:isActive IS NULL OR p.is_active = :isActive)
             AND (:minPrice IS NULL OR p.default_price >= :minPrice)
             AND (:maxPrice IS NULL OR p.default_price <= :maxPrice)
           """,
           nativeQuery = true)
    Page<Product> search(@Param("q") String q,
                         @Param("brand") String brand,
                         @Param("isActive") Boolean isActive,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);
}
