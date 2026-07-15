# Quickstart Validation Summary

Date: 2026-07-10
Reviewer: Codex
Feature: Insurance Broker IKMS V1

## Validation Status

- Automated backend validation completed with `mvn test`.
- Automated frontend validation completed with `npm test -- --run`.
- Production frontend build completed with `npm run build`.
- Playwright quickstart coverage was added in `frontend/tests/quickstart.spec.ts`.
- Backend local seed data was added in `backend/src/main/resources/db/dev/V900__dev_seed.sql`.

## Scenario Coverage Summary

- Scenario 1 Client Profile: covered by backend client tests and frontend client profile tests; manual live run still recommended for visual confirmation.
- Scenario 2 Manual Upload And Review: covered by document upload and review workflow tests; live file upload still recommended.
- Scenario 3 Email Intake: backend adapters and entity flows exist, but a live IMAP run was not executed in this session.
- Scenario 4 Duplicate Detection And Versioning: covered by backend duplicate/version tests.
- Scenario 5 Processor PII Protection: covered by backend PII/redaction tests and frontend permission tests.
- Scenario 6 Supervisor Original Access: covered by redacted/original document access tests.
- Scenario 7 Client-Level Search And Q&A: covered by backend search/AI tests and frontend search tests.
- Scenario 8 Audit Export: covered by backend audit export tests and frontend audit UI tests.

## Manual Validation Remaining

- Start backend and frontend against a local PostgreSQL instance with Flyway migrations applied.
- Load seed data from `backend/src/main/resources/db/dev/V900__dev_seed.sql`.
- Run the Playwright quickstart flow once Playwright dependencies and browsers are installed locally.
- Execute one real IMAP/shared-folder intake cycle if those infrastructure endpoints are available.

## Outcome

Release closeout has strong automated coverage, but full quickstart signoff is still partially dependent on one live local environment run.

---

## Session Update: 2026-07-15 Phase 11 Enterprise Platform Readiness & Operational Excellence

### Completed In This Session

- Created the Phase 11 task block (`T223` through `T227`) in `specs/001-insurance-broker-ikms/tasks.md` before implementation.
- Added a reusable backend observability foundation with request-context headers, MDC propagation, async context propagation, audit trace enrichment, and API error request/correlation identifiers.
- Standardized workflow-level trace identifiers for operations jobs, processing jobs, review actions, retrieval, AI interactions, timeline requests, and search requests.
- Added a platform-neutral alert-definition framework in the operations module without introducing external notification integrations.
- Added final-phase architecture review, platform consistency review, runbooks, go-live readiness guidance, technical debt register, architecture validation, repository quality review, and final documentation review artifacts.
- Corrected stale SDS references to the current UI flow-map path and preserved the boundary that Policy and Claim remain Business Reference Fields only.

### Validation

- Backend compile passed:
  - `cd backend && mvn -q -DskipTests compile`
- Backend targeted validation passed:
  - `cd backend && mvn -q -Dtest=GlobalExceptionHandlerTest,RequestContextFilterTest,OperationsControllerContractTest test`
  - `cd backend && mvn -q -Dtest=GlobalExceptionHandlerTest,RequestContextFilterTest,OperationsControllerContractTest,ClientQuestionAnsweringTest,DocumentIntakeProcessingServiceTest,ReviewQueueWorkflowTest test`
- Frontend targeted validation passed:
  - `cd frontend && npm test -- --run src/api/client.test.ts`
- Frontend lint passed:
  - `cd frontend && npm run lint`
- Frontend production build passed:
  - `cd frontend && npm run build`

### Readiness Notes

- The frontend production build still emits a chunk-size warning for the main bundle; this is a non-blocking optimization follow-up, not a release-candidate blocker.
- The repository remains in a dirty multi-phase worktree; Phase 11 tracking above only covers the readiness hardening changes implemented in this session.

---

## Session Update: 2026-07-13

### Completed In This Session

- Implemented Phase 10 convergence work for note editing/deletion, richer review metadata correction, field-level PII sensitivity, review routing from admin settings, conflict-aware AI answer assembly, prompt-injection blocking/auditing, and persisted retention workflow execution state.
- Added admin AI configuration support for `apiBaseUrl` and `apiKey` in the backend and admin UI.
- Updated convergence tracking in `specs/001-insurance-broker-ikms/tasks.md`.

### Validation Executed

- Backend targeted validation passed:
  - `mvn test -Dtest=ClientQuestionAnsweringTest,RetentionWorkflowTest,ReviewQueueWorkflowTest,RedactedDocumentAccessTest`
  - `mvn test -Dtest=AdminConfigurationTest`
- Frontend targeted validation passed:
  - `npm test -- --run src/features/admin/AdminConfiguration.test.tsx`

### Remaining Follow-Up

- `T131`: add real OCR/PDF/DOCX extraction adapters and production mailbox/shared-folder parsing behind configured provider settings.
- `T132`: replace token-overlap retrieval with real embedding generation and pgvector similarity search using the stored AI provider settings.

### Session Update: 2026-07-13 Continued

- Implemented provider-backed AI client abstractions for chat classification and embeddings using admin-configured `apiBaseUrl` and `apiKey`.
- Replaced UTF-8-only extraction stubs with PDFBox PDF parsing and Apache POI DOCX parsing in the main extraction service.
- Added embedding vector persistence and pgvector similarity search path in client search, with fallback ranking when provider embeddings are unavailable.
- Unified manual upload, shared-folder intake, and email attachment processing through the same document extraction/classification/indexing pipeline.

### Additional Validation Executed

- `mvn test -Dtest=TextExtractionServiceTest,AdminConfigurationTest,ClientQuestionAnsweringTest,RetentionWorkflowTest,ReviewQueueWorkflowTest,NoteControllerContractTest`

### Session Update: 2026-07-13 Evidence And Retrieval Refinement

- Added a dedicated embedding model configuration path so administrators can configure embedding generation independently from the chat/classification model.
- Replaced fixed-width chunking with context-preserving semantic chunking that stores chunk index, token count, source title/section, language, page number, and retrieval-summary metadata.
- Extended PDF extraction and indexing to retain page-aware provenance, then surfaced page/location-aware citations in client search and AI answers.
- Improved retrieval quality with hybrid ranking plus local chunk-neighbor expansion so answers can assemble evidence from multiple documents, emails, and notes within the selected client.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn test -Dtest=ClientQuestionAnsweringTest,ClientSearchTest,EmbeddingIndexServiceTest,TextExtractionServiceTest`
- Frontend targeted validation passed:
  - `npm test -- --run src/features/search/ClientSearchAsk.test.tsx src/features/admin/AdminConfiguration.test.tsx`

### Session Update: 2026-07-13 Provider Validation Hardening

- Added an administrator-facing AI/OCR provider validation endpoint and admin UI action so the configured chat model, embedding model, and OCR provider support can be checked before relying on saved settings.
- Added timeout-based provider probe behavior in the shared AI client and surfaced validation status/messages instead of silently relying on save-only configuration changes.
- Added audit coverage for provider validation success and failure outcomes.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn test -Dtest=AdminConfigurationTest,AiProviderClientTest`
- Frontend targeted validation passed:
  - `npm test -- --run src/features/admin/AdminConfiguration.test.tsx`

### Session Update: 2026-07-13 OCR Extraction Integration

- Added provider-backed OCR extraction for scanned or image-only PDFs using the configured Mistral OCR model when native PDF parsing yields no usable text.
- Preserved OCR output as page-aware extraction segments so downstream indexing and citations continue to carry page provenance.
- Propagated OCR confidence into document extraction confidence so low-confidence OCR output can route intake items to review.
- Corrected extraction provider labeling so native PDF/DOCX parsing records `pdfbox` or `apache-poi`, while OCR-backed extraction records the actual OCR model used.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn test -Dtest=TextExtractionServiceTest,AiProviderClientTest,DocumentIntakeProcessingServiceTest`
- Live Mistral OCR probe succeeded against `sample/pdf_scanned_ocr.pdf` using `mistral-ocr-latest`, returning page-wise markdown and confidence metadata.

### Session Update: 2026-07-13 Provider-Generated Client Answers

- Replaced the rule-based client answer assembly path with provider-generated answer synthesis constrained to retrieved, authorized evidence.
- Kept refusal, no-evidence handling, prompt-injection filtering, citations, and conflict detection in the service layer so the model only synthesizes the final answer from already-approved context.
- Added a deterministic fallback path so if provider synthesis fails, client AI still returns an evidence-based answer instead of breaking the workflow.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn test -Dtest=ClientQuestionAnsweringTest,AiProviderClientTest`

### Session Update: 2026-07-13 Retrieval Observability Hardening

- Added retrieval observability metadata so search results now carry retrieval path and citation quality, and AI answers now carry retrieval mode plus warning messages.
- Surfaced provider degradation when vector retrieval cannot run, falling back to keyword/metadata retrieval while preserving a visible warning trail.
- Added citation-quality validation so document evidence missing strong location provenance is flagged for downstream answer warnings and UI visibility.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn test -Dtest=ClientQuestionAnsweringTest,AiProviderClientTest,ClientSearchTest`
- Frontend targeted validation passed:
  - `npm test -- --run src/features/search/ClientSearchAsk.test.tsx`

### Session Update: 2026-07-15 Enterprise AI Orchestration Backend

- Added reusable orchestration context assembly with token-budget allocation, provider-ready prompt shaping, and conversation-history persistence.
- Added provider-agnostic LLM orchestration with retry/fallback hooks, local-model adapter scaffolding, provider telemetry capture, and deterministic fallback handling.
- Added orchestration-time guardrail enforcement for prompt-injection filtering, PII masking in provider-visible context, token-limit signaling, and citation/retrieval telemetry persistence.
- Extended client AI API coverage with orchestration-backed `summarize`, `explain`, `compare`, `extract`, and `validate` endpoints while preserving existing `search` and `ask` behavior.
- Added orchestration evaluation documentation and backend threshold tests for latency, grounding, citation coverage, fallback handling, and manual-review escalation.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn -Dtest=ClientSearchTest,EnterpriseAiOrchestrationTest,EnterpriseAiEvaluationTest test`

### Remaining Follow-Up

- Introduce labeled retrieval benchmark fixtures for customer, policy, claim, document-version, and cross-document reasoning quality.
- Add live provider timeout/retry/fallback regression coverage once a controllable provider test harness is available.
- Add aggregate reporting over persisted `ai_orchestration_metric`, `ai_citation_record`, and `ai_retrieval_trace` data.

### Session Update: 2026-07-15 Customer-Centric Enterprise Retrieval Completion

- Added typed business-reference extraction and provider-independent query planning for customer-centric enterprise retrieval.
- Refactored retrieval to consume typed query context with customer scope, source filters, date filters, business-reference filters, and version-aware behavior.
- Added first-class indexed business-reference columns and reindex-version tracking on `embedding_chunk`, with projection population during indexing.
- Added fusion and reranking behavior for source diversity, freshness, duplicate suppression, and chunk-lineage-aware grounded retrieval.
- Extended orchestration with streaming, timeout, cancellation, local-model preference, restricted-content notices, and stronger guardrail outcomes.
- Implemented enterprise knowledge APIs for global ask, knowledge search, evidence expansion, conversation continuation, and streaming without introducing Policy or Claim APIs.
- Added evaluation fixtures, controller coverage, guardrail/citation regression tests, and updated operations/testing documentation for reindexing and benchmark validation.

### Additional Validation Executed

- Backend targeted validation passed:
  - `mvn clean -Dtest=EnterpriseKnowledgeControllerTest,EnterpriseGuardrailServiceTest,CitationBuilderServiceTest,EnterpriseAiEvaluationTest,EnterpriseRetrievalEvaluationFixtureTest,LlmOrchestrationServiceTest,EvidenceRankingServiceTest,EmbeddingIndexServiceTest,DocumentIntakeProcessingServiceTest,EnterpriseQueryPlanningServiceTest,ClientSearchTest,EnterpriseAiOrchestrationTest,EnterpriseKnowledgeRetrievalContractsTest test`

### Remaining Follow-Up

- Expand lightweight retrieval evaluation fixtures into broader seeded benchmark datasets.
- Add end-to-end provider degradation and network interruption harness coverage.
- Add aggregated reporting and historical trend analysis over orchestration and retrieval telemetry tables.

### Session Update: 2026-07-15 Retrieval Implementation Status Documentation Alignment

- Updated architecture documentation to distinguish the current PostgreSQL plus pgvector retrieval implementation from the approved future OpenSearch / Elasticsearch target architecture.
- Added implementation-status tables and current-vs-target-vs-future guidance across the Phase 5A blueprint and customer-centric retrieval architecture docs.
- Clarified that `embedding_chunk` is the active retrieval projection, `EmbeddingIndexService` is the projection producer, `pgvector` retrieval is active, and degraded retrieval remains visible through diagnostics or warnings.
- Updated operations and evaluation documentation for projection lifecycle, reindex behavior, stale projection recovery, fallback retrieval, and Phase 5C benchmark scope.
- Added an architecture decision log entry confirming PostgreSQL plus pgvector as the current enterprise retrieval implementation while preserving future migration direction.

### Validation

- Documentation-only alignment pass completed.
- No build or test execution was required because no code, API, or schema changes were made.

### Session Update: 2026-07-15 Phase 7 Enterprise Document Processing And Human Review Platform

- Added a persisted document-processing job model with stage/status tracking, confidence breakdowns, extracted-field lineage, validation findings, reviewer comments, retry state, and publish-completion timestamps.
- Refactored intake orchestration into explicit modular stages for virus scanning, OCR/text extraction, language detection, classification, metadata extraction, validation, confidence aggregation, review routing, and publish/index follow-through.
- Extended review APIs and the Review workspace so queue/detail workflows can surface processing status, validation findings, extracted-field confidence, and retry controls without redesigning the existing review architecture.
- Preserved the product boundary that Policy and Claim remain Business Reference Fields only; processing fields and findings expose reference metadata without introducing policy or claim entities or workflows.

### Validation

- Backend compile passed:
  - `cd backend && mvn -q -DskipTests compile`
- Backend targeted tests passed:
  - `cd backend && mvn -q -Dtest=DocumentIntakeProcessingServiceTest,ReviewQueueWorkflowTest test`
- Frontend targeted tests passed:
  - `cd frontend && npm test -- --run src/features/intake/review/ReviewDetail.test.tsx src/features/intake/review/ReviewQueue.test.tsx`
- Frontend production build passed:
  - `cd frontend && npm run build`

### Session Update: 2026-07-15 Phase 5C Enterprise Retrieval Validation And Optimization

- Added Phase 5C conformance, validation, and release-readiness documentation for the implemented PostgreSQL plus `pgvector` enterprise retrieval platform.
- Added executable Phase 5C fixtures for Business Reference extraction accuracy, query planning validation, retrieval benchmark comparisons, context deduplication validation, guardrail coverage, and fixture-based performance percentile checks.
- Tightened Business Reference extraction to handle OCR-like label normalization and prompt-level broker/external reference extraction without introducing Policy or Claim entities.
- Optimized search execution so vector retrieval runs once per request instead of duplicating the query-embedding and vector lookup path.
- Added context evidence deduplication before token-budget trimming to improve provider-visible evidence density.
- Stabilized Mockito-based backend validation in the current environment by forcing the subclass mock maker in test resources.

### Validation

- Backend targeted validation passed:
  - `mvn -Dtest=BusinessReferenceExtractionAccuracyTest,EnterpriseQueryPlanningServiceTest,EnterpriseQueryPlanningValidationTest,EnterpriseRetrievalEvaluationFixtureTest,ContextBuilderServiceTest,EnterpriseGuardrailServiceTest,ClientSearchServiceOptimizationTest,EnterpriseRetrievalBenchmarkTest,EvidenceRankingServiceTest,LlmOrchestrationServiceTest,EnterpriseKnowledgeControllerTest,EnterpriseKnowledgeRetrievalContractsTest,EnterpriseAiEvaluationTest,EnterpriseAiOrchestrationTest,ClientSearchTest,EnterpriseRetrievalPerformanceBenchmarkTest,SecurityTrimBoundaryTest,EmbeddingIndexServiceTest,DocumentIntakeProcessingServiceTest test`
- Backend package build passed:
  - `mvn -DskipTests package`

### Session Update: 2026-07-15 Phase 9 Enterprise Administration & Operations Platform

- Created the Phase 25 task block for product Phase 9 (`T218` through `T222`) in `specs/001-insurance-broker-ikms/tasks.md` before implementation.
- Added a persisted enterprise operations layer for background jobs, queue state, scheduler state and history, cache controls, and operational metrics in the backend.
- Added `/api/operations/*` endpoints for jobs, queues, schedulers, reindex, embeddings, OCR retry, AI retry, cache, health, and diagnostics with additive operational permissions and audit coverage.
- Extended the existing Administration workspace with operations-focused modules for background jobs, queues, scheduler, embeddings, OCR, AI operations, cache, health, and diagnostics using the established enterprise UI patterns.
- Added architecture and operations documentation for runtime responsibilities, recovery boundaries, operator procedures, deployment implications, and API contracts.
- Preserved the product boundary that Policy and Claim remain Business Reference Fields only; no Policy or Claim entities or lifecycle workflows were introduced.

### Validation

- Backend compile passed:
  - `cd backend && mvn -q -DskipTests compile`
- Backend targeted tests passed:
  - `cd backend && mvn -q -Dtest=OperationsControllerContractTest,PermissionServiceTest test`
- Frontend targeted tests passed:
  - `cd frontend && npm test -- --run src/features/admin/AdminConfiguration.test.tsx`
- Frontend production build passed:
  - `cd frontend && npm run build`

### Session Update: 2026-07-15 Phase 8 Enterprise Knowledge Quality And Data Stewardship Platform

- Added a customer-centric Knowledge Quality platform with rebuildable quality snapshots and issue queues backed by `knowledge_quality_snapshot` and `knowledge_quality_issue`.
- Added explainable quality scoring for metadata completeness, Business Reference validation, customer linkage, duplicate detection, timeline quality, version quality, retrieval readiness, and AI quality without introducing Policy or Claim entities.
- Added stewardship APIs for customer quality summaries, detail, issue queues, revalidation, reindex, and controlled bulk correction using the existing publishing and retrieval-refresh pipeline.
- Added a frontend Knowledge Quality workspace with customer quality summaries, steward issue review, score breakdowns, revalidation/reindex actions, and correction dialogs using the existing shared shell and enterprise UI patterns.
- Updated architecture, API, data-model, workspace, and operations documentation to reflect the new quality lifecycle and to preserve the product boundary that Policy and Claim remain Business Reference Fields only.

### Validation

- Backend targeted validation passed:
  - `cd backend && mvn -q -Dtest=KnowledgeQualityControllerTest test`
- Backend compile passed:
  - `cd backend && mvn -q -DskipTests compile`
- Frontend targeted validation passed:
  - `cd frontend && npm test -- --run src/features/quality/KnowledgeQualityPage.test.tsx src/app/App.test.tsx`
- Frontend production build passed:
  - `cd frontend && npm run build`

### Session Update: 2026-07-15 Phase 10 Enterprise Security, Governance & Compliance Platform

- Created the Phase 10 task block (`T212` through `T217`) in `specs/001-insurance-broker-ikms/tasks.md` before implementation.
- Added additive RBAC-plus-ABAC governance controls using user and document attributes for business unit, department, region, country, broker office, and security clearance.
- Added persisted governance fields for document classification, lifecycle state, export controls, and retention/legal-hold scheduling metadata.
- Added governance APIs and settings-backed policy storage for classification policy, retention policy, legal holds, AI governance, security policy, compliance reporting, and document reclassification.
- Extended Administration with governance visibility plus editable AI governance and security policy modules without redesigning the workspace.
- Preserved the product boundary that Customer remains primary and Policy/Claim stay Business Reference Fields only.

### Validation

- Backend compile passed:
  - `cd backend && mvn -q -DskipTests compile`
- Backend targeted tests passed:
  - `cd backend && mvn -q -Dtest=GovernanceAccessServiceTest,GovernanceControllerTest,SecurityTrimBoundaryTest,AdminConfigurationTest test`
- Frontend targeted tests passed:
  - `cd frontend && npm test -- --run src/features/admin/AdminConfiguration.test.tsx`
- Frontend production build passed:
  - `cd frontend && npm run build`

### Remaining Follow-Up

- Add concurrent load-generation and endurance validation for production-scale enterprise workloads.
- Expand evaluation fixtures into larger gold-label corpora with broader multilingual and OCR-noise coverage.
- Add distributed tracing and operator-facing dashboards for retrieval, reranking, and reindex telemetry.

### Session Update: 2026-07-15 Phase 6 Customer Knowledge Timeline And Related Knowledge

- Added customer-centric timeline and related-knowledge backend contracts, typed API responses, source-level related navigation, and document-version history endpoints without introducing Policy or Claim entities.
- Implemented timeline assembly from documents, document versions, emails, notes, review activity, Business Reference Field extraction/correction metadata, and AI conversations with deterministic ordering and cursor pagination.
- Implemented related-knowledge derivation for email attachments, version lineage, shared Business Reference Fields, email threads, exact duplicates, and pgvector-backed similarity links with inferred-vs-deterministic labeling.
- Replaced the Customer360 synthetic timeline with real API-backed timeline data, populated the relationships tab from related-knowledge APIs, and wired document-viewer evidence sections to related knowledge and version history.
- Extended the Customer360 assistant integration so suggested prompts, source references, and evidence references can reflect timeline and related-knowledge context while preserving the existing workspace design.
- Updated architecture, contract, data-model, and operations documentation to capture the Phase 6 boundary that Policy and Claim remain searchable Business Reference Fields rather than IKMS-owned entities.

### Validation

- Backend compile passed:
  - `cd backend && mvn -q -DskipTests compile`
- Backend targeted validation passed:
  - `cd backend && mvn -q -Dtest=ClientKnowledgeServiceTest test`
- Frontend targeted validation passed:
  - `cd frontend && npm test -- src/features/clients/ClientProfile.test.tsx`
- Frontend build passed:
  - `cd frontend && npm run build`

### Remaining Follow-Up

- Add broader API-contract and authorization regression coverage around timeline filtering, cursor pagination, and restricted-source trimming across more than one customer fixture.
- Extend viewer and assistant tests beyond Customer360 to cover Search and Review surfaces once those flows consume the same Phase 6 contracts directly.
- Add operator-facing telemetry and benchmarking for timeline-query latency, related-knowledge fanout, and similarity-link degradation under large customer histories.

### Session Update: 2026-07-15 Phase 6B Cross-Workspace Timeline And Related-Knowledge Integration

- Consolidated shared frontend knowledge access into `frontend/src/api/knowledge.ts`, with stable query-key helpers for client timeline, client related knowledge, source related knowledge, and document versions.
- Extended the shared API client to accept request options so TanStack Query cancellation signals can flow into the Phase 6 knowledge calls without creating workspace-specific fetch wrappers.
- Integrated Search selected-result context with lazy related-knowledge, compact recent customer knowledge, and version-history queries, and reused that data for Search assistant source/evidence references and document-viewer evidence sections.
- Integrated Review Detail with shared related-knowledge, recent customer knowledge, and version-history context in the Evidence Assistant and viewer evidence sections while preserving the existing approve/reject-first workflow.
- Integrated Review Queue with selected-item-only related-knowledge and version-history indicators so the queue stays row-efficient and avoids row-level N+1 relationship requests.
- Added shared knowledge-context mapping helpers so Business Reference Fields continue to appear as metadata and relationship explanations rather than as Policy or Claim entities.

### Validation

- Frontend lint passed:
  - `cd frontend && npm run lint`
- Frontend targeted workspace validation passed:
  - `cd frontend && npm test -- --run src/features/search/SearchLandingPage.test.tsx src/features/intake/review/ReviewDetail.test.tsx src/features/intake/review/ReviewQueue.test.tsx`
- Frontend build passed:
  - `cd frontend && npm run build`
- Full frontend suite remains partially failing outside the Phase 6B surface:
  - `cd frontend && npm test -- --run`
  - Remaining failures are currently in `src/features/clients/PiiVisibility.test.tsx`, where Customer360 falls into the existing "Unable to load Customer360" error state. Search, Review Queue, and Review Detail targeted coverage passed.

### Remaining Follow-Up

- Investigate the existing Customer360 `PiiVisibility` failure path separately from Phase 6B before treating the full frontend suite as release-clean.
- Add broader integration coverage for restricted-source trimming and lazy related-knowledge fetch behavior across Customer360 and any future Claim/Policy-reference search filters, while preserving the boundary that those values remain Business Reference Fields only.
