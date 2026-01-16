BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS ux_terminals_code
ON terminals(code);

ALTER TABLE terminals
ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_sessions_open_per_terminal
ON sessions(terminal_id)
WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS ix_sessions_terminal_status
ON sessions(terminal_id, status);

CREATE TABLE IF NOT EXISTS terminal_devices (
    id           BIGSERIAL PRIMARY KEY,
    terminal_id  BIGINT NOT NULL REFERENCES terminals(id) ON DELETE CASCADE,
    token_hash   TEXT NOT NULL UNIQUE,
    issued_at    TIMESTAMP NOT NULL DEFAULT now(),
    revoked_at   TIMESTAMP NULL,
    last_seen_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS ix_terminal_devices_active
ON terminal_devices(terminal_id)
WHERE revoked_at IS NULL;

CREATE TABLE IF NOT EXISTS terminal_pairing_codes (
    id          BIGSERIAL PRIMARY KEY,
    terminal_id BIGINT NOT NULL REFERENCES terminals(id) ON DELETE CASCADE,
    code_hash   TEXT NOT NULL,
    issued_at   TIMESTAMP NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP NULL,
    issued_by   BIGINT NULL REFERENCES users(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_pairing_code_active_per_terminal
ON terminal_pairing_codes(terminal_id)
WHERE used_at IS NULL;

COMMIT;
