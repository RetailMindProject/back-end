-- =========================================================
--  V12__inventory_batches_and_wasted_and_hold.sql
--  1) Add WASTED to inventory_movements.ref_type
--  2) Add inventory batch tracking tables
--  3) Add HOLD to orders.status
-- =========================================================

-- -----------------------------------------------------------------
-- 1) inventory_movements.ref_type: add WASTED
--    Previous allowed: PURCHASE, SALE, RETURN, TRANSFER, ADJUSTMENT
-- -----------------------------------------------------------------
DO $$
DECLARE
    c_name text;
BEGIN
    -- Drop any CHECK constraint that is defined on inventory_movements.ref_type
    FOR c_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE ns.nspname = 'public'
          AND rel.relname = 'inventory_movements'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%ref_type%'
    LOOP
        EXECUTE format('ALTER TABLE public.inventory_movements DROP CONSTRAINT IF EXISTS %I', c_name);
    END LOOP;

    ALTER TABLE public.inventory_movements
        ADD CONSTRAINT inventory_movements_ref_type_check
            CHECK (ref_type IN ('PURCHASE','SALE','RETURN','TRANSFER','ADJUSTMENT','WASTED'));
END $$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------
-- 2) orders.status: add HOLD
--    Previous allowed: DRAFT, PAID, CANCELLED, RETURNED
-- -----------------------------------------------------------------
DO $$
DECLARE
    c_name text;
BEGIN
    -- Drop any CHECK constraint that is defined on orders.status
    FOR c_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE ns.nspname = 'public'
          AND rel.relname = 'orders'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE public.orders DROP CONSTRAINT IF EXISTS %I', c_name);
    END LOOP;

    ALTER TABLE public.orders
        ADD CONSTRAINT orders_status_check
            CHECK (status IN ('DRAFT','PAID','CANCELLED','RETURNED','HOLD'));
END $$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------
-- 3) inventory_batches
--    id, product_id, expiration_date
-- -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.inventory_batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    expiration_date DATE,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_inventory_batches_product_id
    ON public.inventory_batches(product_id);

CREATE INDEX IF NOT EXISTS idx_inventory_batches_expiration_date
    ON public.inventory_batches(expiration_date);

-- Composite index requested: (product_id, expiration_date)
CREATE INDEX IF NOT EXISTS idx_inventory_batches_product_exp
    ON public.inventory_batches(product_id, expiration_date);

-- -----------------------------------------------------------------
-- 4) inventory_movement_batches (intermediate table)
--    batch_id, inventory_movement_id, qty
-- -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.inventory_movement_batches (
    batch_id BIGINT NOT NULL REFERENCES public.inventory_batches(id) ON DELETE CASCADE,
    inventory_movement_id BIGINT NOT NULL REFERENCES public.inventory_movements(id) ON DELETE CASCADE,
    qty NUMERIC(10,2) NOT NULL,
    PRIMARY KEY (batch_id, inventory_movement_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_movement_batches_movement
    ON public.inventory_movement_batches(inventory_movement_id);

CREATE INDEX IF NOT EXISTS idx_inventory_movement_batches_batch
    ON public.inventory_movement_batches(batch_id);
