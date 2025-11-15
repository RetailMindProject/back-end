-- ============================================
-- Step 1: Drop view that depends on integer IDs
-- ============================================
DROP VIEW IF EXISTS public.v_cash_book;

-- ============================================
-- Step 2: Convert all integer IDs & FKs to BIGINT
-- ============================================

-- USERS
ALTER TABLE public.users ALTER COLUMN id TYPE BIGINT;

-- CUSTOMERS
ALTER TABLE public.customers ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.customers ALTER COLUMN user_id TYPE BIGINT;

-- CATEGORIES
ALTER TABLE public.categories ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.categories ALTER COLUMN parent_id TYPE BIGINT;

-- PRODUCTS
ALTER TABLE public.products ALTER COLUMN id TYPE BIGINT;

-- PRODUCT_CATEGORIES
ALTER TABLE public.product_categories ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE public.product_categories ALTER COLUMN category_id TYPE BIGINT;

-- MEDIA
ALTER TABLE public.media ALTER COLUMN id TYPE BIGINT;

-- PRODUCT_MEDIA
ALTER TABLE public.product_media ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE public.product_media ALTER COLUMN media_id TYPE BIGINT;
ALTER TABLE public.product_media ALTER COLUMN sort_order TYPE BIGINT;

-- TERMINALS
ALTER TABLE public.terminals ALTER COLUMN id TYPE BIGINT;

-- SESSIONS
ALTER TABLE public.sessions ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.sessions ALTER COLUMN terminal_id TYPE BIGINT;
ALTER TABLE public.sessions ALTER COLUMN user_id TYPE BIGINT;

-- ORDERS
ALTER TABLE public.orders ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.orders ALTER COLUMN customer_id TYPE BIGINT;
ALTER TABLE public.orders ALTER COLUMN session_id TYPE BIGINT;
ALTER TABLE public.orders ALTER COLUMN parent_order_id TYPE BIGINT;

-- ORDER_ITEMS
ALTER TABLE public.order_items ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.order_items ALTER COLUMN order_id TYPE BIGINT;
ALTER TABLE public.order_items ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE public.order_items ALTER COLUMN offer_id TYPE BIGINT;

-- PAYMENTS
ALTER TABLE public.payments ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.payments ALTER COLUMN order_id TYPE BIGINT;

-- INVENTORY_MOVEMENTS
ALTER TABLE public.inventory_movements ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.inventory_movements ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE public.inventory_movements ALTER COLUMN ref_id TYPE BIGINT;

-- STOCK_SNAPSHOT
ALTER TABLE public.stock_snapshot ALTER COLUMN product_id TYPE BIGINT;

-- OFFERS
ALTER TABLE public.offers ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.offers ALTER COLUMN created_by TYPE BIGINT;

-- OFFER_PRODUCTS
ALTER TABLE public.offer_products ALTER COLUMN offer_id TYPE BIGINT;
ALTER TABLE public.offer_products ALTER COLUMN product_id TYPE BIGINT;

-- OFFER_CATEGORIES
ALTER TABLE public.offer_categories ALTER COLUMN offer_id TYPE BIGINT;
ALTER TABLE public.offer_categories ALTER COLUMN category_id TYPE BIGINT;

-- OFFER_BUNDLES
ALTER TABLE public.offer_bundles ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.offer_bundles ALTER COLUMN offer_id TYPE BIGINT;
ALTER TABLE public.offer_bundles ALTER COLUMN product_id TYPE BIGINT;

-- ORDER_OFFERS
ALTER TABLE public.order_offers ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.order_offers ALTER COLUMN offer_id TYPE BIGINT;

-- VOUCHERS
ALTER TABLE public.vouchers ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.vouchers ALTER COLUMN session_id TYPE BIGINT;
ALTER TABLE public.vouchers ALTER COLUMN created_by_user_id TYPE BIGINT;
ALTER TABLE public.vouchers ALTER COLUMN approved_by_user_id TYPE BIGINT;
ALTER TABLE public.vouchers ALTER COLUMN customer_id TYPE BIGINT;

-- VOUCHER_LINES
ALTER TABLE public.voucher_lines ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.voucher_lines ALTER COLUMN voucher_id TYPE BIGINT;
ALTER TABLE public.voucher_lines ALTER COLUMN line_no TYPE BIGINT;

-- MESSAGES
ALTER TABLE public.messages ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.messages ALTER COLUMN from_user_id TYPE BIGINT;
ALTER TABLE public.messages ALTER COLUMN to_user_id TYPE BIGINT;
ALTER TABLE public.messages ALTER COLUMN to_customer_id TYPE BIGINT;
ALTER TABLE public.messages ALTER COLUMN parent_id TYPE BIGINT;

-- MESSAGE_ATTACHMENTS
ALTER TABLE public.message_attachments ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.message_attachments ALTER COLUMN message_id TYPE BIGINT;
ALTER TABLE public.message_attachments ALTER COLUMN file_size TYPE BIGINT;

-- ============================================
-- Step 3: Recreate v_cash_book view
-- (using your latest definition)
-- ============================================

CREATE OR REPLACE VIEW public.v_cash_book AS
-- Order payments (always cash-in for the shift)
SELECT
  p.created_at::date AS tx_date,
  o.session_id,
  p.method,
  'ORDER_PAYMENT' AS source,
  p.id AS source_id,
  p.amount AS amount_in,
  0::NUMERIC(12,2) AS amount_out,
  o.order_number AS ref_code,
  COALESCE(c.first_name || ' ' || c.last_name, '') AS counterparty,
  p.created_at AS created_at
FROM public.payments p
LEFT JOIN public.orders o ON o.id = p.order_id
LEFT JOIN public.customers c ON c.id = o.customer_id

UNION ALL

-- Vouchers (RECEIPT -> in, PAYMENT -> out)
SELECT
  v.created_at::date AS tx_date,
  v.session_id,
  v.method,
  'VOUCHER' AS source,
  v.id AS source_id,
  CASE WHEN v.type='RECEIPT' THEN v.amount ELSE 0 END AS amount_in,
  CASE WHEN v.type='PAYMENT' THEN v.amount ELSE 0 END AS amount_out,
  v.voucher_number AS ref_code,
  COALESCE(
    CASE WHEN v.customer_id IS NOT NULL THEN (c.first_name || ' ' || c.last_name) END,
    v.counterparty_name,
    ''
  ) AS counterparty,
  v.created_at AS created_at
FROM public.vouchers v
LEFT JOIN public.customers c ON c.id = v.customer_id;

-- =========================================================
--  NOTES
--  - لعرض صورة المنتج الأساسية بسرعة: استخدم products.primary_image_url
--    ويمكنك الحفاظ على صور متعددة عبر media/product_media مع is_primary=true.
--  - السندات:
--      * الكاشير ينشئ عادةً RECEIPT فقط.
--      * المدير/المحاسب ينشئ/يعتمد PAYMENT و EXPENSE و TRANSFER.
--      * استخدم created_by_user_id و approved_by_user_id لتتبع الصلاحيات.
--  - v_cash_book يسهّل تقرير الشفت (صافي الحركة = IN - OUT).
-- =========================================================
