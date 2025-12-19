-- =========================================================
-- V10__forecasting_base.sql
-- Base structures for Forecasting System in RetailMind POS
-- =========================================================

-- 1) View: v_daily_product_sales_with_offers
--    - مبيعات يومية لكل منتج
--    - متغيرات عن وجود عروض بأنواعها + نسبة الخصم الفعلية

CREATE OR REPLACE VIEW public.v_daily_product_sales_with_offers AS
WITH order_lines AS (
    SELECT
        DATE(o.paid_at)        AS sales_date,
        o.id                   AS order_id,
        oi.product_id,
        oi.quantity,
        oi.unit_price,
        oi.line_discount,
        (oi.unit_price * oi.quantity) AS line_gross,
        oi.line_total,
        o.discount_total,
        of_line.offer_type     AS line_offer_type
    FROM public.orders o
    JOIN public.order_items oi
        ON oi.order_id = o.id
    LEFT JOIN public.offers of_line
        ON of_line.id = oi.offer_id
    WHERE o.status = 'PAID'
      AND o.paid_at IS NOT NULL
),
order_lines_with_dist AS (
    SELECT
        ol.*,
        -- إجمالي قيمة السطور في الطلب (بدون خصم)
        SUM(ol.line_gross) OVER (PARTITION BY ol.order_id) AS order_gross,

        -- توزيع خصم الطلب (discount_total) على السطور بنسبة مساهمة كل سطر
        CASE
            WHEN SUM(ol.line_gross) OVER (PARTITION BY ol.order_id) > 0
            THEN (
                ol.discount_total
                * (ol.line_gross
                   / SUM(ol.line_gross) OVER (PARTITION BY ol.order_id))
            )
            ELSE 0
        END AS dist_order_discount
    FROM order_lines ol
)
SELECT
    sales_date,
    product_id,

    -- الهدف الأساسي: الكمية المباعة في اليوم (y)
    SUM(quantity)                       AS total_qty_sold,

    -- إجمالي الإيراد (المبلغ بعد الخصم)
    SUM(line_total)                     AS total_revenue,

    -- متوسط سعر البيع في ذلك اليوم
    AVG(unit_price)                     AS avg_unit_price,

    -- عدد الطلبات المختلفة التي شارك فيها هذا المنتج في هذا اليوم
    COUNT(DISTINCT order_id)            AS orders_count,

    -- أعلام لكل نوع عرض
    CASE
        WHEN MAX(
                CASE
                    WHEN line_offer_type = 'PRODUCT' THEN 1
                    ELSE 0
                END
            ) = 1
        THEN 1 ELSE 0
    END                                  AS promo_product_flag,

    CASE
        WHEN MAX(
                CASE
                    WHEN line_offer_type = 'CATEGORY' THEN 1
                    ELSE 0
                END
            ) = 1
        THEN 1 ELSE 0
    END                                  AS promo_category_flag,

    -- عرض ORDER: نعتبره موجودًا إذا:
    --   - يوجد line_offer_type = 'ORDER' لأي سطر
    --   - أو كان هناك discount_total > 0 على مستوى الطلب
    CASE
        WHEN
            MAX(
                CASE
                    WHEN line_offer_type = 'ORDER' THEN 1
                    ELSE 0
                END
            ) = 1
            OR
            MAX(
                CASE
                    WHEN discount_total > 0 THEN 1
                    ELSE 0
                END
            ) = 1
        THEN 1 ELSE 0
    END                                  AS promo_order_flag,

    CASE
        WHEN MAX(
                CASE
                    WHEN line_offer_type = 'BUNDLE' THEN 1
                    ELSE 0
                END
            ) = 1
        THEN 1 ELSE 0
    END                                  AS promo_bundle_flag,

    -- أي نوع عرض (OR لكل الأنواع)
    CASE
        WHEN
            MAX(
                CASE
                    WHEN line_offer_type = 'PRODUCT' THEN 1
                    ELSE 0
                END
            ) = 1
            OR
            MAX(
                CASE
                    WHEN line_offer_type = 'CATEGORY' THEN 1
                    ELSE 0
                END
            ) = 1
            OR
            MAX(
                CASE
                    WHEN line_offer_type = 'BUNDLE' THEN 1
                    ELSE 0
                END
            ) = 1
            OR
            MAX(
                CASE
                    WHEN discount_total > 0 THEN 1
                    ELSE 0
                END
            ) = 1
        THEN 1 ELSE 0
    END                                  AS promo_any_flag,

    -- نسبة الخصم الفعلية في هذا اليوم لهذا المنتج:
    -- effective_discount = line_discount + dist_order_discount
    CASE
        WHEN SUM(line_gross) > 0
        THEN
            SUM(
                line_discount
              + dist_order_discount
            ) / SUM(line_gross)
        ELSE 0
    END                                  AS avg_discount_pct

FROM order_lines_with_dist
GROUP BY
    sales_date,
    product_id;


-- =========================================
-- 2) View: v_product_current_stock
--    - صورة المخزون الحالي لكل منتج
-- =========================================

CREATE OR REPLACE VIEW public.v_product_current_stock AS
SELECT
    p.id                      AS product_id,
    p.sku,
    p.name,
    p.brand,
    COALESCE(s.store_qty, 0)      AS store_qty,
    COALESCE(s.warehouse_qty, 0)  AS warehouse_qty,
    COALESCE(s.store_qty, 0)
      + COALESCE(s.warehouse_qty, 0) AS total_qty,
    s.last_updated_at
FROM public.products p
LEFT JOIN public.stock_snapshot s
    ON s.product_id = p.id
WHERE p.is_active = TRUE;


-- =========================================
-- 3) Table: product_daily_forecast
--    - تخزين تنبؤ Prophet اليومي لكل منتج
-- =========================================

CREATE TABLE IF NOT EXISTS public.product_daily_forecast (
    product_id    bigint NOT NULL,
    forecast_date date   NOT NULL,
    yhat          numeric(12,2) NOT NULL,   -- القيمة المتوقعة
    yhat_lower    numeric(12,2),            -- الحد الأدنى (اختياري)
    yhat_upper    numeric(12,2),            -- الحد الأعلى (اختياري)
    generated_at  timestamp without time zone DEFAULT now(),

    CONSTRAINT product_daily_forecast_pkey
        PRIMARY KEY (product_id, forecast_date),

    CONSTRAINT product_daily_forecast_product_fk
        FOREIGN KEY (product_id)
        REFERENCES public.products (id)
        ON DELETE CASCADE
);


-- =========================================
-- 4) Table: product_stock_forecast_summary
--    - ملخص حالة المخزون + التنبؤات لكل منتج
-- =========================================

CREATE TABLE IF NOT EXISTS public.product_stock_forecast_summary (
    product_id              bigint PRIMARY KEY,
    current_stock           numeric(10,2) NOT NULL,
    avg_daily_demand        numeric(10,2) NOT NULL,
    expected_stockout_date  date,
    recommended_reorder_qty numeric(10,2),
    generated_at            timestamp without time zone DEFAULT now(),

    CONSTRAINT product_stock_forecast_summary_product_fk
        FOREIGN KEY (product_id)
        REFERENCES public.products (id)
        ON DELETE CASCADE
);
