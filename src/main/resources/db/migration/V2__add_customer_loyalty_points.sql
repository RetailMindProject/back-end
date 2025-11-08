-- =========================================================
--  V1__pos_full_schema.sql
--  POS System Database - Full Schema (with images & vouchers)
--  DB: PostgreSQL
-- =========================================================

-- =========================
--  USERS (System Employees)
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60),
    email VARCHAR(120) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(30) CHECK (role IN ('ADMIN', 'CASHIER', 'MANAGER')) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

-- =========================
--  CUSTOMERS
-- =========================
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(60),
    last_name VARCHAR(60),
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(120) UNIQUE,
    gender CHAR(1) CHECK (gender IN ('M','F')),
    birth_date DATE,
    last_visited_at TIMESTAMP,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

-- =========================
--  TERMINALS (POS Devices)
-- =========================
CREATE TABLE IF NOT EXISTS terminals (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    description VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now()
);

-- =========================
--  SESSIONS (Cashier Sessions)
-- =========================
CREATE TABLE IF NOT EXISTS sessions (
    id SERIAL PRIMARY KEY,
    terminal_id INT REFERENCES terminals(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    opened_at TIMESTAMP DEFAULT now(),
    closed_at TIMESTAMP,
    opening_float NUMERIC(12,2) DEFAULT 0,
    closing_amount NUMERIC(12,2),
    status VARCHAR(20) CHECK (status IN ('OPEN','CLOSED')) DEFAULT 'OPEN'
);

-- =========================
--  PRODUCTS (+ Images support)
-- =========================
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(120) NOT NULL,
    brand VARCHAR(60),
    description TEXT,
    default_cost NUMERIC(12,2),
    default_price NUMERIC(12,2),
    tax_rate NUMERIC(5,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    -- صورة رئيسية مختصرة (اختياري للتسهيل)
    primary_image_url TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

-- وسائط عامة (لصور/ملفات)
CREATE TABLE IF NOT EXISTS media (
    id SERIAL PRIMARY KEY,
    url TEXT NOT NULL,
    mime_type VARCHAR(100),
    title VARCHAR(120),
    alt_text VARCHAR(160),
    created_at TIMESTAMP DEFAULT now()
);

-- ربط المنتج بالوسائط (دعم عدة صور وترتيب وصورة أساسية)
CREATE TABLE IF NOT EXISTS product_media (
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    media_id INT REFERENCES media(id) ON DELETE CASCADE,
    sort_order INT DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (product_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_product_media_product ON product_media(product_id);
CREATE INDEX IF NOT EXISTS idx_product_media_primary ON product_media(product_id, is_primary);

-- =========================
--  CATEGORIES
-- =========================
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(80) UNIQUE NOT NULL,
    parent_id INT REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS product_categories (
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    category_id INT REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

-- =========================
--  ORDERS
-- =========================
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(30) UNIQUE NOT NULL,
    customer_id INT REFERENCES customers(id) ON DELETE SET NULL,
    session_id INT REFERENCES sessions(id) ON DELETE SET NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    discount_total NUMERIC(12,2) DEFAULT 0,
    tax_total NUMERIC(12,2) DEFAULT 0,
    grand_total NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) CHECK (status IN ('DRAFT','PAID','CANCELLED','RETURNED')) DEFAULT 'DRAFT',
    paid_at TIMESTAMP,
    parent_order_id INT REFERENCES orders(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT now()
);

-- =========================
--  ORDER ITEMS
-- =========================
CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE SET NULL,
    quantity NUMERIC(10,2) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    line_discount NUMERIC(12,2) DEFAULT 0,
    tax_amount NUMERIC(12,2) DEFAULT 0,
    line_total NUMERIC(12,2) NOT NULL
);

-- =========================
--  PAYMENTS (for Orders)
-- =========================
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    method VARCHAR(20) CHECK (method IN ('CASH','CARD','OTHER')) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

-- =========================
--  VOUCHERS (Cash book outside Orders)
--  - RECEIPT: قبض (مال داخل الصندوق)
--  - PAYMENT: دفع/صرف (مال خارج الصندوق)
--  - TRANSFER: تحويل (اختياري مستقبلاً)
-- =========================
CREATE TABLE IF NOT EXISTS vouchers (
    id SERIAL PRIMARY KEY,
    voucher_number VARCHAR(30) UNIQUE NOT NULL,
    type VARCHAR(20) CHECK (type IN ('RECEIPT','PAYMENT','TRANSFER')) NOT NULL,

    session_id INT REFERENCES sessions(id) ON DELETE SET NULL,     -- الشفت/الصندوق
    created_by_user_id INT REFERENCES users(id) ON DELETE SET NULL, -- من أنشأ السند (كاشير/مدير)
    approved_by_user_id INT REFERENCES users(id) ON DELETE SET NULL, -- من اعتمد السند (عادة مدير)

    -- طرف التعامل (اختياري)
    customer_id INT REFERENCES customers(id) ON DELETE SET NULL,
    counterparty_name VARCHAR(120),
    counterparty_type VARCHAR(20) CHECK (counterparty_type IN ('CUSTOMER','SUPPLIER','OTHER')),

    method VARCHAR(20) CHECK (method IN ('CASH','CARD','BANK','OTHER')) NOT NULL DEFAULT 'CASH',
    instrument_ref VARCHAR(60),          -- رقم شيك/حوالة...
    currency VARCHAR(10) DEFAULT 'ILS',

    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    note TEXT,

    status VARCHAR(20) CHECK (status IN ('DRAFT','APPROVED','CANCELLED')) DEFAULT 'APPROVED',
    created_at TIMESTAMP DEFAULT now(),
    approved_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vouchers_session ON vouchers(session_id);
CREATE INDEX IF NOT EXISTS idx_vouchers_type ON vouchers(type);
CREATE INDEX IF NOT EXISTS idx_vouchers_customer ON vouchers(customer_id);
CREATE INDEX IF NOT EXISTS idx_vouchers_status ON vouchers(status);
CREATE INDEX IF NOT EXISTS idx_vouchers_created_by ON vouchers(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_vouchers_approved_by ON vouchers(approved_by_user_id);

-- تفاصيل السند (اختياري للتفصيل المحاسبي)
CREATE TABLE IF NOT EXISTS voucher_lines (
    id SERIAL PRIMARY KEY,
    voucher_id INT REFERENCES vouchers(id) ON DELETE CASCADE,
    line_no INT NOT NULL DEFAULT 1,
    purpose VARCHAR(120),
    cost_center VARCHAR(60),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    note TEXT
);
CREATE INDEX IF NOT EXISTS idx_voucher_lines_voucher ON voucher_lines(voucher_id);

-- =========================
--  INVENTORY MOVEMENTS
-- =========================
CREATE TABLE IF NOT EXISTS inventory_movements (
    id SERIAL PRIMARY KEY,
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    location_type VARCHAR(20) CHECK (location_type IN ('WAREHOUSE','STORE')) NOT NULL,
    ref_type VARCHAR(20) CHECK (ref_type IN ('PURCHASE','SALE','RETURN','TRANSFER','ADJUSTMENT')) NOT NULL,
    ref_id INT,
    qty_change NUMERIC(10,2) NOT NULL,
    unit_cost NUMERIC(12,2),
    moved_at TIMESTAMP DEFAULT now(),
    note TEXT
);

-- =========================
--  STOCK SNAPSHOT (optional for reporting)
-- =========================
CREATE TABLE IF NOT EXISTS stock_snapshot (
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    store_qty NUMERIC(10,2) DEFAULT 0,
    warehouse_qty NUMERIC(10,2) DEFAULT 0,
    last_updated_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (product_id)
);

-- =========================================================
--  INDEXES (additional)
-- =========================================================
CREATE INDEX IF NOT EXISTS idx_orders_session_id ON orders(session_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_product ON inventory_movements(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_type ON inventory_movements(ref_type);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);

-- =========================================================
--  VIEWS (Unified cash book for shifts)
-- =========================================================
CREATE OR REPLACE VIEW v_cash_book AS
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
FROM payments p
LEFT JOIN orders o ON o.id = p.order_id
LEFT JOIN customers c ON c.id = o.customer_id

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
FROM vouchers v
LEFT JOIN customers c ON c.id = v.customer_id;

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
