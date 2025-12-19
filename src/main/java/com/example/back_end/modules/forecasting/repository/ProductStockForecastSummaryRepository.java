package com.example.back_end.modules.forecasting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductStockForecastSummaryRepository {

    private final JdbcTemplate jdbcTemplate;

    private static class ProductStockForecastSummaryRowMapper implements RowMapper<ProductStockForecastSummaryRow> {
        @Override
        public ProductStockForecastSummaryRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductStockForecastSummaryRow row = new ProductStockForecastSummaryRow();
            row.setProductId(rs.getLong("product_id"));
            row.setCurrentStock(rs.getBigDecimal("current_stock"));
            row.setAvgDailyDemand(rs.getBigDecimal("avg_daily_demand"));

            Date stockoutDate = rs.getDate("expected_stockout_date");
            row.setExpectedStockoutDate(stockoutDate != null ? stockoutDate.toLocalDate() : null);

            row.setRecommendedReorderQty(rs.getBigDecimal("recommended_reorder_qty"));

            Timestamp ts = rs.getTimestamp("generated_at");
            row.setGeneratedAt(ts != null ? ts.toLocalDateTime() : null);
            return row;
        }
    }

    public void upsertSummary(ProductStockForecastSummaryRow row) {
        String sql = """
                INSERT INTO public.product_stock_forecast_summary
                    (product_id, current_stock, avg_daily_demand,
                     expected_stockout_date, recommended_reorder_qty, generated_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (product_id)
                DO UPDATE SET
                    current_stock = EXCLUDED.current_stock,
                    avg_daily_demand = EXCLUDED.avg_daily_demand,
                    expected_stockout_date = EXCLUDED.expected_stockout_date,
                    recommended_reorder_qty = EXCLUDED.recommended_reorder_qty,
                    generated_at = now()
                """;

        jdbcTemplate.update(sql,
                row.getProductId(),
                row.getCurrentStock(),
                row.getAvgDailyDemand(),
                row.getExpectedStockoutDate(),
                row.getRecommendedReorderQty()
        );
    }

    public ProductStockForecastSummaryRow findByProductId(Long productId) {
        String sql = """
                SELECT product_id, current_stock, avg_daily_demand,
                       expected_stockout_date, recommended_reorder_qty, generated_at
                FROM public.product_stock_forecast_summary
                WHERE product_id = ?
                """;

        List<ProductStockForecastSummaryRow> list =
                jdbcTemplate.query(sql, new ProductStockForecastSummaryRowMapper(), productId);

        return list.isEmpty() ? null : list.get(0);
    }
}
