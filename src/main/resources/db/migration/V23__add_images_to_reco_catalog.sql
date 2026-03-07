-- Migration to add image support to recommendation product catalog view
-- This provides image URLs and alt text for the recommendation system UI

CREATE OR REPLACE VIEW public.v_reco_product_catalog AS
WITH prod_cat AS (
    SELECT pc.product_id, MIN(c.name) AS category_name
    FROM public.product_categories pc
    JOIN public.categories c ON c.id = pc.category_id
    GROUP BY pc.product_id
),
prod_primary_image AS (
    SELECT
        pm.product_id,
        m.url AS image_url,
        COALESCE(m.alt_text, p.name) AS image_alt
    FROM public.product_media pm
    JOIN public.media m ON m.id = pm.media_id
    JOIN public.products p ON p.id = pm.product_id
    WHERE pm.is_primary = TRUE
),
prod_first_image AS (
    SELECT
        pm.product_id,
        m.url AS image_url,
        COALESCE(m.alt_text, p.name) AS image_alt,
        ROW_NUMBER() OVER (PARTITION BY pm.product_id ORDER BY pm.sort_order, pm.media_id) AS rn
    FROM public.product_media pm
    JOIN public.media m ON m.id = pm.media_id
    JOIN public.products p ON p.id = pm.product_id
)
SELECT
    p.id AS product_id,
    p.sku,
    p.name,
    COALESCE(pc.category_name, '') AS category_name,
    'physical'::text AS type,
    p.default_price AS current_price,
    p.is_active,
    COALESCE(v.total_qty, COALESCE(ss.store_qty, 0) + COALESCE(ss.warehouse_qty, 0), 0) AS total_qty,
    COALESCE(ppi.image_url, pfi.image_url) AS image_url,
    COALESCE(ppi.image_alt, pfi.image_alt) AS image_alt
FROM public.products p
LEFT JOIN prod_cat pc ON pc.product_id = p.id
LEFT JOIN public.v_product_current_stock v ON v.product_id = p.id
LEFT JOIN public.stock_snapshot ss ON ss.product_id = p.id
LEFT JOIN prod_primary_image ppi ON ppi.product_id = p.id
LEFT JOIN prod_first_image pfi ON pfi.product_id = p.id AND pfi.rn = 1
WHERE p.is_active = TRUE;
