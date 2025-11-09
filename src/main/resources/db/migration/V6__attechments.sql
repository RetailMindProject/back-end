CREATE TABLE IF NOT EXISTS message_attachments (
    id SERIAL PRIMARY KEY,
    message_id INT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT CHECK (file_size IS NULL OR file_size >= 0),
    checksum VARCHAR(64),
    uploaded_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_message_attachments_message ON message_attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_message_attachments_uploaded_at ON message_attachments(uploaded_at);

DROP TRIGGER IF EXISTS trg_message_attachments_touch ON message_attachments;
CREATE TRIGGER trg_message_attachments_touch
BEFORE UPDATE ON message_attachments
FOR EACH ROW EXECUTE FUNCTION trg_touch_updated_at();
