ALTER TABLE ai_interaction
  ADD COLUMN operation_type VARCHAR(32) NOT NULL DEFAULT 'ASK',
  ADD COLUMN conversation_id UUID,
  ADD COLUMN provider_name VARCHAR(80),
  ADD COLUMN model_name VARCHAR(120),
  ADD COLUMN retrieval_mode VARCHAR(64),
  ADD COLUMN total_latency_ms BIGINT,
  ADD COLUMN prompt_tokens INTEGER,
  ADD COLUMN completion_tokens INTEGER,
  ADD COLUMN total_tokens INTEGER,
  ADD COLUMN grounding_score DOUBLE PRECISION,
  ADD COLUMN citation_coverage DOUBLE PRECISION,
  ADD COLUMN fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN warning_summary TEXT,
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE ai_conversation (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID REFERENCES client (id) ON DELETE CASCADE,
  actor_user_id UUID REFERENCES app_user (id) ON DELETE SET NULL,
  scope_type VARCHAR(32) NOT NULL,
  scope_id UUID,
  operation_type VARCHAR(32) NOT NULL,
  title VARCHAR(255),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_conversation_message (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES ai_conversation (id) ON DELETE CASCADE,
  role VARCHAR(32) NOT NULL,
  content TEXT,
  provider_name VARCHAR(80),
  model_name VARCHAR(120),
  status VARCHAR(32) NOT NULL DEFAULT 'READY',
  prompt_tokens INTEGER,
  completion_tokens INTEGER,
  total_tokens INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_retrieval_trace (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  interaction_id UUID REFERENCES ai_interaction (id) ON DELETE CASCADE,
  conversation_id UUID REFERENCES ai_conversation (id) ON DELETE CASCADE,
  client_id UUID REFERENCES client (id) ON DELETE CASCADE,
  retrieval_mode VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id UUID,
  page_number INTEGER,
  source_section VARCHAR(255),
  chunk_index INTEGER,
  lexical_score DOUBLE PRECISION,
  vector_score DOUBLE PRECISION,
  metadata_score DOUBLE PRECISION,
  relationship_score DOUBLE PRECISION,
  final_score DOUBLE PRECISION,
  citation_quality VARCHAR(32),
  permission_trimmed BOOLEAN NOT NULL DEFAULT FALSE,
  pii_masked BOOLEAN NOT NULL DEFAULT FALSE,
  prompt_injection_flagged BOOLEAN NOT NULL DEFAULT FALSE,
  retrieval_path VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_citation_record (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  interaction_id UUID REFERENCES ai_interaction (id) ON DELETE CASCADE,
  conversation_id UUID REFERENCES ai_conversation (id) ON DELETE CASCADE,
  source_type VARCHAR(32) NOT NULL,
  source_id UUID,
  title VARCHAR(255) NOT NULL,
  excerpt TEXT,
  page_number INTEGER,
  chunk_index INTEGER,
  source_section VARCHAR(255),
  confidence VARCHAR(32),
  evidence_text TEXT,
  jump_target_id VARCHAR(255),
  retrieval_path VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_orchestration_metric (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  interaction_id UUID REFERENCES ai_interaction (id) ON DELETE CASCADE,
  conversation_id UUID REFERENCES ai_conversation (id) ON DELETE CASCADE,
  operation_type VARCHAR(32) NOT NULL,
  total_latency_ms BIGINT,
  retrieval_latency_ms BIGINT,
  context_build_latency_ms BIGINT,
  provider_latency_ms BIGINT,
  grounding_score DOUBLE PRECISION,
  retrieval_precision DOUBLE PRECISION,
  citation_coverage DOUBLE PRECISION,
  answer_quality_score DOUBLE PRECISION,
  evidence_count INTEGER,
  warning_count INTEGER,
  fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
  provider_name VARCHAR(80),
  model_name VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_evaluation_result (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  interaction_id UUID REFERENCES ai_interaction (id) ON DELETE CASCADE,
  evaluation_type VARCHAR(64) NOT NULL,
  score DOUBLE PRECISION,
  outcome VARCHAR(32),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_interaction_conversation_id ON ai_interaction (conversation_id);
CREATE INDEX idx_ai_interaction_operation_type ON ai_interaction (operation_type);
CREATE INDEX idx_ai_conversation_client_id ON ai_conversation (client_id);
CREATE INDEX idx_ai_conversation_scope ON ai_conversation (scope_type, scope_id);
CREATE INDEX idx_ai_conversation_message_conversation_id ON ai_conversation_message (conversation_id, created_at);
CREATE INDEX idx_ai_retrieval_trace_interaction_id ON ai_retrieval_trace (interaction_id);
CREATE INDEX idx_ai_retrieval_trace_conversation_id ON ai_retrieval_trace (conversation_id);
CREATE INDEX idx_ai_retrieval_trace_client_id ON ai_retrieval_trace (client_id);
CREATE INDEX idx_ai_citation_record_interaction_id ON ai_citation_record (interaction_id);
CREATE INDEX idx_ai_citation_record_conversation_id ON ai_citation_record (conversation_id);
CREATE INDEX idx_ai_orchestration_metric_interaction_id ON ai_orchestration_metric (interaction_id);
CREATE INDEX idx_ai_evaluation_result_interaction_id ON ai_evaluation_result (interaction_id);
