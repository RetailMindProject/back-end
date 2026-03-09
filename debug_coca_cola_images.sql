-- Check if Coca Cola (product_id=2) has images in the database
SELECT
    p.id AS product_id,
    p.name,
    pm.media_id,
    pm.is_primary,
    pm.sort_order,
    m.url AS media_url,
    m.alt_text AS media_alt_text
FROM products p
LEFT JOIN product_media pm ON pm.product_id = p.id
LEFT JOIN media m ON m.id = pm.media_id
WHERE p.id = 2;

-- Check the view output for Coca Cola
SELECT product_id, name, image_url, image_alt
FROM v_reco_product_catalog
WHERE product_id = 2;

-- Check how many products have images in product_media table
SELECT COUNT(*) as total_products,
       COUNT(DISTINCT pm.product_id) as products_with_images
FROM products p
LEFT JOIN product_media pm ON pm.product_id = p.id;

-- Show sample products with images from the view
SELECT product_id, name, image_url, image_alt
FROM v_reco_product_catalog
WHERE image_url IS NOT NULL
LIMIT 10;
