package com.example.back_end.modules.forecasting.repository;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductStockForecastSummaryViewRepository {

    private final JdbcTemplate jdbcTemplate;

    @Data
    public static class ProductStockForecastSummaryViewRow {
        private Long productId;
        private String sku;
        private String name;
        private String brand;
        private java.math.BigDecimal currentStock;
        private java.math.BigDecimal avgDailyDemand;
        private LocalDate expectedStockoutDate;
        private java.math.BigDecimal recommendedReorderQty;
    }

    private static class RowMapperImpl implements RowMapper<ProductStockForecastSummaryViewRow> {
        @Override
        public ProductStockForecastSummaryViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductStockForecastSummaryViewRow row = new ProductStockForecastSummaryViewRow();
            row.setProductId(rs.getLong("product_id"));
            row.setSku(rs.getString("sku"));
            row.setName(rs.getString("name"));
            row.setBrand(rs.getString("brand"));
            row.setCurrentStock(rs.getBigDecimal("current_stock"));
            row.setAvgDailyDemand(rs.getBigDecimal("avg_daily_demand"));

            Date stockoutDate = rs.getDate("expected_stockout_date");
            row.setExpectedStockoutDate(stockoutDate != null ? stockoutDate.toLocalDate() : null);

            row.setRecommendedReorderQty(rs.getBigDecimal("recommended_reorder_qty"));
            return row;
        }
    }

    public List<ProductStockForecastSummaryViewRow> findPage(
            boolean onlyAtRisk,
            LocalDate stockoutThreshold,
            boolean onlyWithReorder,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    s.product_id,
                    p.sku,
                    p.name,
                    p.brand,
                    s.current_stock,
                    s.avg_daily_demand,
                    s.expected_stockout_date,
                    s.recommended_reorder_qty
                FROM public.product_stock_forecast_summary s
                JOIN public.products p
                    ON p.id = s.product_id
                WHERE 1=1
                """
        );

        List<Object> args = new ArrayList<>();

        if (onlyAtRisk) {
            sql.append(" AND s.expected_stockout_date IS NOT NULL AND s.expected_stockout_date <= ? ");
            args.add(Date.valueOf(stockoutThreshold));
        }

        if (onlyWithReorder) {
            sql.append(" AND s.recommended_reorder_qty IS NOT NULL AND s.recommended_reorder_qty > 0 ");
        }

        sql.append(" ORDER BY s.expected_stockout_date NULLS LAST, p.name ASC ");
        sql.append(" LIMIT ? OFFSET ? ");

        args.add(limit);
        args.add(offset);

        return jdbcTemplate.query(sql.toString(), new RowMapperImpl(), args.toArray());
    }

    public long count(
            boolean onlyAtRisk,
            LocalDate stockoutThreshold,
            boolean onlyWithReorder
    ) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT COUNT(*)
                FROM public.product_stock_forecast_summary s
                JOIN public.products p
                    ON p.id = s.product_id
                WHERE 1=1
                """
        );

        List<Object> args = new ArrayList<>();

        if (onlyAtRisk) {
            sql.append(" AND s.expected_stockout_date IS NOT NULL AND s.expected_stockout_date <= ? ");
            args.add(Date.valueOf(stockoutThreshold));
        }

        if (onlyWithReorder) {
            sql.append(" AND s.recommended_reorder_qty IS NOT NULL AND s.recommended_reorder_qty > 0 ");
        }

        return jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    }
}
