-- Check if product ID 2 (Coca Cola) has image in the view
SELECT
    product_id,
    name,
    category_name,
    image_url,
    image_alt
FROM v_reco_product_catalog
WHERE product_id = 2;

-- Check if any products have images
SELECT
    COUNT(*) as total_products,
    COUNT(image_url) as products_with_images
FROM v_reco_product_catalog;

-- Show sample products with images
SELECT
    product_id,
    name,
    image_url,
    image_alt
FROM v_reco_product_catalog
WHERE image_url IS NOT NULL
LIMIT 5;
