package com.example.back_end.modules.customer.repository.customerbrowse;

import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import com.example.back_end.modules.catalog.product.entity.ProductMediaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductMediaCustomerRepository extends JpaRepository<ProductMedia, ProductMediaId> {

    interface PrimaryImageRow {
        Long getProductId();

        String getUrl();
    }

    @Query(value = """
            SELECT DISTINCT ON (pm.product_id)
                pm.product_id AS productId,
                m.url         AS url
            FROM product_media pm
            JOIN media m ON m.id = pm.media_id
            WHERE pm.product_id IN (:productIds)
            ORDER BY pm.product_id,
                     pm.is_primary DESC,
                     pm.sort_order ASC
            """, nativeQuery = true)
    List<PrimaryImageRow> findPrimaryImages(@Param("productIds") Collection<Long> productIds);

    interface ProductImageRow {
        Long getId();

        String getUrl();

        String getMimeType();

        String getTitle();

        String getAltText();

        Boolean getIsPrimary();

        Integer getSortOrder();
    }

    @Query(value = """
            SELECT
                m.id AS id,
                m.url AS url,
                m.mime_type AS mimeType,
                m.title AS title,
                m.alt_text AS altText,
                pm.is_primary AS isPrimary,
                pm.sort_order AS sortOrder
            FROM product_media pm
            JOIN media m ON m.id = pm.media_id
            WHERE pm.product_id = :productId
            ORDER BY pm.is_primary DESC, pm.sort_order ASC
            """, nativeQuery = true)
    List<ProductImageRow> findImagesForProduct(@Param("productId") Long productId);
}

