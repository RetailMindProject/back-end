-- =========================================================
-- V17__extend_order_number_length.sql
-- Extend orders.order_number length to support return orders
-- =========================================================

-- v_cash_book depends on orders.order_number (ref_code). PostgreSQL does not allow
-- altering the column type while a view depends on it.
DROP VIEW IF EXISTS public.v_cash_book;

ALTER TABLE public.orders
ALTER COLUMN order_number TYPE VARCHAR(60);

-- Recreate v_cash_book (keep consistent with the latest definition in V9__convert_int_to_bigint.sql)
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