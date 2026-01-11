-- =========================================================
-- V16__returns_and_refunds.sql
-- Add support for partial returns and refund tracking
--
-- Business Rules:
-- - Return orders have status='RETURNED' (not PAID)
-- - All amounts (refund_amount, payment.amount) are POSITIVE
-- - Return order totals are POSITIVE (represent refunded amounts)
-- - Refunds recorded as payments(type='REFUND', amount positive)
-- - No customer registration during returns
-- =========================================================

-- Step 1: Extend orders.status to include PARTIALLY_RETURNED
ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE public.orders
    ADD CONSTRAINT orders_status_check
        CHECK (status IN ('DRAFT','PAID','CANCELLED','RETURNED','HOLD','PARTIALLY_RETURNED'));

-- Step 2: Create return_items table
CREATE TABLE IF NOT EXISTS public.return_items (
    id BIGSERIAL PRIMARY KEY,
    return_order_id BIGINT NOT NULL,
    original_order_item_id BIGINT NOT NULL,
    returned_qty NUMERIC(10,2) NOT NULL CHECK (returned_qty > 0),
    refund_amount NUMERIC(12,2) NOT NULL CHECK (refund_amount >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_return_items_return_order
        FOREIGN KEY (return_order_id) REFERENCES public.orders(id) ON DELETE CASCADE,

    CONSTRAINT fk_return_items_original_item
        FOREIGN KEY (original_order_item_id) REFERENCES public.order_items(id) ON DELETE RESTRICT,

    CONSTRAINT uq_return_items_per_order
        UNIQUE (return_order_id, original_order_item_id)
);

COMMENT ON TABLE public.return_items IS 'Tracks which items from original orders were returned and in what quantities. Return orders have status=RETURNED.';
COMMENT ON COLUMN public.return_items.return_order_id IS 'The return order (orders.parent_order_id points to original, status=RETURNED)';
COMMENT ON COLUMN public.return_items.original_order_item_id IS 'The original order_item being returned';
COMMENT ON COLUMN public.return_items.returned_qty IS 'Quantity being returned (positive, must be > 0 and <= remaining qty)';
COMMENT ON COLUMN public.return_items.refund_amount IS 'Refund amount for this item (positive, calculated from original prices)';

-- Step 3: Create indexes for return_items
CREATE INDEX IF NOT EXISTS idx_return_items_return_order
    ON public.return_items(return_order_id);

CREATE INDEX IF NOT EXISTS idx_return_items_original_item
    ON public.return_items(original_order_item_id);

-- Step 4: Add type column to payments to distinguish payments from refunds
ALTER TABLE public.payments
    ADD COLUMN IF NOT EXISTS type VARCHAR(10) NOT NULL DEFAULT 'PAYMENT';

-- Step 5: Add CHECK constraint for payment type
ALTER TABLE public.payments
    DROP CONSTRAINT IF EXISTS payments_type_check;

ALTER TABLE public.payments
    ADD CONSTRAINT payments_type_check
        CHECK (type IN ('PAYMENT','REFUND'));

COMMENT ON COLUMN public.payments.type IS 'PAYMENT for customer payments, REFUND for customer refunds. All amounts are POSITIVE numbers.';

-- Step 6: Create index for payment queries by type
CREATE INDEX IF NOT EXISTS idx_payments_order_type
    ON public.payments(order_id, type);

-- Step 7: Create index for refund reporting
CREATE INDEX IF NOT EXISTS idx_payments_type_created
    ON public.payments(type, created_at)
    WHERE type = 'REFUND';

