package com.example.back_end.modules.recommendation.repository;

import com.example.back_end.modules.recommendation.dto.CustomerDTO;
import com.example.back_end.modules.recommendation.dto.ProductCandidateDTO;
import com.example.back_end.modules.recommendation.dto.ProductCatalogDTO;
import com.example.back_end.modules.recommendation.dto.ProductOfferEnrichmentDTO;
import com.example.back_end.modules.recommendation.dto.ProductWithOfferDTO;
import com.example.back_end.modules.recommendation.dto.PurchaseEventDTO;
import com.example.back_end.modules.recommendation.dto.TrendingProductDTO;
import com.example.back_end.modules.recommendation.dto.UserHistoryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RecommendationRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<UserHistoryItemDTO> USER_HISTORY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public UserHistoryItemDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UserHistoryItemDTO(
                    rs.getLong("product_id"),
                    rs.getObject("event_time", LocalDateTime.class)
            );
        }
    };

    private static final RowMapper<ProductCandidateDTO> PRODUCT_CANDIDATE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ProductCandidateDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProductCandidateDTO(
                    rs.getLong("product_id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getString("type"),
                    rs.getBigDecimal("current_price"),
                    rs.getBigDecimal("total_qty")
            );
        }
    };

    private static final RowMapper<TrendingProductDTO> TRENDING_ROW_MAPPER = new RowMapper<>() {
        @Override
        public TrendingProductDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TrendingProductDTO(
                    rs.getLong("product_id"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getBigDecimal("score")
            );
        }
    };

    private static final RowMapper<PurchaseEventDTO> PURCHASE_EVENT_ROW_MAPPER = new RowMapper<>() {
        @Override
        public PurchaseEventDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PurchaseEventDTO(
                    rs.getLong("user_id"),
                    rs.getLong("product_id"),
                    rs.getObject("event_time", LocalDateTime.class)
            );
        }
    };

    private static final RowMapper<ProductCatalogDTO> PRODUCT_CATALOG_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ProductCatalogDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProductCatalogDTO(
                    rs.getLong("product_id"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getString("type"),
                    rs.getBigDecimal("current_price"),
                    rs.getBigDecimal("total_qty")
            );
        }
    };

    private static final RowMapper<CustomerDTO> CUSTOMER_ROW_MAPPER = new RowMapper<>() {
        @Override
        public CustomerDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CustomerDTO(
                    rs.getLong("user_id"),
                    rs.getString("gender")
            );
        }
    };

    private static final RowMapper<ProductOfferEnrichmentDTO> PRODUCT_OFFER_ENRICHMENT_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ProductOfferEnrichmentDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            BigDecimal originalPrice = rs.getBigDecimal("current_price");
            String discountType = rs.getString("discount_type");
            BigDecimal discountValue = rs.getBigDecimal("discount_value");
            BigDecimal priceAfterDiscount = calculateDiscountedPrice(originalPrice, discountType, discountValue);
            Integer effectiveDiscountPercentage = calculateDiscountPercentage(discountType, discountValue, originalPrice);

            return ProductOfferEnrichmentDTO.builder()
                    .productId(rs.getLong("product_id"))
                    .name(rs.getString("product_name"))
                    .offerId(rs.getLong("offer_id"))
                    .offerTitle(rs.getString("offer_title"))
                    .offerCode(rs.getString("offer_code"))
                    .offerType(rs.getString("offer_type"))
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .originalPrice(originalPrice)
                    .priceAfterDiscount(priceAfterDiscount)
                    .effectiveDiscountPercentage(effectiveDiscountPercentage)
                    .build();
        }
    };

    private static final RowMapper<ProductWithOfferDTO> PRODUCT_WITH_OFFER_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ProductWithOfferDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            BigDecimal originalPrice = rs.getBigDecimal("current_price");
            String discountType = rs.getString("discount_type");
            BigDecimal discountValue = rs.getBigDecimal("discount_value");
            BigDecimal priceAfterDiscount = calculateDiscountedPrice(originalPrice, discountType, discountValue);
            Integer effectiveDiscountPercentage = calculateDiscountPercentage(discountType, discountValue, originalPrice);

            return new ProductWithOfferDTO(
                    rs.getLong("product_id"),
                    rs.getString("product_name"),
                    rs.getString("sku"),
                    originalPrice,
                    rs.getLong("offer_id"),
                    rs.getString("offer_code"),
                    rs.getString("offer_title"),
                    rs.getString("offer_type"),
                    discountType,
                    discountValue,
                    priceAfterDiscount,
                    effectiveDiscountPercentage
            );
        }
    };

    public List<UserHistoryItemDTO> findUserHistory(Long customerId, int limit) {
        String sql = """
            SELECT product_id, event_time
            FROM public.v_reco_user_history
            WHERE user_id = ?
            ORDER BY event_time DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, USER_HISTORY_ROW_MAPPER, customerId, limit);
    }

    public List<ProductCandidateDTO> findProductCandidates(int limit, boolean inStockOnly) {
        String sql = """
            SELECT product_id, sku, name, category_name, type, current_price, total_qty
            FROM public.v_reco_product_catalog
            WHERE (NOT ?) OR (total_qty > 0)
            ORDER BY product_id
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, PRODUCT_CANDIDATE_ROW_MAPPER, inStockOnly, limit);
    }

    public List<TrendingProductDTO> findTrending(int days, int limit) {
        String sql = """
            WITH t AS (
                SELECT product_id, SUM(quantity) AS score
                FROM public.v_reco_purchase_events
                WHERE event_time >= now() - (? * INTERVAL '1 day')
                GROUP BY product_id
            )
            SELECT t.product_id, c.name, c.category_name, t.score
            FROM t
            JOIN public.v_reco_product_catalog c ON c.product_id = t.product_id
            ORDER BY t.score DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, TRENDING_ROW_MAPPER, days, limit);
    }

    public List<PurchaseEventDTO> findPurchaseEventsForTraining(int limit, int offset) {
        String sql = """
            SELECT user_id, product_id, event_time
            FROM public.v_reco_purchase_events
            ORDER BY event_time ASC
            LIMIT ?
            OFFSET ?
            """;
        return jdbcTemplate.query(sql, PURCHASE_EVENT_ROW_MAPPER, limit, offset);
    }

    public List<ProductCatalogDTO> findProductCatalogForTraining(int limit, int offset) {
        String sql = """
            SELECT product_id, name, category_name, type, current_price, total_qty
            FROM public.v_reco_product_catalog
            ORDER BY product_id
            LIMIT ?
            OFFSET ?
            """;
        return jdbcTemplate.query(sql, PRODUCT_CATALOG_ROW_MAPPER, limit, offset);
    }

    public List<CustomerDTO> findCustomersForTraining(int limit, int offset) {
        String sql = """
            SELECT id AS user_id, gender
            FROM public.customers
            ORDER BY id
            LIMIT ?
            OFFSET ?
            """;
        return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER, limit, offset);
    }

    public List<ProductOfferEnrichmentDTO> findActiveOffersByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            WITH active_offers AS (
              SELECT DISTINCT ON (p.id)
                p.id AS product_id,
                p.name AS product_name,
                p.sku,
                p.default_price AS current_price,
                o.id AS offer_id,
                o.code AS offer_code,
                o.title AS offer_title,
                o.offer_type,
                o.discount_type,
                o.discount_value,
                CASE 
                  WHEN o.offer_type = 'PRODUCT' THEN 1
                  WHEN o.offer_type = 'CATEGORY' THEN 2
                  ELSE 99
                END AS offer_priority
              FROM products p
              LEFT JOIN offer_products op ON p.id = op.product_id
              LEFT JOIN offers o ON op.offer_id = o.id
                AND o.is_active = true
                AND o.start_at <= NOW()
                AND o.end_at >= NOW()
              WHERE p.id = ANY(?) AND p.is_active = true
              UNION ALL
              SELECT DISTINCT ON (p.id)
                p.id,
                p.name,
                p.sku,
                p.default_price,
                o.id,
                o.code,
                o.title,
                o.offer_type,
                o.discount_type,
                o.discount_value,
                CASE 
                  WHEN o.offer_type = 'CATEGORY' THEN 2
                  ELSE 99
                END
              FROM products p
              INNER JOIN product_categories pc ON p.id = pc.product_id
              INNER JOIN offer_categories oc ON pc.category_id = oc.category_id
              INNER JOIN offers o ON oc.offer_id = o.id
                AND o.is_active = true
                AND o.start_at <= NOW()
                AND o.end_at >= NOW()
              WHERE p.id = ANY(?) AND p.is_active = true
                AND NOT EXISTS (
                  SELECT 1 FROM offer_products op2
                  WHERE op2.product_id = p.id
                    AND EXISTS (
                      SELECT 1 FROM offers o2
                      WHERE o2.id = op2.offer_id
                        AND o2.is_active = true
                        AND o2.start_at <= NOW()
                        AND o2.end_at >= NOW()
                    )
                )
            )
            SELECT * FROM active_offers
            ORDER BY product_id, offer_priority
            """;

        Long[] ids = productIds.toArray(new Long[0]);
        return jdbcTemplate.query(sql, PRODUCT_OFFER_ENRICHMENT_ROW_MAPPER, ids, ids);
    }

    public List<ProductOfferEnrichmentDTO> findActiveOffersPaginated(int limit, int offset) {
        String sql = """
            WITH ranked_offers AS (
              SELECT 
                p.id AS product_id,
                p.name AS product_name,
                p.sku,
                p.default_price AS current_price,
                o.id AS offer_id,
                o.code AS offer_code,
                o.title AS offer_title,
                o.offer_type,
                o.discount_type,
                o.discount_value,
                ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY 
                  CASE WHEN o.offer_type = 'PRODUCT' THEN 1 ELSE 2 END) AS rn
              FROM products p
              LEFT JOIN offer_products op ON p.id = op.product_id
              LEFT JOIN offers o ON op.offer_id = o.id
                AND o.is_active = true
                AND o.start_at <= NOW()
                AND o.end_at >= NOW()
              WHERE p.is_active = true
              UNION ALL
              SELECT 
                p.id,
                p.name,
                p.sku,
                p.default_price,
                o.id,
                o.code,
                o.title,
                o.offer_type,
                o.discount_type,
                o.discount_value,
                ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY 2) AS rn
              FROM products p
              INNER JOIN product_categories pc ON p.id = pc.product_id
              INNER JOIN offer_categories oc ON pc.category_id = oc.category_id
              INNER JOIN offers o ON oc.offer_id = o.id
                AND o.is_active = true
                AND o.start_at <= NOW()
                AND o.end_at >= NOW()
              WHERE p.is_active = true
                AND NOT EXISTS (
                  SELECT 1 FROM offer_products op2
                  WHERE op2.product_id = p.id
                    AND EXISTS (
                      SELECT 1 FROM offers o2
                      WHERE o2.id = op2.offer_id
                        AND o2.is_active = true
                        AND o2.start_at <= NOW()
                        AND o2.end_at >= NOW()
                    )
                )
            )
            SELECT * FROM ranked_offers
            WHERE rn = 1
            ORDER BY product_id
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, PRODUCT_OFFER_ENRICHMENT_ROW_MAPPER, limit, offset);
    }

    private static BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, String discountType, BigDecimal discountValue) {
        if (originalPrice == null || discountValue == null) {
            return originalPrice;
        }

        BigDecimal discount;
        if ("PERCENTAGE".equals(discountType)) {
            discount = originalPrice.multiply(discountValue).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }

        BigDecimal result = originalPrice.subtract(discount);
        return result.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static Integer calculateDiscountPercentage(String discountType, BigDecimal discountValue, BigDecimal originalPrice) {
        if (discountValue == null || originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        if ("PERCENTAGE".equals(discountType)) {
            return discountValue.intValue();
        } else {
            BigDecimal percentage = discountValue.multiply(new BigDecimal("100")).divide(originalPrice, 2, java.math.RoundingMode.HALF_UP);
            return percentage.intValue();
        }
    }
}
