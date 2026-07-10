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

CREATE TABLE email_item (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID REFERENCES client (id) ON DELETE SET NULL,
  mailbox_config_id UUID,
  message_id VARCHAR(255) NOT NULL UNIQUE,
  thread_id VARCHAR(255),
  subject VARCHAR(255) NOT NULL,
  sender VARCHAR(255) NOT NULL,
  recipients TEXT NOT NULL,
  cc TEXT,
  received_at TIMESTAMPTZ NOT NULL,
  body_text TEXT,
  body_html_storage_path VARCHAR(512),
  processing_status VARCHAR(32) NOT NULL,
  review_status VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID REFERENCES client (id) ON DELETE SET NULL,
  parent_email_id UUID REFERENCES email_item (id) ON DELETE SET NULL,
  document_type_id UUID,
  title VARCHAR(255) NOT NULL,
  current_version_id UUID,
  processing_status VARCHAR(32) NOT NULL,
  review_status VARCHAR(32) NOT NULL,
  client_match_confidence NUMERIC(5, 4),
  classification_confidence NUMERIC(5, 4),
  extraction_confidence NUMERIC(5, 4),
  source VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_version (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL REFERENCES document (id) ON DELETE CASCADE,
  version_number INTEGER NOT NULL,
  file_hash VARCHAR(128) NOT NULL UNIQUE,
  file_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(160) NOT NULL,
  file_size_bytes BIGINT NOT NULL,
  original_storage_path VARCHAR(512) NOT NULL,
  redacted_storage_path VARCHAR(512),
  extracted_text TEXT,
  ocr_provider VARCHAR(120),
  embedding_model VARCHAR(120),
  language VARCHAR(32),
  redaction_status VARCHAR(32) NOT NULL,
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by UUID
);

CREATE TABLE review_queue_item (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  item_type VARCHAR(32) NOT NULL,
  item_id VARCHAR(128) NOT NULL,
  reason VARCHAR(64) NOT NULL,
  assigned_to UUID,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMPTZ
);

CREATE TABLE embedding_chunk (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID NOT NULL REFERENCES client (id) ON DELETE CASCADE,
  source_type VARCHAR(32) NOT NULL,
  source_id UUID NOT NULL,
  chunk_text TEXT NOT NULL,
  embedding_reference VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_interaction (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID NOT NULL REFERENCES client (id) ON DELETE CASCADE,
  question TEXT NOT NULL,
  answer TEXT,
  status VARCHAR(32) NOT NULL,
  cited_sources TEXT,
  helpful_feedback BOOLEAN,
  feedback_comment TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_status ON app_user (status);
CREATE INDEX idx_client_display_name ON client (display_name);
CREATE INDEX idx_client_status ON client (status);
CREATE INDEX idx_note_client_created_at ON note (client_id, created_at DESC);
CREATE INDEX idx_email_item_client_id ON email_item (client_id);
CREATE INDEX idx_document_client_id ON document (client_id);
CREATE INDEX idx_document_parent_email_id ON document (parent_email_id);
CREATE INDEX idx_review_queue_status_reason ON review_queue_item (status, reason);
CREATE INDEX idx_embedding_chunk_client_id ON embedding_chunk (client_id, created_at DESC);
CREATE INDEX idx_ai_interaction_client_id ON ai_interaction (client_id, created_at DESC);

COMMENT ON TABLE audit_log IS 'Baseline audit event store for later foundation and governance slices.';
COMMENT ON COLUMN audit_log.details IS 'Extensible event payload for audit metadata.';
COMMENT ON TABLE app_user IS 'Local authenticated users for IKMS foundation authentication.';
COMMENT ON TABLE app_setting IS 'Key-value registry for broker and system configuration.';
COMMENT ON TABLE client IS 'Fixed master client profile for the IKMS V1 broker workspace.';
COMMENT ON TABLE note IS 'Manual client-linked notes with audit-friendly lifecycle fields.';
COMMENT ON TABLE email_item IS 'Mailbox-derived email knowledge items for client-linked intake.';
COMMENT ON TABLE document IS 'Logical document records linked to clients and optional parent emails.';
COMMENT ON TABLE document_version IS 'Preserved document file versions and extraction/redaction state.';
COMMENT ON TABLE review_queue_item IS 'Human review tasks for intake, extraction, linking, and duplicate exceptions.';
COMMENT ON TABLE embedding_chunk IS 'Client-scoped text chunks reserved for keyword/vector retrieval workflows.';
COMMENT ON TABLE ai_interaction IS 'Client-level AI question and feedback history.';
