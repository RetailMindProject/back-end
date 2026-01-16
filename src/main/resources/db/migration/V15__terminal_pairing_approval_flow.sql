-- =========================================================
-- V15__terminal_pairing_approval_flow.sql
-- Store Manager Approval Pairing Flow (no code shown to cashier)
-- =========================================================

-- Step 1: Add new columns to terminal_pairing_codes
ALTER TABLE terminal_pairing_codes
ADD COLUMN IF NOT EXISTS requested_by BIGINT,
ADD COLUMN IF NOT EXISTS approved_by BIGINT,
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS request_token_hash TEXT,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';

-- Step 2: Backfill status for existing rows (correct logic)
UPDATE terminal_pairing_codes
SET status = CASE
    WHEN used_at IS NOT NULL THEN 'USED'
    WHEN expires_at < now() THEN 'EXPIRED'
    ELSE 'PENDING'
END
WHERE status IS NULL OR status = 'PENDING';

-- Step 3: Backfill request_token_hash for legacy rows
UPDATE terminal_pairing_codes
SET request_token_hash = md5(id::text || '-' || COALESCE(issued_at::text, now()::text))
WHERE request_token_hash IS NULL;

-- Step 4: Backfill requested_by from issued_by where available
UPDATE terminal_pairing_codes
SET requested_by = issued_by
WHERE requested_by IS NULL AND issued_by IS NOT NULL;

-- Step 5: Make columns NOT NULL after backfill
ALTER TABLE terminal_pairing_codes
ALTER COLUMN request_token_hash SET NOT NULL,
ALTER COLUMN status SET NOT NULL;

-- Step 6: Make code_hash nullable (not used in approval flow)
ALTER TABLE terminal_pairing_codes
ALTER COLUMN code_hash DROP NOT NULL;

-- Step 7: Drop old unique constraint (conflicting with new workflow)
DROP INDEX IF EXISTS ux_pairing_code_active_per_terminal;

-- Step 8: Add new partial unique constraint (at most one active request per terminal)
CREATE UNIQUE INDEX IF NOT EXISTS ux_pairing_request_active_per_terminal
ON terminal_pairing_codes(terminal_id)
WHERE status IN ('PENDING', 'APPROVED');

-- Step 9: Add CHECK constraint for status
ALTER TABLE terminal_pairing_codes
DROP CONSTRAINT IF EXISTS terminal_pairing_codes_status_check;

ALTER TABLE terminal_pairing_codes
ADD CONSTRAINT terminal_pairing_codes_status_check
CHECK (status IN ('PENDING', 'APPROVED', 'USED', 'EXPIRED', 'REJECTED'));

-- Step 10: Add foreign key for requested_by (nullable, ON DELETE RESTRICT)
ALTER TABLE terminal_pairing_codes
DROP CONSTRAINT IF EXISTS fk_terminal_pairing_codes_requested_by;

ALTER TABLE terminal_pairing_codes
ADD CONSTRAINT fk_terminal_pairing_codes_requested_by
FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE RESTRICT;

-- Step 11: Add foreign key for approved_by (nullable, ON DELETE SET NULL)
ALTER TABLE terminal_pairing_codes
DROP CONSTRAINT IF EXISTS fk_terminal_pairing_codes_approved_by;

ALTER TABLE terminal_pairing_codes
ADD CONSTRAINT fk_terminal_pairing_codes_approved_by
FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;

-- Step 12: Add helpful indexes for queries
CREATE INDEX IF NOT EXISTS ix_terminal_pairing_codes_terminal_status
ON terminal_pairing_codes(terminal_id, status);

CREATE INDEX IF NOT EXISTS ix_terminal_pairing_codes_requested_by_status
ON terminal_pairing_codes(requested_by, status)
WHERE requested_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_terminal_pairing_codes_request_token
ON terminal_pairing_codes(request_token_hash);
