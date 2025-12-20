package com.example.back_end.modules.catalog.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.back_end.modules.catalog.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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


                         Optional<Product> findBySku(String sku);
    
    /**
     * Find all active products (for POS catalog)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.name")
    List<Product> findAllActiveProducts();
    
    /**
     * Find products by category (most important for POS!)
     */
    @Query(value = """
           SELECT p.* FROM products p
           JOIN product_categories pc ON p.id = pc.product_id
           WHERE pc.category_id = :categoryId 
             AND p.is_active = true
           ORDER BY p.name
           """, nativeQuery = true)
    List<Product> findProductsByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * Find products by category with pagination
     */
    @Query(value = """
           SELECT p.* FROM products p
           JOIN product_categories pc ON p.id = pc.product_id
           WHERE pc.category_id = :categoryId 
             AND p.is_active = true
           ORDER BY p.name
           """,
           countQuery = """
           SELECT COUNT(*) FROM products p
           JOIN product_categories pc ON p.id = pc.product_id
           WHERE pc.category_id = :categoryId 
             AND p.is_active = true
           """,
           nativeQuery = true)
    Page<Product> findProductsByCategoryIdPaginated(@Param("categoryId") Long categoryId, Pageable pageable);
    
    /**
     * Simple search for POS (no filters, just name/sku)
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.name")
    List<Product> quickSearch(@Param("search") String search);

}
