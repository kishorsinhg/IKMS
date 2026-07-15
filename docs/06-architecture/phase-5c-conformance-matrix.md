# Phase 5C Conformance Matrix

## Purpose

This document compares the implemented enterprise retrieval platform against:

- `docs/06-architecture/phase-5a-enterprise-rag-blueprint.md`
- current implementation-status documentation
- `specs/001-insurance-broker-ikms/contracts/api.md`
- completed tasks `T171` through `T182`

This is a conformance assessment, not a redesign proposal.

## Product Boundary Confirmation

- Customer remains the primary business entity.
- Policy Number, Claim Number, Insurer, Policy Type, Effective Date, Expiry Date, Renewal Date, Broker Reference, and related insurance values remain Business Reference Fields.
- Business Reference Fields are searchable, filterable, and indexed.
- Policy and Claim are not IKMS entities, repositories, services, aggregate roots, or lifecycle models.
- The Broker Management System remains the system of record.

## Conformance Matrix

| Capability | Status | Notes |
| --- | --- | --- |
| Authentication | Implemented | Existing session and authentication governance remain active. |
| Authorization before retrieval | Implemented | Retrieval remains permission-aware before provider-visible context assembly. |
| Prompt injection detection | Implemented | Applied in guardrails and validated with OCR-style injection fixtures. |
| Business Reference extraction | Implemented with justified deviation | Deterministic extraction with normalization for OCR-like label noise; not ML-based extraction. |
| Typed query planning | Implemented | Customer/global/document-version scope, source types, date ranges, result limits, sort order, and evidence granularity are covered. |
| Hybrid retrieval | Implemented | PostgreSQL lexical, metadata, relationship, Business Reference, and `pgvector` retrieval are active. |
| Fusion | Implemented | Reciprocal-rank-style fusion is present in ranking. |
| Reranking | Implemented with justified deviation | Deterministic heuristic reranking is active; model-based reranking remains future work. |
| Version awareness | Implemented | Query planning and retrieval include current/previous version preferences. |
| Business Reference search | Implemented | Indexed first-class fields on `embedding_chunk` remain active. |
| Context builder | Implemented with justified deviation | Deterministic token budgeting and trimming are active; no semantic summarization compression exists yet. |
| Duplicate suppression in context | Implemented | Phase 5C adds evidence deduplication before provider-visible assembly. |
| Conversation memory | Implemented with justified deviation | Recent-message loading and trimming are active; long-horizon memory summarization is deferred. |
| LLM orchestration | Implemented | Streaming, timeout, fallback, and local-model preference scaffolding are active. |
| Grounding validation | Implemented | Grounding score, citation coverage, and insufficient-evidence handling are active. |
| Citation builder | Implemented | Citation lineage, location metadata, and supporting Business Reference attributes are active. |
| Audit logging | Implemented | Retrieval/orchestration telemetry and interaction persistence are active. |
| Streaming response | Implemented | Request/contract coverage and provider orchestration coverage are active. |
| Failure degradation | Implemented | Keyword/metadata fallback, restricted-content exclusion, and insufficient-evidence behavior are active. |
| Performance benchmarking | Implemented with justified deviation | Fixture-based stage benchmarks exist; full load harness remains future work. |
| Observability | Partially implemented | Retrieval diagnostics and telemetry persistence are present; full distributed tracing remains future work. |
| OpenSearch / Elasticsearch lexical engine | Deferred | Planned architecture only; not part of the current implementation. |
| Cross-encoder or local reranking model | Deferred | Current reranking is heuristic and intentionally lightweight. |
| Load-test scale validation | Missing | No concurrent enterprise load harness exists in the repository yet. |

## Deviations

### Justified Deviations

1. Reranking is heuristic instead of model-based.
   Reason:
   The current implementation prioritizes deterministic, local, infrastructure-light behavior for PostgreSQL plus `pgvector`.

2. Business Reference extraction is deterministic instead of classifier-backed.
   Reason:
   The current platform needs explainable, low-complexity extraction with no new provider dependency.

3. Context memory trimming is deterministic instead of summary-based.
   Reason:
   This avoids introducing additional model calls during retrieval validation.

4. Performance benchmarks are fixture-based rather than live-load benchmarks.
   Reason:
   Phase 5C focuses on correctness, consistency, and release-gate baselining before full-scale load generation.

### Deferred Items

1. OpenSearch / Elasticsearch remains future architecture only.
2. Model-based reranking remains future work.
3. Distributed tracing and enterprise load-generation remain future work.

## Conformance Conclusion

The implemented retrieval platform conforms to the customer-centric architecture boundary and to the active Phase 5B implementation model.

Primary gaps are operational-scale enhancements rather than correctness or boundary violations.
