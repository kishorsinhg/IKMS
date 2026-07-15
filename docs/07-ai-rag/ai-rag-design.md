# AI And RAG Design

## Phase 5A Blueprint

The production target architecture for enterprise retrieval is documented in:

- `docs/06-architecture/phase-5a-enterprise-rag-blueprint.md`

That blueprint should be treated as the approved target architecture, not as a statement that every target component is already active in production.

## Current Implementation Status as of July 15, 2026

- PostgreSQL is the active retrieval storage platform.
- `pgvector` is the active vector retrieval implementation.
- `embedding_chunk` is the active retrieval projection.
- PostgreSQL-backed keyword, Business Reference Field, and structured retrieval paths are active.
- OpenSearch / Elasticsearch is not active and requires a future implementation phase.
- Phase 5C benchmark reporting should measure the current PostgreSQL and `pgvector` implementation rather than the future OpenSearch target.

## V1 AI Capabilities

- OCR/ICR for PDF where needed.
- DOCX text extraction.
- Document classification.
- Metadata extraction.
- PII detection assistance.
- Prompt injection detection.
- Embedding generation.
- Client-level RAG Q&A.

## Provider Strategy

The current baseline provider is configurable.

The Phase 5A target architecture assumes local or private-network model hosting for:

- embeddings
- reranking
- answer generation
- optional local intent classification

Mistral Cloud remains part of historical baseline context, not the target production dependency for the enterprise retrieval platform.

Configuration should include:

- Provider name
- API key/secret reference
- OCR model
- Chat model
- Embedding model
- Timeout and retry settings
- Enable/disable flags

## RAG Rules

- Client-level behavior is the current baseline.
- Enterprise customer-centric retrieval is the next architecture target.
- Retrieval must filter by Client before search.
- Retrieval must filter by user role and PII permissions before model context assembly.
- Use current document version by default.
- Use original OCR/email/note chunks as retrieval source.
- AI answer must cite sources.
- If no evidence is found, answer must say no evidence was found.
- Decision-making requests must be refused.
- Policy Number, Claim Number, Insurer, Effective Date, Expiry Date, Renewal Date, and similar insurance attributes are Business Reference Fields, not IKMS-owned entities.

## Phase 5C Benchmark Alignment

Phase 5C benchmark coverage should measure the current implementation:

- PostgreSQL keyword retrieval
- `pgvector` retrieval
- combined hybrid retrieval
- reranked retrieval
- vector-unavailable degradation behavior
- stale projection behavior
- projection rebuild and reindex behavior

OpenSearch should not appear in current benchmark results. It may appear only as future evaluation scope.

## Language Support

- UI is English only.
- Documents and RAG must support English and German content.
- Embedding model must support English and German.
