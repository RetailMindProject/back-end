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

    /**
     * Typo-tolerant multilingual fuzzy search using PostgreSQL pg_trgm trigram similarity.
     * Returns active products only, ranked by similarity to query.
     *
     * Searches across:
     * - products.name (English/base name)
     * - products.sku (product code)
     * - product_search_terms.term (multilingual aliases e.g., Arabic "حليب" for "Milk")
     *
     * Uses:
     * - pg_trgm % operator (trigram match) for fast similarity search
     * - similarity() function for ranking
     * - ILIKE fallback for exact substring matches
     * - EXISTS subquery for search terms to avoid duplicate products
     *
     * @param query Search query (product name, SKU, or multilingual term)
     * @param minSimilarity Minimum similarity threshold (0.0-1.0), typically 0.25
     * @return List of Products matching query, sorted by relevance (deduplicated)
     */
    @Query(value = """
           WITH product_matches AS (
             SELECT DISTINCT ON (p.id)
               p.*,
               GREATEST(
                 -- Similarity to product name
                 similarity(p.name, :q),
                 -- Similarity to SKU
                 COALESCE(similarity(p.sku, :q), 0),
                 -- Maximum similarity to any search term for this product
                 COALESCE(
                   (SELECT MAX(similarity(pst.term, :q))
                    FROM public.product_search_terms pst
                    WHERE pst.product_id = p.id),
                   0
                 )
               ) AS max_similarity
             FROM public.products p
             WHERE p.is_active = true
               AND (
                 -- Match on product name
                 p.name % :q
                 OR similarity(p.name, :q) >= :minSim
                 OR p.name ILIKE '%' || :q || '%'
                 -- Match on SKU
                 OR (p.sku IS NOT NULL AND p.sku % :q)
                 OR (p.sku IS NOT NULL AND similarity(p.sku, :q) >= :minSim)
                 OR (p.sku IS NOT NULL AND p.sku ILIKE '%' || :q || '%')
                 -- Match on search terms (multilingual aliases)
                 OR EXISTS (
                   SELECT 1 FROM public.product_search_terms pst
                   WHERE pst.product_id = p.id
                     AND (
                       pst.term % :q
                       OR similarity(pst.term, :q) >= :minSim
                       OR pst.term ILIKE '%' || :q || '%'
                     )
                 )
               )
           )
           SELECT id, sku, name, brand, description, default_cost, default_price, 
                  tax_rate, is_active, created_at, updated_at, unit, wholesale_price
           FROM product_matches
           ORDER BY max_similarity DESC, name ASC
           """,
           nativeQuery = true)
    List<Product> fuzzySearch(@Param("q") String query, @Param("minSim") float minSimilarity);

}
