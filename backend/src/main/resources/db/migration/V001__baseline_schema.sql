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

CREATE TABLE app_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(80) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  email VARCHAR(255),
  status VARCHAR(32) NOT NULL,
  failed_login_count INTEGER NOT NULL DEFAULT 0,
  last_login_at TIMESTAMPTZ
);

CREATE TABLE app_user_role (
  user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  role_name VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role_name)
);

CREATE TABLE app_setting (
  setting_key VARCHAR(160) PRIMARY KEY,
  setting_value TEXT NOT NULL,
  description TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_status ON app_user (status);

COMMENT ON TABLE audit_log IS 'Baseline audit event store for later foundation and governance slices.';
COMMENT ON COLUMN audit_log.details IS 'Extensible event payload for audit metadata.';
COMMENT ON TABLE app_user IS 'Local authenticated users for IKMS foundation authentication.';
COMMENT ON TABLE app_setting IS 'Key-value registry for broker and system configuration.';
