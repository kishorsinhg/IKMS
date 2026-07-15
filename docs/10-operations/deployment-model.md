# Deployment Model

## V1 Deployment

Single-tenant deployment for one broker.

Supported environments:

- On-premise server
- Private cloud server

## Runtime Profiles

- Web profile
- Worker profile

Both profiles are built from the same Spring Boot codebase.

## Core Dependencies

- PostgreSQL with pgvector
- Controlled document file storage
- IMAP mailbox access
- Local or private-network search infrastructure as approved for enterprise retrieval deployment
- Local or private-network LLM runtime for grounded AI orchestration

## Retrieval Operations

Operational deployment of the customer-centric retrieval platform assumes:

- indexed chunk projection lives in PostgreSQL `embedding_chunk`
- PostgreSQL plus `pgvector` is the active retrieval engine for current implementation
- business-reference fields are first-class indexed columns, not hidden in opaque metadata blobs
- evaluation and benchmark validation can run against seeded customer knowledge fixtures before rollout
- restricted-content handling and permission trimming are validated before provider-visible orchestration is enabled in production environments

## Implementation Status as of July 15, 2026

Current retrieval deployment status:

- PostgreSQL `embedding_chunk` is the live retrieval projection
- `pgvector` nearest-neighbour retrieval is active
- keyword retrieval, structured filtering, and Business Reference Field search are active through PostgreSQL-backed retrievers
- OpenSearch / Elasticsearch is planned and not active

Operational recovery expectations:

- if vector retrieval degrades, the platform should continue serving keyword, Business Reference Field, and structured retrieval with visible diagnostics
- projection rebuilds should use canonical customer knowledge artifacts as the source of truth
- stale projection recovery should prefer targeted or full reindex rather than ad hoc data edits
- monitoring should track retrieval fallback frequency, projection freshness, and reindex completion
- Phase 5C validation confirms the current release path should not depend on OpenSearch / Elasticsearch for recovery, failover, or benchmark interpretation

## Enterprise Operations Runtime

The Enterprise Operations platform adds:

- persisted operational jobs for reindex, embedding rebuild, OCR retry, AI retry, and maintenance work
- persisted queue state and queue-item overrides for operator control
- persisted schedulers and scheduler execution history
- local operational metrics storage for future observability
- administration-driven cache, health, and diagnostics controls

This runtime still uses the same Spring Boot deployment model and does not introduce Kubernetes-specific code.

## Non-SaaS Boundary

V1 is not a multi-tenant SaaS platform. Tenant isolation is not part of the V1 design.

## System Of Record Boundary

Policy Number, Claim Number, Insurer, Effective Date, Expiry Date, Renewal Date, and similar insurance attributes are structured Business Reference Fields within IKMS.

They are searchable, filterable, and indexable customer knowledge attributes.

IKMS does not own policy or claim lifecycle management.

The broker management system remains the system of record.
