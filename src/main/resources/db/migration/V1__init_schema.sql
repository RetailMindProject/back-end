-- =========================================================
--  V1__init_schema.sql
--  POS System Database Initialization
--  Author: AE Team
--  Description: Initial schema for POS + Inventory
-- =========================================================

-- =========================
--  USERS (System Employees)
-- =========================
CREATE TABLE users (
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
CREATE TABLE customers (
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
CREATE TABLE terminals (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    description VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now()
);

-- =========================
--  SESSIONS (Cashier Sessions)
-- =========================
CREATE TABLE sessions (
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
--  PRODUCTS
-- =========================
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(120) NOT NULL,
    brand VARCHAR(60),
    description TEXT,
    default_cost NUMERIC(12,2),
    default_price NUMERIC(12,2),
    tax_rate NUMERIC(5,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);

-- =========================
--  CATEGORIES
-- =========================
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(80) UNIQUE NOT NULL,
    parent_id INT REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE product_categories (
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    category_id INT REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

-- =========================
--  ORDERS
-- =========================
CREATE TABLE orders (
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
CREATE TABLE order_items (
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
--  PAYMENTS
-- =========================
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    method VARCHAR(20) CHECK (method IN ('CASH','CARD','OTHER')) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

-- =========================
--  INVENTORY MOVEMENTS
-- =========================
CREATE TABLE inventory_movements (
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
CREATE TABLE stock_snapshot (
    product_id INT REFERENCES products(id) ON DELETE CASCADE,
    store_qty NUMERIC(10,2) DEFAULT 0,
    warehouse_qty NUMERIC(10,2) DEFAULT 0,
    last_updated_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (product_id)
);

-- =========================================================
--  INDEXES
-- =========================================================
CREATE INDEX idx_orders_session_id ON orders(session_id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_inventory_movements_product ON inventory_movements(product_id);
CREATE INDEX idx_inventory_movements_type ON inventory_movements(ref_type);
CREATE INDEX idx_payments_order_id ON payments(order_id);

-- =========================================================
--  END OF SCHEMA
-- =========================================================
