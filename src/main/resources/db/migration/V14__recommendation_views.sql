CREATE OR REPLACE VIEW public.v_reco_purchase_events AS
SELECT
    o.customer_id AS user_id,
    oi.product_id,
    'purchase'::text AS event_type,
    COALESCE(o.paid_at, o.created_at) AS event_time,
    oi.quantity,
    oi.unit_price,
    o.id AS order_id
FROM public.orders o
JOIN public.order_items oi ON oi.order_id = o.id
WHERE o.status = 'PAID'
  AND oi.product_id IS NOT NULL
  AND o.customer_id IS NOT NULL;

CREATE OR REPLACE VIEW public.v_reco_user_history AS
SELECT
    e.user_id,
    e.product_id,
    e.event_time
FROM public.v_reco_purchase_events e;

CREATE OR REPLACE VIEW public.v_reco_product_catalog AS
WITH prod_cat AS (
    SELECT pc.product_id, MIN(c.name) AS category_name
    FROM public.product_categories pc
    JOIN public.categories c ON c.id = pc.category_id
    GROUP BY pc.product_id
)
SELECT
    p.id AS product_id,
    p.sku,
    p.name,
    COALESCE(pc.category_name, '') AS category_name,
    'physical'::text AS type,
    p.default_price AS current_price,
    p.is_active,
    COALESCE(v.total_qty, COALESCE(ss.store_qty, 0) + COALESCE(ss.warehouse_qty, 0), 0) AS total_qty
FROM public.products p
LEFT JOIN prod_cat pc ON pc.product_id = p.id
LEFT JOIN public.v_product_current_stock v ON v.product_id = p.id
LEFT JOIN public.stock_snapshot ss ON ss.product_id = p.id
WHERE p.is_active = TRUE;

CREATE OR REPLACE VIEW public.v_reco_trending_30d AS
SELECT
    e.product_id,
    SUM(e.quantity) AS score
FROM public.v_reco_purchase_events e
WHERE e.event_time >= now() - INTERVAL '30 days'
GROUP BY e.product_id;

CREATE INDEX IF NOT EXISTS ix_orders_status_paid_at_customer ON public.orders(status, paid_at, customer_id);
CREATE INDEX IF NOT EXISTS ix_order_items_order_product ON public.order_items(order_id, product_id);
CREATE INDEX IF NOT EXISTS ix_product_categories_prod_cat ON public.product_categories(product_id, category_id);
CREATE INDEX IF NOT EXISTS ix_stock_snapshot_product ON public.stock_snapshot(product_id);
