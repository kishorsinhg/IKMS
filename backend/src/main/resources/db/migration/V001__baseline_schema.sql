CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actor_user_id UUID,
  client_id UUID,
  category VARCHAR(64) NOT NULL,
  action VARCHAR(128) NOT NULL,
  outcome VARCHAR(32) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(128),
  pii_access BOOLEAN NOT NULL DEFAULT FALSE,
  details JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_client_id ON audit_log (client_id);

COMMENT ON TABLE audit_log IS 'Baseline audit event store for later foundation and governance slices.';
COMMENT ON COLUMN audit_log.details IS 'Extensible event payload for audit metadata.';
