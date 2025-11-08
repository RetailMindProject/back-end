-- =========================================================
--  V4__offers.sql
--  Add Offers/Promotions + normalize user roles
-- =========================================================

-- 1) احذف القيد القديم أولاً عشان نقدر نعدّل الداتا
ALTER TABLE users
  DROP CONSTRAINT IF EXISTS users_role_check;

-- 2) نظّف الداتا القديمة
-- ADMIN القديمة = CEO
UPDATE users
SET role = 'CEO'
WHERE role = 'ADMIN';

-- MANAGER القديمة = STORE_MANAGER
UPDATE users
SET role = 'STORE_MANAGER'
WHERE role = 'MANAGER';

-- أي رول فاضي/NULL نخليه CASHIER عشان ما يكسر القيد
UPDATE users
SET role = 'CASHIER'
WHERE role IS NULL OR role = '';

-- 3) رجّع القيد لكن بالقيم الجديدة
ALTER TABLE users
  ADD CONSTRAINT users_role_check
  CHECK (role IN (
      'CEO',
      'STORE_MANAGER',
      'INVENTORY_MANAGER',
      'CASHIER'
  ));

-- 4) توحيد طرق الدفع (CASH, CARD فقط)
ALTER TABLE payments
  DROP CONSTRAINT IF EXISTS payments_method_check;

ALTER TABLE payments
  ADD CONSTRAINT payments_method_check
  CHECK (method IN ('CASH','CARD'));

ALTER TABLE vouchers
  DROP CONSTRAINT IF EXISTS vouchers_method_check;

ALTER TABLE vouchers
  ADD CONSTRAINT vouchers_method_check
  CHECK (method IN ('CASH','CARD'));

-- 5) جدول العروض الرئيسي
CREATE TABLE IF NOT EXISTS offers (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) UNIQUE,
    title VARCHAR(120) NOT NULL,
    description TEXT,
    offer_type VARCHAR(20) NOT NULL
        CHECK (offer_type IN ('PRODUCT','CATEGORY','ORDER','BUNDLE')),
    discount_type VARCHAR(20) NOT NULL
        CHECK (discount_type IN ('PERCENTAGE','FIXED_AMOUNT')),
    discount_value NUMERIC(12,2) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at   TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_by INT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_offers_active_period
    ON offers (is_active, start_at, end_at, offer_type);

-- 6) ربط عرض بمنتجات
CREATE TABLE IF NOT EXISTS offer_products (
    offer_id INT REFERENCES offers(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    PRIMARY KEY (offer_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_offer_products_product
    ON offer_products (product_id);

-- 7) ربط عرض بتصنيفات
CREATE TABLE IF NOT EXISTS offer_categories (
    offer_id INT REFERENCES offers(id) ON DELETE CASCADE,
    category_id INT REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (offer_id, category_id)
);

-- 8) عروض على الأوردر كامل
CREATE TABLE IF NOT EXISTS order_offers (
    id SERIAL PRIMARY KEY,
    offer_id INT REFERENCES offers(id) ON DELETE CASCADE,
    min_order_amount NUMERIC(12,2),
    apply_once BOOLEAN DEFAULT TRUE
);

-- 9) عروض تركيبات (Bundle)
CREATE TABLE IF NOT EXISTS offer_bundles (
    id SERIAL PRIMARY KEY,
    offer_id INT REFERENCES offers(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    required_qty NUMERIC(10,2) NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_offer_bundles_offer
    ON offer_bundles (offer_id);

-- 10) ربط سطر الفاتورة بالعرض
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS offer_id INT REFERENCES offers(id) ON DELETE SET NULL;
