package com.example.back_end.modules.forecasting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ForecastingDataRepository {

    private final JdbcTemplate jdbcTemplate;

    private static class DailyProductSalesRowMapper implements RowMapper<DailyProductSalesRow> {
        @Override
        public DailyProductSalesRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            DailyProductSalesRow row = new DailyProductSalesRow();
            row.setSalesDate(rs.getObject("sales_date", LocalDate.class));
            row.setTotalQtySold(rs.getBigDecimal("total_qty_sold"));
            row.setPromoAnyFlag(rs.getInt("promo_any_flag"));
            if (rs.wasNull()) {
                row.setPromoAnyFlag(null);
            }
            row.setAvgDiscountPct(rs.getBigDecimal("avg_discount_pct"));
            return row;
        }
    }

    /**
     * جلب السلسلة الزمنية لمنتج معيّن من الـ View مع تضمين الأيام بدون مبيعات (Zero Demand)
     */
    public List<DailyProductSalesRow> findDailySalesForProduct(Long productId,
                                                               LocalDate fromDate,
                                                               LocalDate toDate) {
        String sql = """
            SELECT d::date as sales_date,
                   COALESCE(v.total_qty_sold, 0) as total_qty_sold,
                   COALESCE(v.promo_any_flag, 0) as promo_any_flag,
                   COALESCE(v.avg_discount_pct, 0) as avg_discount_pct
            FROM generate_series(?::date, ?::date, interval '1 day') AS d
            LEFT JOIN public.v_daily_product_sales_with_offers v
              ON v.product_id = ?
             AND v.sales_date = d::date
            ORDER BY d
            """;

        return jdbcTemplate.query(
                sql,
                new DailyProductSalesRowMapper(),
                fromDate,
                toDate,
                productId
        );
    }
}