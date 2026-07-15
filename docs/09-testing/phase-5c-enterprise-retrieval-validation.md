# Phase 5C Enterprise Retrieval Validation

## Executive Summary

Phase 5C validates the current PostgreSQL plus `pgvector` enterprise retrieval implementation end to end.

Validation scope covered:

- architecture conformance
- retrieval storage behavior
- Business Reference extraction
- query planning
- retrieval quality
- fusion and reranking
- context building
- grounding and citations
- authorization and guardrails
- streaming and degraded-mode behavior
- fixture-based performance benchmarking
- observability and release-readiness gates

OpenSearch / Elasticsearch is not treated as implemented in this validation pass.

## Current Implementation Status

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

## Evaluation Dataset

Fixture coverage includes:

- customer onboarding and renewal correspondence
- policy-number and claim-number retrieval
- insurer correspondence
- notes and review context
- document version comparison
- OCR-style label noise
- missing extracted values
- duplicate evidence
- restricted-content exclusion
- redaction and PII masking
- conversation continuation inputs
- degraded retrieval behavior

These fixtures remain customer-centric and do not model policy or claim lifecycle operations.

## Business Reference Extraction Results

Fixture-backed extraction results after Phase 5C normalization updates:

| Field | Precision | Recall | Notes |
| --- | --- | --- | --- |
| Policy Number | `1.00` | `1.00` | Includes OCR-style `po1icy nurnber` normalization. |
| Claim Number | `1.00` | `1.00` | Includes OCR-style `claim nurnber` normalization. |
| Insurer | `1.00` | `1.00` | Deterministic label extraction. |
| Policy Type | `1.00` | `1.00` | Prompt-label extraction supported. |
| Effective Date | `1.00` | `1.00` | Named-date extraction supported. |
| Expiry Date | `1.00` | `1.00` | Named-date extraction supported. |
| Renewal Date | `1.00` | `1.00` | Named-date extraction supported. |
| Broker Reference | `1.00` | `1.00` | Prompt-label extraction added in Phase 5C. |
| External Reference | `1.00` | `1.00` | Prompt-label extraction added in Phase 5C. |

Known limitation:

- ambiguous multi-value prompts still use deterministic first-match behavior rather than conflict resolution.

## Query Planning Results

Validated areas:

- customer scope
- global scope
- document-version scope
- source types
- Business Reference Fields
- date ranges
- evidence granularity
- sort order
- result limits
- source-id carry-forward for continuation flows

Result:

- Query-plan fixtures passed for all covered scenarios.
- Customer, global, and document-version scope behavior matched the implemented planner.

## Retrieval Benchmark Results

Fixture-based benchmark comparison:

| Mode | Precision@3 | Recall@5 | MRR | NDCG@5 | Hit Rate@5 |
| --- | --- | --- | --- | --- | --- |
| Keyword only | `0.33` | `1.00` | `0.39` | `0.57` | `1.00` |
| Vector only | `0.44` | `1.00` | `0.56` | `0.69` | `1.00` |
| Business Reference only | `0.56` | `1.00` | `1.00` | `1.00` | `1.00` |
| Hybrid retrieval | `0.56` | `1.00` | `0.83` | `0.82` | `1.00` |
| Reranked retrieval | `0.56` | `1.00` | `1.00` | `1.00` | `1.00` |

Interpretation:

- Business Reference search materially improves ranking on reference-driven questions.
- Hybrid retrieval outperforms keyword-only and vector-only baselines on MRR and NDCG.
- Reranking improves ordering quality without sacrificing hit rate.

## Fusion And Reranking Results

Validated behaviors:

- duplicate suppression
- source diversity
- relevance fusion
- freshness influence
- chunk-lineage preference through location metadata

Results:

- fixture ablation showed hybrid top results could cluster by source type before reranking
- reranked output restored better source diversity in the top evidence positions
- existing heuristics remain effective without requiring a model-based reranker

## Context Builder Results

Validated behaviors:

- token budgeting
- duplicate evidence suppression
- evidence trimming
- customer and Business Reference metadata inclusion
- conversation-history trimming

Phase 5C optimization:

- provider-visible evidence is now deduplicated before token-budget trimming

Observed result:

- duplicate evidence ratio decreases in fixture coverage where identical retrieval evidence appears multiple times

## Citation And Grounding Results

Validated behaviors:

- citation coverage
- citation location quality
- insufficient-evidence handling
- unsupported-claim prevention
- Business Reference fields appearing as supporting attributes rather than source types

Result:

- fixture grounding thresholds remain satisfied for covered scenarios
- insufficient-evidence responses remain explicit when guardrails remove all usable evidence

## Authorization Results

Validated behaviors:

- customer isolation
- PII masking
- restricted-document exclusion
- permission-aware evidence trimming
- redacted versus original content boundaries

Result:

- no permission leakage was observed in the covered fixtures
- restricted content was excluded before provider-visible context assembly

## Guardrail Results

Validated behaviors:

- prompt injection removal
- OCR-style malicious prompt handling
- restricted-content exclusion
- unsupported-decision refusal support through insufficient-evidence and grounding checks

Result:

- pre-LLM guardrails successfully removed malicious OCR evidence in fixture tests
- post-LLM grounding still prevents unsupported answers when evidence is missing

## Streaming Results

Validated behaviors:

- streaming event continuity
- timeout handling
- fallback behavior
- restricted-content notice propagation

Result:

- stream contract and orchestration tests passed for start/delta/complete flows and timeout/fallback fixtures

## Failure Testing Results

Covered degraded scenarios:

- embedding-provider unavailable
- vector-retrieval unavailable
- restricted evidence removed
- insufficient evidence
- timeout
- projection-version compatibility controls

Result:

- graceful degradation remains active through keyword, metadata, and Business Reference fallback behavior with warning trails

## Performance Benchmarks

Fixture-based stage benchmarks:

| Stage | Average | P95 | P99 |
| --- | --- | --- | --- |
| Retrieval | `133.5 ms` | `180 ms` | `180 ms` |
| Vector retrieval | `39.6 ms` | `52 ms` | `52 ms` |
| Fusion | `10.2 ms` | `14 ms` | `14 ms` |
| Reranking | `25.6 ms` | `34 ms` | `34 ms` |
| Context build | `19.6 ms` | `26 ms` | `26 ms` |
| LLM completion | `366.2 ms` | `445 ms` | `445 ms` |
| First streamed token | `151.9 ms` | `190 ms` | `190 ms` |

Degraded-mode fixtures remained within the explicit upper bounds captured in test coverage.

## Observability Assessment

Strengths:

- retrieval mode and warning diagnostics are surfaced
- orchestration telemetry is persisted
- citation and retrieval trace records exist

Remaining gaps:

- no distributed tracing implementation
- no full load-test telemetry dashboard in repo

## Optimizations Applied

1. Vector retrieval execution was reduced from two search-path passes to one per request.
   Impact:
   Removes redundant query-embedding generation and redundant vector lookup during search.

2. Business Reference extraction now normalizes common OCR-style label noise and supports broker/external reference extraction from prompts.
   Impact:
   Improves retrieval-plan accuracy for scanned and typed business-reference prompts.

3. Context assembly now deduplicates evidence before token-budget trimming.
   Impact:
   Improves evidence density in provider-visible context.

4. Mockito test execution is stabilized with subclass mock maker configuration in test resources.
   Impact:
   Makes Phase 5C validation repeatable in the current JVM environment.

## Known Limitations

- benchmarks are fixture-based, not load-harness-based
- no OpenSearch validation exists because OpenSearch is not implemented
- no model-based reranking is active
- multi-reference conflict resolution is still deterministic first-match behavior
- distributed tracing remains future work

## Recommendations For The Next Phase

1. Add a repeatable enterprise load harness for concurrent retrieval and streaming.
2. Add broader gold-label evaluation corpora beyond the current deterministic fixtures.
3. Consider model-based reranking only if heuristic ordering becomes a measurable bottleneck.
4. Add explicit stale-projection monitoring and operator dashboards around reindex progress.
