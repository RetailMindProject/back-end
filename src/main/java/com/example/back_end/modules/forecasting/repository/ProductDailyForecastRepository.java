package com.example.back_end.modules.forecasting.repository;

import com.example.back_end.modules.forecasting.dto.ForecastPointDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductDailyForecastRepository {

    private final JdbcTemplate jdbcTemplate;

    // 🔹 Mapper لتحويل صفوف الجدول إلى ForecastPointDTO
    private static class ForecastPointRowMapper implements RowMapper<ForecastPointDTO> {
        @Override
        public ForecastPointDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            ForecastPointDTO dto = new ForecastPointDTO();
            dto.setDs(rs.getObject("forecast_date", LocalDate.class));
            dto.setYhat(rs.getDouble("yhat"));

            double lower = rs.getDouble("yhat_lower");
            if (rs.wasNull()) {
                dto.setYhatLower(null);
            } else {
                dto.setYhatLower(lower);
            }

            double upper = rs.getDouble("yhat_upper");
            if (rs.wasNull()) {
                dto.setYhatUpper(null);
            } else {
                dto.setYhatUpper(upper);
            }

            return dto;
        }
    }

    /**
     * تخزين / تحديث نتائج التنبؤ لمنتج معيّن
     */
    public void upsertForecast(Long productId, List<ForecastPointDTO> points) {
        String sql = """
                INSERT INTO public.product_daily_forecast
                    (product_id, forecast_date, yhat, yhat_lower, yhat_upper, generated_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (product_id, forecast_date)
                DO UPDATE SET
                    yhat = EXCLUDED.yhat,
                    yhat_lower = EXCLUDED.yhat_lower,
                    yhat_upper = EXCLUDED.yhat_upper,
                    generated_at = now()
                """;

        jdbcTemplate.batchUpdate(sql, points, points.size(), (ps, point) -> {
            ps.setLong(1, productId);
            ps.setDate(2, Date.valueOf(point.getDs()));
            ps.setDouble(3, point.getYhat());
            if (point.getYhatLower() != null) {
                ps.setDouble(4, point.getYhatLower());
            } else {
                ps.setNull(4, java.sql.Types.NUMERIC);
            }
            if (point.getYhatUpper() != null) {
                ps.setDouble(5, point.getYhatUpper());
            } else {
                ps.setNull(5, java.sql.Types.NUMERIC);
            }
        });
    }

    /**
     * قراءة التنبؤ المخزّن لمنتج معيّن ضمن فترة تواريخ
     */
    public List<ForecastPointDTO> findForecastForProduct(Long productId,
                                                         LocalDate fromDate,
                                                         LocalDate toDate) {
        String sql = """
                SELECT product_id,
                       forecast_date,
                       yhat,
                       yhat_lower,
                       yhat_upper,
                       generated_at
                FROM public.product_daily_forecast
                WHERE product_id = ?
                  AND forecast_date BETWEEN ? AND ?
                ORDER BY forecast_date
                """;

        return jdbcTemplate.query(
                sql,
                new ForecastPointRowMapper(),
                productId,
                Date.valueOf(fromDate),
                Date.valueOf(toDate)
        );
    }
}
