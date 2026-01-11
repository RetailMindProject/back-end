package com.example.back_end.modules.catalog.product.repository;

import com.example.back_end.modules.catalog.product.entity.ProductSearchTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSearchTermRepository extends JpaRepository<ProductSearchTerm, Long> {

    /**
     * Find all search terms for a product
     */
    List<ProductSearchTerm> findByProductIdOrderByTermAsc(Long productId);

    /**
     * Delete all search terms for a product
     */
    @Modifying
    @Query("DELETE FROM ProductSearchTerm pst WHERE pst.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    /**
     * Check if a term exists for a product (case-insensitive)
     */
    @Query("SELECT COUNT(pst) > 0 FROM ProductSearchTerm pst " +
           "WHERE pst.productId = :productId AND LOWER(pst.term) = LOWER(:term)")
    boolean existsByProductIdAndTermIgnoreCase(
        @Param("productId") Long productId,
        @Param("term") String term
    );
}

