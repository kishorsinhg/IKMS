ALTER TABLE embedding_chunk
  ADD COLUMN document_id UUID,
  ADD COLUMN document_version_id UUID,
  ADD COLUMN document_type_id UUID,
  ADD COLUMN policy_number VARCHAR(120),
  ADD COLUMN claim_number VARCHAR(120),
  ADD COLUMN insurer VARCHAR(255),
  ADD COLUMN policy_type VARCHAR(120),
  ADD COLUMN effective_date DATE,
  ADD COLUMN expiry_date DATE,
  ADD COLUMN renewal_date DATE,
  ADD COLUMN broker_reference VARCHAR(120),
  ADD COLUMN external_reference VARCHAR(120),
  ADD COLUMN source_system VARCHAR(80),
  ADD COLUMN security_classification VARCHAR(64) NOT NULL DEFAULT 'STANDARD',
  ADD COLUMN acl_summary TEXT,
  ADD COLUMN content_hash VARCHAR(128),
  ADD COLUMN reindex_version INTEGER NOT NULL DEFAULT 2,
  ADD COLUMN indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_embedding_chunk_document_id ON embedding_chunk (document_id);
CREATE INDEX idx_embedding_chunk_document_version_id ON embedding_chunk (document_version_id);
CREATE INDEX idx_embedding_chunk_document_type_id ON embedding_chunk (document_type_id);
CREATE INDEX idx_embedding_chunk_policy_number ON embedding_chunk (policy_number);
CREATE INDEX idx_embedding_chunk_claim_number ON embedding_chunk (claim_number);
CREATE INDEX idx_embedding_chunk_insurer ON embedding_chunk (insurer);
CREATE INDEX idx_embedding_chunk_business_refs ON embedding_chunk (client_id, policy_number, claim_number, insurer);
CREATE INDEX idx_embedding_chunk_security_classification ON embedding_chunk (security_classification, client_id);
CREATE INDEX idx_embedding_chunk_reindex_version ON embedding_chunk (reindex_version, indexed_at DESC);

COMMENT ON COLUMN embedding_chunk.document_id IS 'Logical document lineage for version-aware retrieval and citation backtracking.';
COMMENT ON COLUMN embedding_chunk.document_version_id IS 'Specific document version used to create the indexed chunk.';
COMMENT ON COLUMN embedding_chunk.policy_number IS 'Structured business reference field extracted or reviewed from customer knowledge; not an IKMS Policy entity.';
COMMENT ON COLUMN embedding_chunk.claim_number IS 'Structured business reference field extracted or reviewed from customer knowledge; not an IKMS Claim entity.';
COMMENT ON COLUMN embedding_chunk.insurer IS 'Structured insurer reference indexed for customer-centric retrieval and filtering.';
COMMENT ON COLUMN embedding_chunk.security_classification IS 'Permission-aware retrieval classification for prompt-context filtering.';
COMMENT ON COLUMN embedding_chunk.reindex_version IS 'Index projection version used to support safe reindex migrations without changing source-of-record entities.';
