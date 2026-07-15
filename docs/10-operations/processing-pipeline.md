# Processing Pipeline

## Intake Sources

- Manual upload
- Shared folder polling
- IMAP email polling

## Pipeline

1. Detect or receive item.
2. Validate file type.
3. Calculate hash.
4. Check duplicate.
5. Store original file.
6. Create intake item.
7. Extract text/OCR.
8. Classify document type.
9. Extract metadata.
10. Detect PII and prompt injection risk.
11. Generate chunks and embeddings.
12. Persist first-class Business Reference Fields such as policy number, claim number, insurer, policy type, effective date, expiry date, renewal date, broker reference, and external reference as searchable customer knowledge attributes.
12. Generate redacted copy if needed.
13. Match or suggest Client.
14. Route to review queue when needed.
15. Release to Client profile after approved/linking.

## Phase 7 Processing Lifecycle

The current implementation now persists a document-processing job projection for operational review and retry handling.

Current lifecycle states:

- `INTAKE_RECEIVED`
- `VIRUS_SCANNED`
- `EXTRACTING`
- `OCR_COMPLETE`
- `CLASSIFIED`
- `VALIDATED`
- `WAITING_REVIEW`
- `APPROVED`
- `PUBLISHED`
- `INDEXED`
- `FAILED`

Operational notes:

- Virus scan, OCR/text extraction, language detection, classification, metadata extraction, Business Reference extraction, validation, and confidence aggregation are modular stages.
- Stage outputs are persisted into processing fields and findings so Review can display source lineage, confidence, and validation state without recomputing the pipeline on every page load.
- Low-confidence or failed runs remain review-driven; they are never auto-approved.
- Approval republishes authorized knowledge into the retrieval projection, related-knowledge graph, and customer timeline asynchronously.
- Retry is available only for document-backed review items with a processing job and should be treated as an auditable operational action.

## Review And Publishing Recovery

- `PROCESSING_FAILED` review items indicate the pipeline could not complete automatically.
- `WAITING_REVIEW` indicates the pipeline completed but requires human confirmation or correction.
- Reviewer metadata corrections remain traceable through extracted, corrected, and approved values.
- Publishing should be retried from persisted processing and review state rather than by editing retrieval projections directly.

## Retrieval Boundary

Policy Number, Claim Number, Insurer, Effective Date, Expiry Date, Renewal Date, and similar insurance values are structured Business Reference Fields within IKMS.

They are searchable, filterable, and indexable, but they do not create Policy or Claim entities.

The broker management system remains the system of record for policy and claim lifecycle operations.

## Reindex Guidance

Customer-centric retrieval now projects first-class Business Reference Fields into `embedding_chunk` for hybrid search and evidence ranking.

Operational expectations:

- `embedding_chunk.reindex_version` identifies the active projection schema used for retrieval.
- New ingestion writes indexed business-reference columns such as `policy_number`, `claim_number`, `insurer`, `policy_type`, `effective_date`, `expiry_date`, `renewal_date`, `broker_reference`, and `external_reference`.
- Schema changes that affect retrieval projection should increment the reindex version and trigger controlled reindex planning for affected customer knowledge sources.
- Benchmark and evaluation runs should compare like-for-like projection versions; do not mix legacy and current chunk projections when validating retrieval quality.

## Implementation Status as of July 15, 2026

Current active retrieval projection behavior:

- PostgreSQL `embedding_chunk` is the live retrieval projection used by the backend retrieval platform.
- `EmbeddingIndexService` generates and persists embeddings into `embedding_chunk.embedding_vector`.
- Query-time retrieval combines vector search, keyword search, Business Reference Field search, and structured filtering over persisted projections.
- The system does not parse original documents during every query.
- When embeddings or vector retrieval are unavailable, the platform degrades to keyword, Business Reference Field, and structured retrieval while surfacing diagnostics or warnings.

Operational implications:

- stale projections can reduce retrieval quality even when source artifacts are correct
- reindex planning should be part of schema evolution, metadata extraction changes, and embedding-model changes
- recovery should prefer projection rebuild from canonical customer knowledge artifacts rather than manual projection edits
- monitoring should include fallback-rate increases, stale projection detection, and reindex completion status

OpenSearch / Elasticsearch operational procedures are not active in the current implementation and should be treated as future architecture only.

## Background Jobs

Use PostgreSQL-backed job tables for V1. Avoid Kafka/RabbitMQ unless future scale requires it.

## Customer Knowledge Timeline And Related Knowledge

Current Phase 6 behavior:

- Timeline events are assembled dynamically from canonical customer knowledge artifacts, review activity, metadata values, and AI conversation records.
- Related Knowledge links are assembled dynamically from canonical lineage, email attachment linkage, shared Business Reference Fields, document/version duplication signals, and existing PostgreSQL plus `pgvector` similarity queries.
- No dedicated Policy or Claim projection is introduced.
- No standalone timeline projection is required for the current implementation.

Operational expectations:

- Timeline and related-knowledge responses inherit the same customer-scope and source-scope authorization rules as the underlying artifacts.
- Business Reference Field corrections appear through metadata-derived timeline events rather than through policy or claim lifecycle models.
- Similarity-based related links are inferred and should be monitored separately from deterministic lineage and reference matches if operator telemetry is extended later.

## Knowledge Quality Revalidation

Phase 8 adds a stewardship-oriented quality pass over published customer knowledge.

Operational expectations:

- customer knowledge quality reads canonical customer-linked knowledge and existing processing/review projections
- quality snapshots and issue queues are rebuildable projections, not canonical source records
- revalidation recomputes customer quality scores and issue detection without introducing new policy or claim lifecycle data
- reindex uses the existing publishing and embedding-index pipeline to refresh retrieval readiness after stewardship corrections
- bulk corrections must remain auditable and human-triggered
- quality workflows must preserve the current product boundary that Policy and Claim are Business Reference Fields only
