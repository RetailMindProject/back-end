-- =========================================================
--  V20__email_verification_and_password_reset.sql
--  Email Verification & Password Reset Support
--  Author: POS System Team
--  Date: 2026-01-14
--  Description: Adds email verification tracking and secure
--               token-based password reset functionality
-- =========================================================

-- =========================
--  Add Email Verification to Users
-- =========================
-- Track whether a user's email has been verified
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE NOT NULL;

-- Track when the email was verified (NULL if not verified)
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;

-- Index for querying unverified users
CREATE INDEX IF NOT EXISTS idx_users_email_verified
ON public.users(email_verified) WHERE email_verified = FALSE;

-- =========================
--  Verification Tokens Table
-- =========================
-- Stores hashed email verification tokens with expiration
CREATE TABLE IF NOT EXISTS public.verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    -- Store SHA-256 hash of the token (64 hex chars)
    token_hash VARCHAR(64) NOT NULL,
    -- Type: EMAIL_VERIFICATION, PASSWORD_RESET, etc.
    token_type VARCHAR(30) NOT NULL CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    -- Token expiration timestamp
    expires_at TIMESTAMP NOT NULL,
    -- Track if token has been used (NULL = unused, timestamp = used)
    used_at TIMESTAMP,
    -- Track token creation
    created_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Unique constraint: Only ONE active (unused) token per user per type
-- Note: We use a partial unique index instead of a UNIQUE constraint because
-- PostgreSQL treats NULL values as distinct in UNIQUE constraints, which would
-- allow multiple unused tokens (used_at IS NULL) for the same user/type.
-- The partial index only includes rows where used_at IS NULL, correctly
-- enforcing "one active token per user per type" while allowing multiple used tokens.
CREATE UNIQUE INDEX IF NOT EXISTS uk_verification_tokens_user_type_active
ON public.verification_tokens(user_id, token_type)
WHERE used_at IS NULL;

-- Index for efficient token lookup (primary use case)
CREATE INDEX IF NOT EXISTS idx_verification_tokens_hash
ON public.verification_tokens(token_hash);

-- Index for expiration cleanup queries
CREATE INDEX IF NOT EXISTS idx_verification_tokens_expires_at
ON public.verification_tokens(expires_at);

-- Index for user token lookup
CREATE INDEX IF NOT EXISTS idx_verification_tokens_user_id
ON public.verification_tokens(user_id);

-- Composite index for common query pattern: find unused, non-expired tokens
-- This is a partial index covering only active (unused) tokens for better performance
CREATE INDEX IF NOT EXISTS idx_verification_tokens_active
ON public.verification_tokens(token_hash, token_type, expires_at)
WHERE used_at IS NULL;

-- =========================
--  Audit: Password Reset Log
-- =========================
-- Optional: Track password reset attempts for security auditing
CREATE TABLE IF NOT EXISTS public.password_reset_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    -- IP address from which reset was requested/completed
    ip_address VARCHAR(45),
    -- User agent string
    user_agent VARCHAR(255),
    -- Status: REQUESTED, COMPLETED, FAILED, EXPIRED
    status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED', 'EXPIRED')),
    -- Additional context/reason for failure
    notes TEXT,
    created_at TIMESTAMP DEFAULT now() NOT NULL
);

-- Index for security analysis queries
CREATE INDEX IF NOT EXISTS idx_password_reset_log_user_id
ON public.password_reset_log(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_password_reset_log_ip
ON public.password_reset_log(ip_address, created_at DESC);

-- =========================
--  Comments for Documentation
-- =========================
COMMENT ON COLUMN public.users.email_verified IS 'Indicates whether the user has verified their email address';
COMMENT ON COLUMN public.users.email_verified_at IS 'Timestamp when the email was verified (NULL if not verified)';

COMMENT ON TABLE public.verification_tokens IS 'Stores hashed tokens for email verification and password reset with expiration';
COMMENT ON COLUMN public.verification_tokens.token_hash IS 'SHA-256 hash of the actual token (never store plain tokens)';
COMMENT ON COLUMN public.verification_tokens.token_type IS 'Type of verification: EMAIL_VERIFICATION or PASSWORD_RESET';
COMMENT ON COLUMN public.verification_tokens.expires_at IS 'Token expiration timestamp (typically 24h for email, 1h for password reset)';
COMMENT ON COLUMN public.verification_tokens.used_at IS 'Timestamp when token was used (NULL if unused)';

COMMENT ON TABLE public.password_reset_log IS 'Audit log for password reset attempts and completions';
COMMENT ON COLUMN public.password_reset_log.status IS 'REQUESTED: Token sent, COMPLETED: Password changed, FAILED: Invalid attempt, EXPIRED: Token expired';

-- =========================
--  Cleanup Function (Optional)
-- =========================
-- Function to clean up expired tokens (can be called by scheduled job)
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM public.verification_tokens
    WHERE expires_at < now()
      AND used_at IS NULL;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_tokens() IS 'Deletes expired, unused verification tokens. Returns count of deleted records.';

