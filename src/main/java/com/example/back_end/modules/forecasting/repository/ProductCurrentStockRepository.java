package com.example.back_end.modules.forecasting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductCurrentStockRepository {

    private final JdbcTemplate jdbcTemplate;

    private static class ProductCurrentStockRowMapper implements RowMapper<ProductCurrentStockViewRow> {
        @Override
        public ProductCurrentStockViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductCurrentStockViewRow row = new ProductCurrentStockViewRow();
            row.setProductId(rs.getLong("product_id"));
            row.setSku(rs.getString("sku"));
            row.setName(rs.getString("name"));
            row.setBrand(rs.getString("brand"));
            row.setStoreQty(rs.getBigDecimal("store_qty"));
            row.setWarehouseQty(rs.getBigDecimal("warehouse_qty"));
            row.setTotalQty(rs.getBigDecimal("total_qty"));
            row.setLastUpdatedAt(
                    rs.getTimestamp("last_updated_at") != null
                            ? rs.getTimestamp("last_updated_at").toLocalDateTime()
                            : null
            );
            return row;
        }
    }

    public ProductCurrentStockViewRow findByProductId(Long productId) {
        String sql = """
                SELECT product_id, sku, name, brand,
                       store_qty, warehouse_qty, total_qty, last_updated_at
                FROM public.v_product_current_stock
                WHERE product_id = ?
                """;

        List<ProductCurrentStockViewRow> list =
                jdbcTemplate.query(sql, new ProductCurrentStockRowMapper(), productId);

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * إرجاع كل المنتجات من v_product_current_stock
     */
    public List<ProductCurrentStockViewRow> findAll() {
        String sql = """
                SELECT product_id, sku, name, brand,
                       store_qty, warehouse_qty, total_qty, last_updated_at
                FROM public.v_product_current_stock
                """;

        return jdbcTemplate.query(sql, new ProductCurrentStockRowMapper());
    }

    /**
     * إرجاع المنتجات التي مخزونها الكلي >= minTotalQty
     */
    public List<ProductCurrentStockViewRow> findAllWithMinTotalQty(BigDecimal minTotalQty) {
        String sql = """
                SELECT product_id, sku, name, brand,
                       store_qty, warehouse_qty, total_qty, last_updated_at
                FROM public.v_product_current_stock
                WHERE total_qty >= ?
                """;

        return jdbcTemplate.query(sql, new ProductCurrentStockRowMapper(), minTotalQty);
    }
}
