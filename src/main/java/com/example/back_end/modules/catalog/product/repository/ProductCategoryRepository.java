package com.example.back_end.modules.catalog.product.repository;

import com.example.back_end.modules.catalog.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategory.ProductCategoryId> {
    
    
     
    @Query("SELECT pc FROM ProductCategory pc WHERE pc.productId = :productId")
    List<ProductCategory> findByProductId(@Param("productId") Long productId);
    
  
    @Query("SELECT COUNT(pc) FROM ProductCategory pc WHERE pc.categoryId = :categoryId")
    Long countProductsInCategory(@Param("categoryId") Long categoryId);
    
 
  
    void deleteByProductId(Long productId);
    
    
    @Query("SELECT COUNT(pc) > 0 FROM ProductCategory pc WHERE pc.productId = :productId AND pc.categoryId = :categoryId")
    boolean existsByProductIdAndCategoryId(@Param("productId") Long productId, @Param("categoryId") Long categoryId);
}