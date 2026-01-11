-- =========================================================
-- V18__enable_pg_trgm_and_product_search.sql
-- Add PostgreSQL trigram extension and GIN indexes for typo-tolerant search
-- =========================================================

-- Step 1: Enable trigram extension
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Step 2: Create GIN trigram index on products.name
-- This enables the % operator for fast fuzzy matching on product names
CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON public.products USING gin (name gin_trgm_ops);

-- Step 3: Create GIN trigram index on products.sku
-- This enables fast fuzzy matching on SKU codes
-- Safe creation: check if sku column exists before creating index
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'products' AND column_name = 'sku'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_products_sku_trgm
                 ON public.products USING gin (sku gin_trgm_ops)';
    END IF;
END $$;

-- Step 4: Optional - Create composite index for active product searches
-- Supports queries filtering by is_active with name/sku search
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_products_active_name_trgm'
    ) THEN
        EXECUTE 'CREATE INDEX idx_products_active_name_trgm
                 ON public.products (is_active)
                 WHERE is_active = true';
    END IF;
END $$;

-- =========================================================
-- Migration complete: pg_trgm indexes ready for fuzzy search
-- =========================================================

