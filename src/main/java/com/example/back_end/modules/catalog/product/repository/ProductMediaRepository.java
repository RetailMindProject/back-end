package com.example.back_end.modules.catalog.product.repository;

import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdOrderBySortOrderAsc(Long productId);

    Optional<ProductMedia> findByProductIdAndMediaId(Long productId, Long mediaId);

    Optional<ProductMedia> findByProductIdAndIsPrimaryTrue(Long productId);

    @Modifying
    @Query("DELETE FROM ProductMedia pm WHERE pm.product.id = :productId AND pm.media.id = :mediaId")
    void deleteByProductIdAndMediaId(@Param("productId") Long productId, @Param("mediaId") Long mediaId);

    @Modifying
    @Query("UPDATE ProductMedia pm SET pm.isPrimary = false WHERE pm.product.id = :productId")
    void clearPrimaryFlags(@Param("productId") Long productId);
}

