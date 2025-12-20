package com.example.back_end.modules.forecasting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ForecastingProductRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * إرجاع قائمة بالمنتجات النشطة التي لديها عدد كافٍ من الأيام في الفترة المحددة.
     *
     * @param fromDate  تاريخ البداية
     * @param toDate    تاريخ النهاية
     * @param minPoints أقل عدد نقاط (أيام) لاعتبار المنتج مؤهَّلاً للتنبؤ
     */
    public List<Long> findEligibleProductIds(LocalDate fromDate,
                                             LocalDate toDate,
                                             int minPoints) {
        String sql = """
                SELECT p.id
                FROM public.products p
                JOIN public.v_daily_product_sales_with_offers v
                  ON v.product_id = p.id
                WHERE p.is_active = TRUE
                  AND v.sales_date BETWEEN ? AND ?
                GROUP BY p.id
                HAVING COUNT(*) >= ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("id"),
                Date.valueOf(fromDate),
                Date.valueOf(toDate),
                minPoints
        );
    }
}
