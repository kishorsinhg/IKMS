# Enterprise AI Evaluation

## Purpose

This document defines the current evaluation and benchmark approach for the customer-centric enterprise retrieval and AI orchestration backend.

The goal is to make retrieval, grounding, citation behavior, and safe failure measurable before broader frontend integration and before introducing provider-specific tuning.

## Current Retrieval Benchmark Scope

As of July 15, 2026, benchmark and evaluation results apply to the implemented PostgreSQL and `pgvector` retrieval platform.

They do not represent OpenSearch / Elasticsearch behavior, because OpenSearch / Elasticsearch is not yet the active retrieval engine.

Phase 5C validation details and fixture-backed benchmark results are documented in:

- `docs/09-testing/phase-5c-enterprise-retrieval-validation.md`

## Evaluation Dimensions

The backend currently tracks or derives these evaluation signals:

- Latency
  - total orchestration latency
  - provider latency
- Retrieval quality
  - retrieval precision
  - retrieval recall
  - evidence count
  - retrieval mode
  - retrieval trace persistence
- Business reference search quality
  - policy number lookup accuracy as a Business Reference Field
  - claim number lookup accuracy as a Business Reference Field
  - insurer and related metadata lookup accuracy
- Grounding
  - grounding score
  - citation coverage
  - citation accuracy
  - warning count
- Permission safety
  - permission leakage rate
  - restricted-content exclusion behavior
- Execution resilience
  - fallback used
  - provider/model used
  - timeout handling
  - streaming event continuity
- Answer quality
  - deterministic review thresholds for manual follow-up
  - feedback capture through existing AI interaction feedback

## Benchmark Targets

Current benchmark thresholds for backend validation:

| Signal | Target | Action When Missed |
| --- | --- | --- |
| Total orchestration latency | `<= 1500 ms` for targeted test scenarios | flag for performance follow-up |
| Grounding score | `>= 0.85` | mark response for manual review |
| Citation coverage | `>= 0.90` | mark response for manual review |
| Citation accuracy | `>= 0.90` on curated fixtures | mark response for retrieval/citation review |
| Business reference search accuracy | `>= 0.90` on curated fixtures | investigate field extraction or indexed-field projection |
| Permission leakage rate | `0.0` on restricted fixtures | block release until resolved |
| Answer quality score | `>= 0.80` when available | use manual review until richer scoring exists |
| Fallback usage | `false` for healthy primary-provider execution | record degradation and warning trail |
| Streaming continuity | `start -> delta -> complete` for streamed happy path | inspect orchestration/provider pipeline |

These are backend guardrails, not final product SLAs.

## Current Automated Coverage

The following tests now provide orchestration evaluation coverage:

- `backend/src/test/java/com/ikms/ai/EnterpriseAiOrchestrationTest.java`
  - response contract shape
  - citations
  - evidence references
  - guardrail metadata
  - structured payloads
- `backend/src/test/java/com/ikms/ai/EnterpriseAiEvaluationTest.java`
  - grounding score thresholds
  - citation coverage expectations
  - latency and fallback review thresholds
- `backend/src/test/java/com/ikms/ai/EnterpriseRetrievalEvaluationFixtureTest.java`
  - business reference retrieval fixture thresholds
  - permission leakage regression fixtures
- `backend/src/test/java/com/ikms/ai/EnterpriseGuardrailServiceTest.java`
  - restricted-content exclusion
  - PII and permission trimming behavior
- `backend/src/test/java/com/ikms/ai/provider/LlmOrchestrationServiceTest.java`
  - streaming event capture
  - timeout handling
  - local-model fallback path
- `backend/src/test/java/com/ikms/search/ClientSearchTest.java`
  - hybrid retrieval diagnostics
  - retrieval-path quality baseline
- `backend/src/test/java/com/ikms/ai/EnterpriseKnowledgeControllerTest.java`
  - enterprise API contract coverage for knowledge search, ask, evidence expansion, continuation, and stream
- `backend/src/test/java/com/ikms/ai/BusinessReferenceExtractionAccuracyTest.java`
  - Business Reference precision/recall coverage across OCR-like label noise, missing values, and structured overrides
- `backend/src/test/java/com/ikms/ai/ContextBuilderServiceTest.java`
  - duplicate suppression and token-budget-aware context assembly
- `backend/src/test/java/com/ikms/search/EnterpriseRetrievalBenchmarkTest.java`
  - fixture-based retrieval mode comparison for keyword, vector, Business Reference, hybrid, and reranked retrieval
- `backend/src/test/java/com/ikms/performance/EnterpriseRetrievalPerformanceBenchmarkTest.java`
  - fixture-based latency percentile snapshots and degraded-mode upper bounds

## Operational Review Procedure

For backend validation of orchestration changes:

1. Run:
   `mvn clean -Dtest=EnterpriseKnowledgeControllerTest,EnterpriseGuardrailServiceTest,CitationBuilderServiceTest,EnterpriseAiEvaluationTest,EnterpriseRetrievalEvaluationFixtureTest,LlmOrchestrationServiceTest,EvidenceRankingServiceTest,EmbeddingIndexServiceTest,DocumentIntakeProcessingServiceTest,EnterpriseQueryPlanningServiceTest,ClientSearchTest,EnterpriseAiOrchestrationTest,EnterpriseKnowledgeRetrievalContractsTest test`
2. Confirm contract responses still return:
   - retrieval diagnostics
   - citation jump targets
   - guardrail metadata
   - structured payload scaffolding
   - restricted-content notice when applicable
   - streaming events for streamed requests
3. Confirm evaluation thresholds still hold:
   - strong citations produce grounded responses
   - low-confidence evidence reduces grounding score
   - restricted or permission-trimmed evidence does not leak into grounded answers
   - business-reference fixtures keep customer-centric lookup accuracy above target
   - degraded latency/fallback snapshots require manual review
4. Inspect persisted orchestration telemetry in:
   - `ai_interaction`
   - `ai_orchestration_metric`
   - `ai_citation_record`
   - `ai_retrieval_trace`
5. During reindex or schema changes, confirm:
   - `embedding_chunk.reindex_version` reflects the current projection version
   - business-reference columns are populated for newly indexed content
   - legacy chunks are either reindexed or excluded from benchmark comparisons

## Known Gaps

- No production-grade answer-quality scorer exists yet.
- Retrieval evaluation still uses lightweight fixtures rather than a large labeled gold dataset.
- No load or concurrency benchmark suite exists yet.
- No live provider streaming benchmark is executed in CI.
- Feedback capture exists, but no aggregation pipeline is implemented yet.
- Streaming cancellation is covered with unit-level orchestration tests, not end-to-end network interruption tests.

## Next Follow-Up

- Expand the lightweight evaluation fixtures into seeded benchmark datasets for customer, insurer, document-version, and cross-document reasoning.
- Add repeatable provider-degradation tests that simulate timeout, retry, and fallback transitions against a controllable provider harness.
- Add benchmark reporting that summarizes orchestration metrics over seeded scenarios and persisted telemetry tables.
- Add explicit benchmark scenarios for vector-unavailable degradation, stale projections, projection rebuilds, and reindex compatibility behavior on the current PostgreSQL-backed retrieval implementation.
- Treat OpenSearch / Elasticsearch benchmarks as future evaluation work only after implementation and migration exist.
