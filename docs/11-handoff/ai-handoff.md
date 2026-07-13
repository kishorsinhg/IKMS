# AI Handoff

Use this file to give future Codex sessions the minimum context needed to continue.

## Current Product Direction

Build V1 as an Insurance Broker Knowledge Management System, not a generic configurable platform.

## Confirmed Technology

- Backend: Spring Boot Java monolith.
- Frontend: React.
- Database: PostgreSQL with pgvector.
- AI/OCR: configurable provider, Mistral Cloud first.
- Auth: local username/password.
- Deployment: single-tenant on-premise or private cloud.

## Key Product Decisions

- Client is the only master record.
- Existing broker system remains source of truth.
- Policy/claim data is searchable metadata, not managed records.
- Documents, emails, and notes link to Client.
- Client-level AI Q&A only.
- Processor cannot access PII except redacted previews/downloads.
- Supervisor can access PII and originals.
- Original files are preserved.
- Document versioning is supported.
- Exact duplicate file hash is blocked/skipped.
- Supported files: PDF, DOCX, Email.
- Email intake uses IMAP.
- Shared folder intake uses server path polling.

## Current Spec Kit Feature

- Feature: `001-insurance-broker-ikms`
- Spec: `specs/001-insurance-broker-ikms/spec.md`
- Plan: `specs/001-insurance-broker-ikms/plan.md`
- Tasks: `specs/001-insurance-broker-ikms/tasks.md`
- Constitution: `.specify/memory/constitution.md` v1.1.0
- Task count: 132

## Next Recommended Task

Local scaffold work for Phase 1 setup exists and should now be treated as the current baseline.

Current implementation checkpoint:

- `T001-T121` are completed and reflected in `specs/001-insurance-broker-ikms/tasks.md`.
- Convergence tasks `T122-T130` are partially completed; `T122-T125`, `T128-T130` are done, while `T126-T127` remain partial and have been split into follow-up tasks `T131-T132`.
- Backend scaffold exists in `backend/` with Spring Boot app entrypoint, dependency management, test skeleton, and application config.
- Baseline Flyway migration exists in `backend/src/main/resources/db/migration/V001__baseline_schema.sql` with pgvector/pgcrypto extensions and initial `audit_log` table.
- Shared API error contract and global exception handling exist in `backend/src/main/java/com/ikms/common/api/`.
- Audit extension point exists in `backend/src/main/java/com/ikms/audit/AuditService.java`.
- Local user/auth domain, permission mapping, session auth endpoints, and bootstrap users exist in `backend/src/main/java/com/ikms/security/`.
- Database-backed app settings exist in `backend/src/main/java/com/ikms/config/`.
- File storage abstraction exists in `backend/src/main/java/com/ikms/storage/FileStorageService.java`.
- Frontend now has a fetch client, `/api/auth/me`-driven protected routing, and a role-aware shell in `frontend/src/`.
- Local PostgreSQL/pgvector runtime exists in `infra/docker-compose.yml`.
- Developer onboarding notes exist in `docs/10-operations/local-development.md`.
- The repository had been at the initial spec/artifacts commit before the scaffold checkpoint was committed.
- Repository workflow now defaults to direct commits on `main`; PR review is optional and not required.
- Current development process guidance lives in `docs/10-operations/pull-request-workflow.md`.
- Default bootstrap users: `indexer`, `processor`, `supervisor`, `admin` with password `ChangeMe123!` unless bootstrap is disabled.
- `main` is the source of truth and includes the UI guideline reference commit `6f30b50`.
- The local session/CORS usability fix is now part of the working baseline so the frontend can call the backend from `http://localhost:5173`.
- Backend integration test support now exists in `backend/src/test/java/com/ikms/support/PostgresIntegrationTest.java` with a smoke test proving pgvector-backed startup.
- Frontend test setup now resets DOM and mocked globals between test runs in `frontend/src/test/setup.ts`.
- Foundation review artifact exists at `docs/09-testing/reviews/foundation-review.md`.
- Generated `frontend/vite.config.js` and `frontend/vite.config.d.ts` are gitignored and should not be committed.
- CSV import validation/API/UI baseline now exists in `backend/src/main/java/com/ikms/client/` and `frontend/src/features/clients/import/`.
- Authentication governance now applies failed-login tracking, lockout, session timeout, and login audit via `backend/src/main/java/com/ikms/security/AuthenticationGovernanceService.java`.
- Shared security trimming contract now exists in `backend/src/main/java/com/ikms/security/SecurityTrimService.java`.
- Retention workflow policy contract now exists in `backend/src/main/java/com/ikms/retention/RetentionWorkflowService.java`.
- Lightweight SLA policy checks exist in `backend/src/main/java/com/ikms/performance/V1SlaPolicy.java`.
- Hardening review artifact exists at `docs/09-testing/reviews/hardening-review.md`.
- Known follow-up gaps from hardening review:
  - CSV import validates and reports results but does not yet persist Client records.
  - Security trim is a policy contract and is not yet wired into document/search/AI endpoints.
  - Retention workflow is policy-level only and not yet connected to stored records/files.
- Client and note domain baseline now exists in `backend/src/main/java/com/ikms/client/` and `backend/src/main/java/com/ikms/note/`.
- Client profile MVP endpoints now exist in `backend/src/main/java/com/ikms/client/ClientController.java`.
- Frontend client workspace and client profile routes now exist in `frontend/src/features/clients/`.
- Client profile review artifact exists at `docs/09-testing/reviews/us1-client-profile-review.md`.
- Known follow-up gaps from the client profile review:
  - CSV import is still not persisted into the real client model.
  - Notes support create/list only; edit/delete remains unimplemented.
  - Documents, emails, AI Q&A, and activity sections are placeholders pending later user stories.
- Intake/review schema and entity baseline now exists in `backend/src/main/java/com/ikms/document/`, `backend/src/main/java/com/ikms/email/`, and `backend/src/main/java/com/ikms/review/`.
- Duplicate detection, manual upload/original preservation, and document versioning rules now exist in `backend/src/main/java/com/ikms/document/`.
- Extraction/classification adapters, shared folder + IMAP workers, email attachment linking, and review queue endpoints now exist in `backend/src/main/java/com/ikms/ai/`, `backend/src/main/java/com/ikms/worker/`, `backend/src/main/java/com/ikms/email/`, and `backend/src/main/java/com/ikms/review/`.
- Live intake API bindings now exist in `frontend/src/api/intake.ts`.
- `frontend/src/features/intake/IntakePage.tsx` now performs multipart PDF/DOCX upload, shows duplicate outcomes, and exposes intake routing controls.
- `frontend/src/features/intake/review/ReviewQueuePage.tsx` now lists queue items, filters by status/reason, links clients, corrects metadata, and approves/rejects items.
- Client profile document/email sections now live in `frontend/src/features/clients/knowledge/`.
- Backend runtime now includes multipart upload and client knowledge list endpoints plus a local filesystem storage bean in `backend/src/main/java/com/ikms/document/DocumentController.java`, `backend/src/main/java/com/ikms/email/EmailController.java`, and `backend/src/main/java/com/ikms/storage/LocalFileStorageService.java`.
- Intake/review review artifact exists at `docs/09-testing/reviews/us2-intake-review.md`.
- Known follow-up gaps from the intake/review review:
  - Review detail payloads still expose queue metadata only, not richer extracted evidence.
  - Shared-folder and IMAP status cards are not yet backed by live worker telemetry endpoints.
- PII masking now applies to client profile and email summary responses for users without `VIEW_PII`.
- Redacted/original document preview and download routing now exists in `backend/src/main/java/com/ikms/document/DocumentAccessController.java`.
- Placeholder document redaction generation now exists in `backend/src/main/java/com/ikms/document/DocumentRedactionService.java`.
- The client knowledge UI now distinguishes processor redacted actions from supervisor original actions in `frontend/src/features/clients/knowledge/`.
- PII protection review artifact exists at `docs/09-testing/reviews/us4-pii-protection-review.md`.
- Known follow-up gaps from the PII protection review:
  - Redaction generation is still placeholder text output, not file-format-aware PDF/DOCX redaction.
  - Document `containsPii` behavior is still inferred coarsely from client linkage rather than explicit metadata/config flags.
- Client-scoped search and AI Q&A now exist in `backend/src/main/java/com/ikms/search/`, `backend/src/main/java/com/ikms/ai/`, and `frontend/src/features/search/`.
- `embedding_chunk` and `ai_interaction` baseline persistence now exist in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`.
- Search and AI review artifact exists at `docs/09-testing/reviews/us3-search-ai-review.md`.
- Known follow-up gaps from the search/AI review:
  - Retrieval remains keyword-driven with placeholder chunk indexing, not real vector similarity.
  - Answer synthesis is rule-based and not yet provider-backed.
- Administration config entities, endpoints, and UI now exist in `backend/src/main/java/com/ikms/config/` and `frontend/src/features/admin/`.
- Admin configuration review artifact exists at `docs/09-testing/reviews/us5-administration-review.md`.
- Known follow-up gaps from the administration review:
  - User management is read-only in this slice.
  - Secret-bearing mailbox/provider credential handling is not implemented yet.
- Audit events now persist into `audit_log` through `backend/src/main/java/com/ikms/audit/LoggingAuditService.java`.
- Audit search/export services and controller now exist in `backend/src/main/java/com/ikms/audit/`.
- Audit API bindings and UI now exist in `frontend/src/api/audit.ts` and `frontend/src/features/audit/`.
- Audit and governance review artifact exists at `docs/09-testing/reviews/us6-audit-governance-review.md`.
- Known follow-up gaps from the audit/governance review:
  - Audit filtering is intentionally narrow and does not yet include category/outcome facets.
  - CSV export is previewed in-page instead of downloaded as a browser file.
- Playwright quickstart coverage now exists in `frontend/tests/quickstart.spec.ts` with config in `frontend/playwright.config.ts`.
- Backend local validation seed data now exists in `backend/src/main/resources/db/dev/V900__dev_seed.sql`.
- Quickstart validation summary now exists in `docs/13-worklog/worklog.md`.
- Release changelog now reflects delivered foundation, story, and polish slices in `docs/14-release/changelog.md`.
- Release readiness review now exists in `docs/14-release/release-readiness-review.md`.
- Current remaining practical gaps after closeout:
  - Playwright dependency was added to `frontend/package.json`, but a local install and browser download were not executed in this session.
  - Full manual quickstart signoff still depends on a live backend/frontend/database run, especially for IMAP/shared-folder intake.
  - Several V1 implementation compromises remain intentionally documented from earlier reviews: placeholder redaction output, keyword-only retrieval, rule-based answer synthesis, read-only user admin, and in-page CSV preview.

Current post-convergence implementation checkpoint:

- Notes now support update and soft-delete via `backend/src/main/java/com/ikms/note/NoteController.java` and corresponding client profile UI flows.
- Review correction now persists document type and metadata values through `backend/src/main/java/com/ikms/review/ReviewQueueService.java` and `backend/src/main/java/com/ikms/config/domain/MetadataValue.java`.
- PII sensitivity is now driven by metadata field flags through `backend/src/main/java/com/ikms/security/ContentSensitivityService.java`, not by blanket client linkage.
- Review routing now uses administrator-configured thresholds/modes through `backend/src/main/java/com/ikms/review/ReviewRoutingService.java`.
- AI provider settings are now consumed by extraction/classification/index metadata helpers through `backend/src/main/java/com/ikms/ai/AiProviderSettingsService.java`.
- Admin AI settings UI now supports `providerName`, `modelName`, `apiBaseUrl`, `apiKey`, and `ocrProvider`; the read API exposes only `apiKeyConfigured`, not the raw secret.
- Client search now ranks persisted chunks and document metadata, but it still does not execute real pgvector similarity search.
- Client AI answers now block prompt-injection-tainted context, audit blocked evidence, and flag contradictory evidence patterns for manual review.
- Retention workflow state is now persisted in `backend/src/main/java/com/ikms/retention/RetentionRecord.java` and can execute controlled delete/anonymize actions for supported targets.

Current highest-value remaining gaps:

- Extraction now supports real PDF/DOCX parsing, but OCR-specific external adapter coverage for image-only/scanned files is still missing.
- AI answer assembly is still rule-based rather than provider-generated.
- Embedding retrieval now attempts provider-generated embeddings plus pgvector nearest-neighbor search, but still falls back to heuristic ranking when provider calls are unavailable.
- AI provider secret handling now exists for admin configuration and is consumed by classification/embedding clients, but live provider validation is still pending.

Updated state after the latest `T131-T132` slice:

- `TextExtractionService` now uses PDFBox for PDF parsing and Apache POI for DOCX parsing before falling back to plain text decoding.
- `AiProviderClient` now exists in `backend/src/main/java/com/ikms/ai/AiProviderClient.java` and calls provider-compatible `/chat/completions` and `/embeddings` endpoints using stored admin AI settings.
- `EmbeddingChunk` now persists `embeddingVector`, and `ClientSearchService` now attempts pgvector similarity ranking via SQL before falling back to token overlap.
- Manual upload, shared-folder intake, email attachment ingestion, and note updates now trigger indexing more consistently than before.
- Review-linking for documents/emails now also triggers indexing so previously unlinked content can enter search/RAG after manual resolution.

Start the next session by reviewing `git status`, confirming `main` is clean and pushed through the Phase 9 closeout slice, then choose between live quickstart validation hardening or post-V1 enhancements.

Recommended next implementation slice:

- Validate the new provider-backed extraction/embedding path against a live configured LLM endpoint and, if needed, add OCR-specific provider adapters for scanned-document cases.
