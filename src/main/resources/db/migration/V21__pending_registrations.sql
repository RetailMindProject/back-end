-- =========================================================
--  V21__pending_registrations.sql
--  Pending Customer Registrations (Email Verification Required)
--  Author: POS System Team
--  Date: 2026-01-14
--  Description: Creates pending_registrations table to enforce
--               email verification BEFORE account creation for
--               new customer registrations.
-- =========================================================

-- =========================
--  Pending Registrations Table
-- =========================
-- Stores registration data until email is verified.
-- Once verified, user account is created and this record is deleted.
CREATE TABLE IF NOT EXISTS public.pending_registrations (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    verification_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,

    CONSTRAINT chk_pending_registrations_role
        CHECK (role IN ('CEO', 'STORE_MANAGER', 'INVENTORY_MANAGER', 'CASHIER', 'CUSTOMER'))
);

-- Unique constraint on email (only one pending registration per email)
CREATE UNIQUE INDEX IF NOT EXISTS uk_pending_registrations_email
    ON public.pending_registrations(email);

-- Index for token lookup
CREATE INDEX IF NOT EXISTS idx_pending_registrations_token_hash
    ON public.pending_registrations(verification_token_hash);

-- Index for cleanup queries (expired registrations)
CREATE INDEX IF NOT EXISTS idx_pending_registrations_expires_at
    ON public.pending_registrations(expires_at);

-- =========================
--  Comments for Documentation
-- =========================
COMMENT ON TABLE public.pending_registrations IS
    'Stores pending customer registrations awaiting email verification. User accounts are created ONLY after email verification.';

COMMENT ON COLUMN public.pending_registrations.verification_token_hash IS
    'SHA-256 hash of the verification token sent via email';

COMMENT ON COLUMN public.pending_registrations.expires_at IS
    'Verification link expiration (typically 24 hours from creation)';

COMMENT ON COLUMN public.pending_registrations.password_hash IS
    'BCrypt hashed password (will be transferred to users table after verification)';

-- =========================
--  Cleanup Function (Optional)
-- =========================
-- Function to clean up expired pending registrations
CREATE OR REPLACE FUNCTION cleanup_expired_pending_registrations()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM public.pending_registrations
    WHERE expires_at < now();

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_pending_registrations() IS
    'Deletes expired pending registrations. Returns count of deleted records. Should be called by scheduled task.';

