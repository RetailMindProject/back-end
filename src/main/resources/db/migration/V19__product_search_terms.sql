-- =========================================================
-- V19__product_search_terms.sql
-- Add multilingual search terms table for Arabic/English product aliases
-- Enables queries like "حليب" (haleb/milk) to find "Milk" products
-- =========================================================

-- Step 1: Create product_search_terms table
CREATE TABLE IF NOT EXISTS public.product_search_terms (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    term TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_search_terms_product
        FOREIGN KEY (product_id)
        REFERENCES public.products(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE public.product_search_terms IS 'Multilingual search terms and aliases for products (e.g., Arabic "حليب" for English "Milk")';
COMMENT ON COLUMN public.product_search_terms.product_id IS 'Product this search term belongs to';
COMMENT ON COLUMN public.product_search_terms.term IS 'Search term or alias (e.g., "حليب", "لبن", "milk 1l")';

-- Step 2: Add unique constraint (case-insensitive term per product)
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_search_terms_product_term
    ON public.product_search_terms (product_id, LOWER(term));

-- Step 3: Add index for product lookup
CREATE INDEX IF NOT EXISTS idx_product_search_terms_product_id
    ON public.product_search_terms (product_id);

-- Step 4: Add trigram index for fuzzy search
-- pg_trgm extension already enabled in V18
CREATE INDEX IF NOT EXISTS idx_product_search_terms_term_trgm
    ON public.product_search_terms USING gin (term gin_trgm_ops);

-- =========================================================
-- Optional: Seed sample data for demo/testing
-- =========================================================
-- Add Arabic search terms for existing milk products (if any)
-- This is safe: only adds terms if products exist with 'milk' in name

DO $$
DECLARE
    milk_product RECORD;
BEGIN
    -- Find active products with 'milk' in name
    FOR milk_product IN
        SELECT id FROM public.products
        WHERE LOWER(name) LIKE '%milk%'
        AND is_active = true
        LIMIT 5
    LOOP
        -- Add Arabic term "حليب" (haleb/milk) if not exists
        INSERT INTO public.product_search_terms (product_id, term)
        VALUES (milk_product.id, 'حليب')
        ON CONFLICT (product_id, LOWER(term)) DO NOTHING;

        -- Add Arabic term "لبن" (laban/milk) if not exists
        INSERT INTO public.product_search_terms (product_id, term)
        VALUES (milk_product.id, 'لبن')
        ON CONFLICT (product_id, LOWER(term)) DO NOTHING;
    END LOOP;
END $$;

-- =========================================================
-- Migration complete: product_search_terms table ready
-- =========================================================

