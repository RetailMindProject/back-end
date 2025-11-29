package com.example.back_end.modules.products.repository;

import com.example.back_end.modules.products.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    @Query("""
           SELECT p FROM Product p
           WHERE (:q IS NULL OR lower(p.name)  LIKE lower(concat('%', :q, '%'))
                           OR lower(p.brand) LIKE lower(concat('%', :q, '%'))
                           OR lower(p.sku)   LIKE lower(concat('%', :q, '%')))
             AND (:brand IS NULL OR lower(p.brand) = lower(:brand))
             AND (:isActive IS NULL OR p.isActive = :isActive)
             AND (:minPrice IS NULL OR p.defaultPrice >= :minPrice)
             AND (:maxPrice IS NULL OR p.defaultPrice <= :maxPrice)
           """)
    Page<Product> search(@Param("q") String q,
                         @Param("brand") String brand,
                         @Param("isActive") Boolean isActive,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);
}
