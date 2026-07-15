# Customer-Centric Enterprise Retrieval

## Purpose

This document defines the backend retrieval-platform design and the current implementation status for the customer-centric enterprise retrieval platform.

It extends the client-scoped RAG and orchestration foundation into a customer-centric enterprise retrieval platform that powers:

- Global Search
- Customer360
- Enterprise AI Assistant
- Review Detail
- AI Evidence Workspace
- Enterprise Document Viewer

## Mandatory Boundary

IKMS is not:

- a Broker Management System
- a Policy Administration System
- a Claims Management System
- an Underwriting System

The broker management platform remains the system of record.

Customer is the primary knowledge context in IKMS.

Policy Number, Claim Number, Insurer, Effective Date, Expiry Date, Renewal Date, Broker Reference, External System Reference, and similar insurance values are structured Business Reference Fields within IKMS.

They are:

- searchable
- filterable
- indexable
- citeable as supporting attributes

They are not:

- Policy entities
- Claim entities
- lifecycle-managed records
- standalone operational workspaces

## Implementation Status as of July 15, 2026

| Capability | Current Status | Active Implementation | Future Direction |
| --- | --- | --- | --- |
| Canonical Knowledge Storage | Active | Customer knowledge artifacts | Unchanged |
| Retrieval Projection | Active | PostgreSQL `embedding_chunk` | Continue |
| Vector Retrieval | Active | PostgreSQL `pgvector` | Evaluate coexistence |
| Keyword Retrieval | Active | PostgreSQL retrievers | OpenSearch candidate |
| Structured Filtering | Active | PostgreSQL | OpenSearch candidate |
| Business Reference Search | Active | PostgreSQL indexed fields | Continue |
| OpenSearch | Planned | Not active | Future lexical engine |
| Reindexing | Active | Projection rebuild | Extend |
| Consistency | Active | Eventual | Preserve |

## Current Implementation

The current backend already provides:

- PostgreSQL-backed chunk retrieval through the live `embedding_chunk` projection
- active `pgvector` nearest-neighbour retrieval over persisted query and chunk embeddings
- lexical, metadata, business-reference, date-filtered, version-aware, and relationship-aware PostgreSQL retrieval paths
- provider-backed embedding generation and projection population through `EmbeddingIndexService`
- fallback from vector retrieval to keyword, Business Reference Field, and structured-filter retrieval when embeddings or vector lookup are unavailable
- retrieval diagnostics and warnings that surface degraded retrieval modes instead of failing silently
- `reindex_version` tracking on `embedding_chunk` for projection compatibility and rebuild detection

OpenSearch / Elasticsearch is planned only. It is not the active retrieval engine in the current backend implementation.

## Current Baseline

The current implementation baseline already provides:

- customer-scoped lexical, metadata, vector, and relationship retrieval
- provider-backed extraction and embeddings
- enterprise orchestration scaffolding for ask, summarize, explain, compare, extract, and validate
- prompt-injection inspection, PII trimming, citation modeling, and orchestration metrics

Current gaps relative to the target phase:

- retrieval is still anchored to one-customer search rather than a provider-independent typed query plan
- OpenSearch / Elasticsearch is not yet implemented as a lexical or structured retrieval engine
- the backend still assumes remote-provider execution paths rather than a local-model-first deployment target

## Core Domain Context

The retrieval platform should treat the following as the enterprise knowledge graph inside IKMS:

```text
Customer
  -> Documents
  -> Document Versions
  -> OCR Text
  -> Extracted Fields
  -> Emails
  -> Notes
  -> Reviews
  -> AI Conversations
  -> Timeline
  -> Business Reference Fields
  -> AI Knowledge
```

Policy and claim references are resolved through customer-linked documents, emails, extracted fields, notes, reviews, and AI conversation history.

## End-To-End Retrieval Pipeline

```text
User Question
  -> Authentication
  -> Authorization
  -> Prompt Injection Detection
  -> Intent Classification
  -> Business Reference Extraction
  -> Typed Query Planner
  -> Hybrid Retrieval
  -> Fusion
  -> Reranking
  -> Context Builder
  -> On-Prem LLM
  -> Grounding Validation
  -> Citation Builder
  -> Audit Logging
  -> Streaming Response
```

## Supported Knowledge Sources

The retrieval layer must support:

- Documents
- Document Versions
- OCR Text
- Extracted Fields
- Emails
- Notes
- Review Records
- Customer Attributes
- AI Conversation History
- Business Reference Fields

Unsupported sources must not be fabricated into the retrieval plan.

## Business Reference Extraction

Business reference extraction is a planning step, not an entity lookup step.

Example:

```text
Question: Summarize documents for policy POL-12345

Plan output:
  Scope: Customer
  BusinessReferenceFields:
    PolicyNumber = POL-12345
```

Example:

```text
Question: Find correspondence for claim CLM-9988

Plan output:
  Scope: Customer
  BusinessReferenceFields:
    ClaimNumber = CLM-9988
```

No policy table, claim table, policy CRUD API, or claim CRUD API should be introduced to support this behavior.

## Typed Query Plan

The provider-independent query plan should support:

- customer scope
- query text
- source types
- document types
- date range
- business reference fields
- result limits
- sort order
- version preference
- required evidence granularity

Business Reference Fields should be modeled explicitly:

- `PolicyNumber`
- `ClaimNumber`
- `Insurer`
- `PolicyType`
- `EffectiveDate`
- `ExpiryDate`
- `RenewalDate`
- `BrokerReference`
- `ExternalReference`

These fields should not be hidden inside an opaque metadata blob when indexing or planning.

## Search Index Target

The enterprise search index should be customer-centric and document-centric at the same time.

Suggested indexed fields:

```text
CustomerId
DocumentId
DocumentVersionId
DocumentType
PolicyNumber
ClaimNumber
Insurer
PolicyType
EffectiveDate
ExpiryDate
RenewalDate
ReceivedDate
CreatedDate
SourceSystem
OCRText
ChunkText
Embedding
SecurityClassification
ACL
ContentHash
```

Policy Number and Claim Number remain supporting searchable fields. They never become the primary source record.

## Active Retrieval Storage and Projection Lifecycle

Current implementation details:

- PostgreSQL is the active retrieval storage platform.
- `embedding_chunk` is the live retrieval projection, not the canonical source of truth.
- The projection contains chunk text, embeddings, source lineage, customer scope, Business Reference Fields, document version identifiers, content hash, security classification, ACL summary, and `reindex_version`.
- The canonical source remains documents, document versions, emails, notes, OCR text, extracted fields, reviewed values, customer associations, and Business Reference Fields.
- The retrieval projection is rebuildable through reindexing and should be treated as derived state.

## Active Retrievers

The current implementation uses:

- PostgreSQL keyword and lexical retrievers
- PostgreSQL metadata and structured-field retrievers
- PostgreSQL Business Reference Field filtering and matching
- PostgreSQL `pgvector` nearest-neighbour retrieval over `embedding_chunk.embedding_vector`
- relationship-aware and version-aware retrieval over persisted customer knowledge projections

When vector retrieval fails or embeddings are unavailable, retrieval degrades gracefully to:

- keyword retrieval
- Business Reference Field search
- structured filtering

This degraded mode must remain visible through retrieval diagnostics or warnings.

## Retrieval Strategy

Hybrid retrieval must support:

- lexical search
- vector search
- structured-field search
- business-reference search
- customer search
- date filtering
- document-type filtering
- version-aware retrieval

The implementation should review the current search stack and align it to the approved architecture. If OpenSearch or Elasticsearch is introduced or already present in environment-specific deployment, it should still preserve the same customer-centric field model and ACL behavior.

OpenSearch or Elasticsearch should be treated as planned future architecture only in current documentation unless explicitly discussing roadmap or migration.

No policy index or claim index should be introduced.

## Reindexing and Consistency

Current implementation uses eventual consistency.

Operationally:

- retrieval reads from persisted retrieval projections
- the system does not parse original documents during every query
- `reindex_version` tracks retrieval projection schema compatibility
- stale projections must be detected and corrected through rebuild or targeted reindexing
- benchmark comparisons should not mix different projection versions without explicit controls

## Diagnostics and Limitations

Current implementation characteristics:

- vector retrieval warnings are surfaced when the embedding provider or `pgvector` query path is unavailable
- degraded retrieval remains functional through keyword, Business Reference Field, and structured retrieval
- retrieval quality depends on projection freshness and embedding availability
- OpenSearch-scale lexical and structured retrieval behavior is not part of current production-active implementation

## Fusion And Reranking

The retrieval layer should combine lexical and vector candidates through reciprocal rank fusion or an equivalent documented strategy.

Required behaviors:

- duplicate removal
- chunk lineage preservation
- version awareness
- source diversity
- customer-aware ranking
- freshness handling
- evidence quality weighting

Cloud-only reranking dependencies are out of bounds.

## Context Builder

The context builder should include only:

- user question
- customer context
- retrieved evidence
- business reference fields
- conversation history
- token budget
- guardrail instructions

Restricted documents must never enter prompt context.

## Grounded Answer Contract

The answer contract should return:

- answer
- sources
- evidence
- citations
- grounding status
- warnings
- restricted content notice
- conversation id
- audit correlation id

If evidence is insufficient, the response should fail safely with:

```text
Insufficient evidence to answer.
```

## Citation Model

Citations should point to actual evidence sources:

- document
- document version
- page
- chunk
- email
- note
- review record
- future OCR region

Business reference fields should appear only as supporting attributes inside the citation payload.

Example:

```text
Document
Page 4
Policy Number: POL-12345
Claim Number: CLM-9988
```

The document is the evidence source. The business reference fields are supporting attributes.

## Deployment Direction

The target phase is local-model and on-prem friendly.

Architecture assumptions:

- no public-cloud dependency is required for steady-state RAG execution
- model abstraction must allow local LLM providers
- streaming, cancellation, retry, timeout, and fallback are orchestration concerns
- audit, grounding, and permission enforcement remain mandatory

## Implementation Sequence

1. architecture and contract correction
2. retrieval tests and evaluation harness
3. business-reference extraction and typed query planning
4. search index and retrieval refactor
5. fusion and reranking
6. streaming and local-LLM orchestration
7. guardrail and citation hardening
8. API extension
9. benchmark and operational validation
