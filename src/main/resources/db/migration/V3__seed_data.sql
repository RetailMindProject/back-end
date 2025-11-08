-- =========================================================
--  V3__seed_data.sql
--  POS System - Realistic seed data (no primary_image_url in products)
-- =========================================================

-- =========================================================
-- 0) HELPER SEQUENCES & FUNCTIONS
-- =========================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'next_order_no') THEN
        CREATE SEQUENCE seq_order_no START 1001;
        CREATE OR REPLACE FUNCTION next_order_no()
        RETURNS TEXT AS $f$
        BEGIN
            RETURN 'ORD-' || LPAD(nextval('seq_order_no')::text, 5, '0');
        END;
        $f$ LANGUAGE plpgsql;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'next_voucher_no') THEN
        CREATE SEQUENCE seq_voucher_no START 5001;
        CREATE OR REPLACE FUNCTION next_voucher_no()
        RETURNS TEXT AS $f$
        BEGIN
            RETURN 'VCH-' || LPAD(nextval('seq_voucher_no')::text, 5, '0');
        END;
        $f$ LANGUAGE plpgsql;
    END IF;
END $$;

-- =========================================================
-- 1) USERS
-- =========================================================
INSERT INTO users (first_name, last_name, email, phone, address, role, is_active)
VALUES
('Ahmad', 'Admin', 'admin@retailmind.ps', '0599000001', 'Ramallah - HQ', 'ADMIN', TRUE),
('Moath', 'Cashier', 'cashier.moath@retailmind.ps', '0599000002', 'Ramallah - Branch 1', 'CASHIER', TRUE),
('Lina', 'Inventory', 'lina.store@retailmind.ps', '0599000003', 'Ramallah - Warehouse', 'MANAGER', TRUE)
ON CONFLICT (email) DO NOTHING;

-- =========================================================
-- 2) CUSTOMERS
-- =========================================================
WITH u AS (
    SELECT
        (SELECT id FROM users WHERE email='admin@retailmind.ps' LIMIT 1) AS admin_id,
        (SELECT id FROM users WHERE email='cashier.moath@retailmind.ps' LIMIT 1) AS cashier_id
)
INSERT INTO customers (first_name, last_name, phone, email, gender, birth_date, last_visited_at, user_id)
SELECT
    'Omar', 'Hassan', '0598111101', 'omar.hassan@example.com', 'M', '1999-04-12',
    now() - INTERVAL '1 day',
    u.admin_id
FROM u
ON CONFLICT (phone) DO NOTHING;

WITH u AS (
    SELECT
        (SELECT id FROM users WHERE email='admin@retailmind.ps' LIMIT 1) AS admin_id,
        (SELECT id FROM users WHERE email='cashier.moath@retailmind.ps' LIMIT 1) AS cashier_id
)
INSERT INTO customers (first_name, last_name, phone, email, gender, birth_date, last_visited_at, user_id)
SELECT
    'Sara', 'Khalil', '0598222202', 'sara.khalil@example.com', 'F', '2001-08-21',
    now() - INTERVAL '2 day',
    u.cashier_id
FROM u
ON CONFLICT (phone) DO NOTHING;

WITH u AS (
    SELECT
        (SELECT id FROM users WHERE email='admin@retailmind.ps' LIMIT 1) AS admin_id
)
INSERT INTO customers (first_name, last_name, phone, email, gender, birth_date, last_visited_at, user_id)
SELECT
    'Mahmoud', 'Salem', '0598333303', NULL, 'M', '1995-02-03',
    now() - INTERVAL '5 day',
    u.admin_id
FROM u
ON CONFLICT (phone) DO NOTHING;

WITH u AS (
    SELECT
        (SELECT id FROM users WHERE email='cashier.moath@retailmind.ps' LIMIT 1) AS cashier_id
)
INSERT INTO customers (first_name, last_name, phone, email, gender, birth_date, last_visited_at, user_id)
SELECT
    'Noura', 'Zaid', '0598444404', 'noura.zaid@example.com', 'F', '1998-11-30',
    now() - INTERVAL '3 hour',
    u.cashier_id
FROM u
ON CONFLICT (phone) DO NOTHING;

-- =========================================================
-- 3) TERMINALS
-- =========================================================
INSERT INTO terminals (code, description, is_active)
VALUES
('POS-01', 'Front cash counter', TRUE),
('POS-02', 'Backup counter', TRUE)
ON CONFLICT (code) DO NOTHING;

-- =========================================================
-- 4) SESSIONS
-- =========================================================
INSERT INTO sessions (terminal_id, user_id, opened_at, opening_float, status)
SELECT
    t.id,
    u.id,
    date_trunc('day', now()) + INTERVAL '09:00',
    200.00,
    'OPEN'
FROM terminals t
JOIN users u ON u.email = 'cashier.moath@retailmind.ps'
WHERE t.code = 'POS-01'
  AND NOT EXISTS (
      SELECT 1 FROM sessions s
      WHERE s.status='OPEN' AND s.terminal_id = t.id
  );

INSERT INTO sessions (terminal_id, user_id, opened_at, closed_at, opening_float, closing_amount, status)
SELECT
    t.id,
    u.id,
    (date_trunc('day', now()) - INTERVAL '1 day') + INTERVAL '09:00',
    (date_trunc('day', now()) - INTERVAL '1 day') + INTERVAL '17:00',
    200.00,
    1350.50,
    'CLOSED'
FROM terminals t
JOIN users u ON u.email = 'cashier.moath@retailmind.ps'
WHERE t.code = 'POS-01'
  AND NOT EXISTS (
      SELECT 1 FROM sessions s
      WHERE s.status='CLOSED'
  );

-- =========================================================
-- 5) CATEGORIES
-- =========================================================
INSERT INTO categories (name, parent_id) VALUES
('Beverages', NULL),
('Dairy & Cheese', NULL),
('Snacks', NULL),
('Cleaning Supplies', NULL),
('Fresh & Produce', NULL)
ON CONFLICT (name) DO NOTHING;

-- =========================================================
-- 6) PRODUCTS  (بدون primary_image_url)
-- =========================================================
INSERT INTO products (sku, name, brand, description, default_cost, default_price, tax_rate, is_active)
VALUES
('BEV-PEPSI-330', 'Pepsi Can 330ml', 'Pepsi', 'Cold soft drink', 1.80, 3.00, 0, TRUE),
('BEV-COCA-500', 'Coca Cola 500ml', 'Coca Cola', 'Bottle soft drink', 2.20, 3.50, 0, TRUE),
('DAIRY-ALMARAI-MILK1L', 'Fresh Milk 1L', 'Almarai', 'Fresh milk 1 liter', 4.00, 5.50, 0, TRUE),
('SNACKS-LAYS-CHS', 'Lays Cheese 48g', 'Lays', 'Potato chips cheese flavor', 1.50, 2.50, 0, TRUE),
('CLEAN-FAIRY-650', 'Fairy Dishwashing 650ml', 'Fairy', 'Dishwashing liquid', 6.00, 8.50, 0, TRUE),
('FRESH-APPLE-RED', 'Red Apple (kg)', 'Local', 'Fresh red apples', 4.50, 7.00, 0, TRUE)
ON CONFLICT (sku) DO NOTHING;

-- =========================================================
-- 7) MEDIA & PRODUCT_MEDIA
-- =========================================================
INSERT INTO media (url, mime_type, title, alt_text)
VALUES
('https://cdn.example.com/img/pepsi330.png', 'image/png', 'Pepsi 330ml', 'Pepsi Can'),
('https://cdn.example.com/img/cocacola500.png', 'image/png', 'Coca Cola 500ml', 'Coca Cola Bottle')
ON CONFLICT DO NOTHING;

-- ربط المنتج بالصور
INSERT INTO product_media (product_id, media_id, sort_order, is_primary)
SELECT p.id, m.id, 1, TRUE
FROM products p
JOIN media m ON m.url LIKE '%pepsi330%'
WHERE p.sku = 'BEV-PEPSI-330'
ON CONFLICT DO NOTHING;

INSERT INTO product_media (product_id, media_id, sort_order, is_primary)
SELECT p.id, m.id, 1, TRUE
FROM products p
JOIN media m ON m.url LIKE '%cocacola500%'
WHERE p.sku = 'BEV-COCA-500'
ON CONFLICT DO NOTHING;

-- =========================================================
-- 8) PRODUCT CATEGORIES
-- =========================================================
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id
FROM products p, categories c
WHERE p.sku = 'BEV-PEPSI-330' AND c.name = 'Beverages'
ON CONFLICT DO NOTHING;

INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id
FROM products p, categories c
WHERE p.sku = 'BEV-COCA-500' AND c.name = 'Beverages'
ON CONFLICT DO NOTHING;

INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id
FROM products p, categories c
WHERE p.sku = 'SNACKS-LAYS-CHS' AND c.name = 'Snacks'
ON CONFLICT DO NOTHING;

INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id
FROM products p, categories c
WHERE p.sku = 'CLEAN-FAIRY-650' AND c.name = 'Cleaning Supplies'
ON CONFLICT DO NOTHING;

-- =========================================================
-- 9) ORDERS + ORDER ITEMS + PAYMENTS
-- =========================================================

-- Order 1: PAID (cash)
WITH s AS (
    SELECT id AS session_id FROM sessions
    WHERE status = 'OPEN'
    ORDER BY opened_at DESC
    LIMIT 1
),
c AS (
    SELECT id AS customer_id FROM customers
    WHERE phone = '0598111101'
)
INSERT INTO orders (
    order_number, customer_id, session_id,
    subtotal, discount_total, tax_total, grand_total,
    status, paid_at
)
SELECT
    next_order_no(),
    c.customer_id,
    s.session_id,
    9.00, 0, 0,
    9.00,
    'PAID',
    now()
FROM s, c;

-- items for order 1
WITH o AS (
    SELECT id FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_discount, tax_amount, line_total)
SELECT o.id, p.id, 1, 3.00, 0, 0, 3.00
FROM o, products p
WHERE p.sku = 'BEV-PEPSI-330';

WITH o AS (
    SELECT id FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_discount, tax_amount, line_total)
SELECT o.id, p.id, 1, 3.50, 0, 0, 3.50
FROM o, products p
WHERE p.sku = 'BEV-COCA-500';

WITH o AS (
    SELECT id FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_discount, tax_amount, line_total)
SELECT o.id, p.id, 1, 2.50, 0, 0, 2.50
FROM o, products p
WHERE p.sku = 'SNACKS-LAYS-CHS';

-- payment
WITH o AS (
    SELECT id, grand_total FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO payments (order_id, method, amount, created_at)
SELECT o.id, 'CASH', o.grand_total, now()
FROM o;

-- Order 2: PAID (card)
WITH s AS (
    SELECT id AS session_id FROM sessions
    WHERE status = 'OPEN'
    ORDER BY opened_at DESC
    LIMIT 1
),
c AS (
    SELECT id AS customer_id FROM customers
    WHERE phone = '0598222202'
)
INSERT INTO orders (
    order_number, customer_id, session_id,
    subtotal, discount_total, tax_total, grand_total,
    status, paid_at
)
SELECT
    next_order_no(),
    c.customer_id,
    s.session_id,
    14.00, 1.00, 0,
    13.00,
    'PAID',
    now()
FROM s, c;

WITH o AS (
    SELECT id FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_discount, tax_amount, line_total)
SELECT o.id, p.id, 1, 5.50, 0, 0, 5.50
FROM o, products p
WHERE p.sku = 'DAIRY-ALMARAI-MILK1L';

WITH o AS (
    SELECT id FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_discount, tax_amount, line_total)
SELECT o.id, p.id, 1, 8.50, 1.00, 0, 7.50
FROM o, products p
WHERE p.sku = 'CLEAN-FAIRY-650';

WITH o AS (
    SELECT id, grand_total FROM orders WHERE status='PAID' ORDER BY id DESC LIMIT 1
)
INSERT INTO payments (order_id, method, amount, created_at)
SELECT o.id, 'CARD', o.grand_total, now()
FROM o;

-- Order 3: CANCELLED
WITH s AS (
    SELECT id AS session_id FROM sessions
    WHERE status = 'OPEN'
    ORDER BY opened_at DESC
    LIMIT 1
)
INSERT INTO orders (
    order_number, customer_id, session_id,
    subtotal, discount_total, tax_total, grand_total,
    status
)
SELECT
    next_order_no(),
    NULL,
    s.session_id,
    7.00, 0, 0,
    7.00,
    'CANCELLED'
FROM s;

-- =========================================================
-- 10) VOUCHERS + LINES
-- =========================================================
WITH s AS (
    SELECT id AS session_id FROM sessions WHERE status='OPEN' ORDER BY opened_at DESC LIMIT 1
),
c AS (
    SELECT id AS customer_id, first_name, last_name FROM customers WHERE phone='0598333303' LIMIT 1
)
INSERT INTO vouchers (
    voucher_number, type, session_id, created_by_user_id, approved_by_user_id,
    customer_id, counterparty_name, counterparty_type,
    method, instrument_ref, currency, amount, note, status, created_at, approved_at
)
SELECT
    next_voucher_no(),
    'RECEIPT',
    s.session_id,
    (SELECT id FROM users WHERE email='cashier.moath@retailmind.ps'),
    (SELECT id FROM users WHERE email='admin@retailmind.ps'),
    c.customer_id,
    c.first_name || ' ' || c.last_name,
    'CUSTOMER',
    'CASH',
    NULL,
    'ILS',
    50.00,
    'Customer paid part of previous balance',
    'APPROVED',
    now(),
    now()
FROM s, c;

INSERT INTO voucher_lines (voucher_id, line_no, purpose, cost_center, amount, note)
SELECT v.id, 1, 'Customer receivable settlement', 'POS', 50.00, 'Auto line'
FROM vouchers v
WHERE v.type='RECEIPT'
ORDER BY v.id DESC
LIMIT 1;

WITH s AS (
    SELECT id AS session_id FROM sessions WHERE status='OPEN' ORDER BY opened_at DESC LIMIT 1
)
INSERT INTO vouchers (
    voucher_number, type, session_id, created_by_user_id, approved_by_user_id,
    counterparty_name, counterparty_type,
    method, currency, amount, note, status, created_at, approved_at
)
SELECT
    next_voucher_no(),
    'PAYMENT',
    s.session_id,
    (SELECT id FROM users WHERE email='cashier.moath@retailmind.ps'),
    (SELECT id FROM users WHERE email='admin@retailmind.ps'),
    'Cleaning Company',
    'OTHER',
    'CASH',
    'ILS',
    30.00,
    'Daily store cleaning',
    'APPROVED',
    now(),
    now()
FROM s;

INSERT INTO voucher_lines (voucher_id, line_no, purpose, cost_center, amount, note)
SELECT v.id, 1, 'Store cleaning service', 'ADMIN', 30.00, 'Paid at end of shift'
FROM vouchers v
WHERE v.type='PAYMENT'
ORDER BY v.id DESC
LIMIT 1;

-- =========================================================
-- 11) INVENTORY MOVEMENTS
-- =========================================================
INSERT INTO inventory_movements (product_id, location_type, ref_type, ref_id, qty_change, unit_cost, note)
SELECT p.id, 'WAREHOUSE', 'PURCHASE', NULL, 100, p.default_cost, 'Initial stock'
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM inventory_movements im
    WHERE im.product_id = p.id AND im.location_type='WAREHOUSE'
);

INSERT INTO inventory_movements (product_id, location_type, ref_type, ref_id, qty_change, unit_cost, note)
SELECT DISTINCT p.id, 'STORE', 'SALE', o.id, -1, p.default_cost, 'Sale from order'
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
JOIN products p ON p.id = oi.product_id
WHERE o.status='PAID';

-- =========================================================
-- 12) STOCK SNAPSHOT
-- =========================================================
DELETE FROM stock_snapshot;

INSERT INTO stock_snapshot (product_id, store_qty, warehouse_qty, last_updated_at)
SELECT
    p.id,
    COALESCE((
        SELECT SUM(im.qty_change)
        FROM inventory_movements im
        WHERE im.product_id = p.id AND im.location_type = 'STORE'
    ), 0) AS store_qty,
    COALESCE((
        SELECT SUM(im.qty_change)
        FROM inventory_movements im
        WHERE im.product_id = p.id AND im.location_type = 'WAREHOUSE'
    ), 0) AS warehouse_qty,
    now()
FROM products p;
