CREATE OR REPLACE FUNCTION trg_touch_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE products
  ADD COLUMN IF NOT EXISTS unit VARCHAR(20),
  ADD COLUMN IF NOT EXISTS wholesale_price NUMERIC(12,2) CHECK (wholesale_price IS NULL OR wholesale_price >= 0);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    from_user_id INT REFERENCES users(id) ON DELETE SET NULL,
    to_user_id INT REFERENCES users(id) ON DELETE SET NULL,
    to_customer_id INT REFERENCES customers(id) ON DELETE SET NULL,
    parent_id INT REFERENCES messages(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) CHECK (status IN ('SENT','READ','ARCHIVED')) DEFAULT 'SENT',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    CHECK (to_user_id IS NOT NULL OR to_customer_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_messages_to_user ON messages(to_user_id, status);
CREATE INDEX IF NOT EXISTS idx_messages_to_customer ON messages(to_customer_id, status);
CREATE INDEX IF NOT EXISTS idx_messages_from_user ON messages(from_user_id);
CREATE INDEX IF NOT EXISTS idx_messages_parent ON messages(parent_id);

DROP TRIGGER IF EXISTS trg_messages_touch ON messages;
CREATE TRIGGER trg_messages_touch
BEFORE UPDATE ON messages
FOR EACH ROW EXECUTE FUNCTION trg_touch_updated_at();
