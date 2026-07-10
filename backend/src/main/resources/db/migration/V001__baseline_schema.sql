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

CREATE TABLE client (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id VARCHAR(80) NOT NULL UNIQUE,
  client_id_temporary BOOLEAN NOT NULL DEFAULT FALSE,
  client_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  display_name VARCHAR(200) NOT NULL,
  legal_name VARCHAR(200),
  primary_email VARCHAR(255),
  secondary_email VARCHAR(255),
  primary_phone VARCHAR(64),
  secondary_phone VARCHAR(64),
  contact_person VARCHAR(255),
  address_line_1 VARCHAR(255),
  address_line_2 VARCHAR(255),
  city VARCHAR(120),
  state_or_region VARCHAR(120),
  postal_code VARCHAR(32),
  country VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE note (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID NOT NULL REFERENCES client (id) ON DELETE CASCADE,
  note_text VARCHAR(4000) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by UUID
);

CREATE INDEX idx_app_user_status ON app_user (status);
CREATE INDEX idx_client_display_name ON client (display_name);
CREATE INDEX idx_client_status ON client (status);
CREATE INDEX idx_note_client_created_at ON note (client_id, created_at DESC);

COMMENT ON TABLE audit_log IS 'Baseline audit event store for later foundation and governance slices.';
COMMENT ON COLUMN audit_log.details IS 'Extensible event payload for audit metadata.';
COMMENT ON TABLE app_user IS 'Local authenticated users for IKMS foundation authentication.';
COMMENT ON TABLE app_setting IS 'Key-value registry for broker and system configuration.';
COMMENT ON TABLE client IS 'Fixed master client profile for the IKMS V1 broker workspace.';
COMMENT ON TABLE note IS 'Manual client-linked notes with audit-friendly lifecycle fields.';
