# Tasks: Insurance Broker IKMS V1 Requirements Baseline

**Input**: Design documents from `specs/001-insurance-broker-ikms/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Included because the SDS workflow requires test cases and `quickstart.md` defines end-to-end validation scenarios.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently after the shared foundation is ready.

## Phase 1: Setup

**Purpose**: Initialize the monorepo, tooling, and local runtime skeleton.

- [x] T001 Create backend Spring Boot project structure in `backend/`
- [x] T002 Create frontend React/Vite project structure in `frontend/`
- [x] T003 [P] Create local PostgreSQL/pgvector Docker setup in `infra/docker-compose.yml`
- [x] T004 [P] Create backend environment template in `backend/src/main/resources/application.yml`
- [x] T005 [P] Create frontend environment template in `frontend/.env.example`
- [x] T006 Configure backend build, test, and dependency management in `backend/pom.xml`
- [x] T007 Configure frontend build, test, lint, and formatting in `frontend/package.json`
- [x] T008 Add repository developer quickstart notes in `docs/10-operations/local-development.md`

---

## Phase 2: Foundational

**Purpose**: Build shared infrastructure that blocks all user stories.

**Critical**: No user story implementation should start until this phase is complete.

- [x] T009 Create initial database migration framework in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [x] T010 Implement shared API error response model in `backend/src/main/java/com/ikms/common/api/ApiError.java`
- [x] T011 Implement global exception handling in `backend/src/main/java/com/ikms/common/api/GlobalExceptionHandler.java`
- [x] T012 Implement audit event writer interface in `backend/src/main/java/com/ikms/audit/AuditService.java`
- [x] T013 Implement local user, role, and permission model in `backend/src/main/java/com/ikms/security/domain/`
- [x] T014 Implement Spring Security username/password authentication in `backend/src/main/java/com/ikms/security/`
- [x] T015 Implement role permission checks for Indexer, Processor, Supervisor, and Administrator in `backend/src/main/java/com/ikms/security/PermissionService.java`
- [x] T016 Implement file storage abstraction for originals and redacted files in `backend/src/main/java/com/ikms/storage/FileStorageService.java`
- [x] T017 Implement database-backed configuration registry in `backend/src/main/java/com/ikms/config/AppSettingService.java`
- [x] T018 Implement frontend API client foundation in `frontend/src/api/client.ts`
- [x] T019 Implement frontend route shell and protected route handling in `frontend/src/app/`
- [x] T020 [P] Add backend Testcontainers setup for PostgreSQL/pgvector in `backend/src/test/java/com/ikms/support/PostgresIntegrationTest.java`
- [x] T021 [P] Add frontend test setup in `frontend/src/test/setup.ts`
- [x] T022 Complete Foundation code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/foundation-review.md`

**Checkpoint**: Authentication, authorization hooks, storage abstraction, migration framework, audit writer, and app shell are ready.

---

## Phase 2A: Pre-Implementation Hardening

**Purpose**: Close pre-implementation gaps found during constitution and artifact review.

**Critical**: Complete these tasks before broad document access, search, RAG, or administrative workflows are considered implementation-ready.

- [x] T023 Add explicit CSV client import tests in `backend/src/test/java/com/ikms/client/ClientImportTest.java`
- [x] T024 [US1] Implement CSV client import validation, duplicate warnings, and audit events in `backend/src/main/java/com/ikms/client/ClientImportService.java`
- [x] T025 [US1] Add CSV import API and UI entry points in `backend/src/main/java/com/ikms/client/ClientImportController.java` and `frontend/src/features/clients/import/`
- [x] T026 Add account status, failed-login tracking, lockout, session timeout, and login audit tests in `backend/src/test/java/com/ikms/security/AuthenticationGovernanceTest.java`
- [x] T027 Extend authentication implementation for account status, failed-login tracking, lockout, session timeout, and login audit in `backend/src/main/java/com/ikms/security/`
- [x] T028 Add foundation-level security trimming tests for search, preview, download, and AI context assembly in `backend/src/test/java/com/ikms/security/SecurityTrimBoundaryTest.java`
- [x] T029 Implement shared security-trim contract before document access, search retrieval, and AI context assembly in `backend/src/main/java/com/ikms/security/SecurityTrimService.java`
- [x] T030 Add retention, legal hold, deletion, and anonymization tests in `backend/src/test/java/com/ikms/retention/RetentionWorkflowTest.java`
- [x] T031 Implement retention policy, legal hold, controlled deletion, and anonymization workflow services in `backend/src/main/java/com/ikms/retention/`
- [x] T032 Add lightweight performance/SLA validation for client profile open and intake routing in `backend/src/test/java/com/ikms/performance/V1SlaValidationTest.java`
- [x] T033 Complete Pre-Implementation Hardening code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/hardening-review.md`

**Checkpoint**: FR-005, FR-030, FR-032, FR-039, SC-001, and SC-002 have explicit implementation and validation coverage.

---

## Phase 3: User Story 1 - Build A Client Knowledge Profile (Priority: P1) MVP

**Goal**: Authorized users can create/find clients, handle temporary ClientID replacement, add notes, and view the client profile sections.

**Independent Test**: Create a client with temporary ClientID, replace it with actual ClientID, add a note, link sample client knowledge, and confirm the profile sections render.

### Tests For User Story 1

- [x] T034 [P] [US1] Add client API contract tests in `backend/src/test/java/com/ikms/client/ClientControllerContractTest.java`
- [x] T035 [P] [US1] Add client profile UI tests in `frontend/src/features/clients/ClientProfile.test.tsx`
- [x] T036 [P] [US1] Add note API tests in `backend/src/test/java/com/ikms/note/NoteControllerContractTest.java`

### Implementation For User Story 1

- [x] T037 [P] [US1] Create Client JPA entity and repository in `backend/src/main/java/com/ikms/client/`
- [x] T038 [P] [US1] Create Note JPA entity and repository in `backend/src/main/java/com/ikms/note/`
- [x] T039 [US1] Extend baseline migration for clients and notes in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [x] T040 [US1] Implement temporary ClientID generation and uniqueness validation in `backend/src/main/java/com/ikms/client/ClientIdService.java`
- [x] T041 [US1] Implement client profile service in `backend/src/main/java/com/ikms/client/ClientService.java`
- [x] T042 [US1] Implement note service with audit events in `backend/src/main/java/com/ikms/note/NoteService.java`
- [x] T043 [US1] Implement client and note REST endpoints in `backend/src/main/java/com/ikms/client/ClientController.java`
- [x] T044 [US1] Implement client search and profile API bindings in `frontend/src/api/clients.ts`
- [x] T045 [US1] Implement client list, create/edit, and profile pages in `frontend/src/features/clients/`
- [x] T046 [US1] Implement notes section in `frontend/src/features/clients/notes/`
- [x] T047 [US1] Complete Client Profile code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us1-client-profile-review.md`

**Checkpoint**: Client profile MVP is usable without document intake, AI, or advanced administration.

---

## Phase 4: User Story 2 - Ingest And Review Client Knowledge (Priority: P1)

**Goal**: Indexers can intake PDF/DOCX/email knowledge, preserve originals, detect duplicates, and resolve unlinked review items.

**Independent Test**: Upload a PDF without a client, see it in review, manually link it to a client, correct metadata, approve it, and see it on the client profile.

### Tests For User Story 2

- [x] T048 [P] [US2] Add document upload and duplicate tests in `backend/src/test/java/com/ikms/document/DocumentUploadTest.java`
- [x] T049 [P] [US2] Add review queue workflow tests in `backend/src/test/java/com/ikms/review/ReviewQueueWorkflowTest.java`
- [x] T050 [P] [US2] Add intake UI tests in `frontend/src/features/intake/IntakePage.test.tsx`
- [x] T051 [P] [US2] Add review queue UI tests in `frontend/src/features/intake/ReviewQueue.test.tsx`

### Implementation For User Story 2

- [x] T052 [P] [US2] Create Document and DocumentVersion entities in `backend/src/main/java/com/ikms/document/`
- [x] T053 [P] [US2] Create Email entity in `backend/src/main/java/com/ikms/email/`
- [x] T054 [P] [US2] Create ReviewQueueItem entity in `backend/src/main/java/com/ikms/review/`
- [x] T055 [US2] Extend migration for documents, versions, emails, and review queue in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [x] T056 [US2] Implement hash-based duplicate detection in `backend/src/main/java/com/ikms/document/DuplicateDetectionService.java`
- [x] T057 [US2] Implement manual upload and original preservation in `backend/src/main/java/com/ikms/document/DocumentUploadService.java`
- [x] T058 [US2] Implement document versioning rules in `backend/src/main/java/com/ikms/document/DocumentVersionService.java`
- [x] T059 [US2] Implement text extraction adapter for PDF/DOCX in `backend/src/main/java/com/ikms/worker/extract/TextExtractionService.java`
- [x] T060 [US2] Implement AI classification and metadata extraction adapter in `backend/src/main/java/com/ikms/ai/ClassificationService.java`
- [x] T061 [US2] Implement shared folder polling worker in `backend/src/main/java/com/ikms/worker/intake/SharedFolderIntakeWorker.java`
- [x] T062 [US2] Implement IMAP mailbox intake worker in `backend/src/main/java/com/ikms/worker/intake/ImapIntakeWorker.java`
- [x] T063 [US2] Implement email attachment-to-document linking in `backend/src/main/java/com/ikms/email/EmailAttachmentService.java`
- [x] T064 [US2] Implement review queue services and endpoints in `backend/src/main/java/com/ikms/review/`
- [x] T065 [US2] Implement document, email, and review queue API bindings in `frontend/src/api/intake.ts`
- [x] T066 [US2] Implement manual upload, intake status, and duplicate result UI in `frontend/src/features/intake/`
- [x] T067 [US2] Implement review queue link/correct/approve UI in `frontend/src/features/intake/review/`
- [x] T068 [US2] Add document and email sections to client profile in `frontend/src/features/clients/knowledge/`
- [x] T069 [US2] Complete Intake and Review code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us2-intake-review.md`

**Checkpoint**: Intake and review create searchable client-linked knowledge records, even before RAG is enabled.

---

## Phase 5: User Story 4 - Protect PII With Role-Based Access (Priority: P1)

**Goal**: Processor users receive masked/redacted access only, while authorized Supervisors can access originals and PII.

**Independent Test**: Mark metadata as PII, compare Processor and Supervisor preview/download/search/AI behavior, and confirm PII access is audited.

### Tests For User Story 4

- [x] T070 [P] [US4] Add PII masking and permission tests in `backend/src/test/java/com/ikms/security/PiiAccessControlTest.java`
- [x] T071 [P] [US4] Add redacted preview/download tests in `backend/src/test/java/com/ikms/document/RedactedDocumentAccessTest.java`
- [x] T072 [P] [US4] Add Processor/Supervisor UI permission tests in `frontend/src/features/clients/PiiVisibility.test.tsx`

### Implementation For User Story 4

- [x] T073 [US4] Implement metadata PII masking service in `backend/src/main/java/com/ikms/security/PiiMaskingService.java`
- [x] T074 [US4] Implement document redaction adapter and failure states in `backend/src/main/java/com/ikms/document/DocumentRedactionService.java`
- [x] T075 [US4] Enforce redacted preview/download routing in `backend/src/main/java/com/ikms/document/DocumentAccessController.java`
- [x] T076 [US4] Enforce PII filtering before search retrieval and AI context assembly in `backend/src/main/java/com/ikms/security/SecurityTrimService.java`
- [x] T077 [US4] Add PII/original access audit events in `backend/src/main/java/com/ikms/audit/AuditService.java`
- [x] T078 [US4] Add frontend masked-field and redacted-action states in `frontend/src/features/clients/knowledge/`
- [x] T079 [US4] Complete PII Protection code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us4-pii-protection-review.md`

**Checkpoint**: Processor cannot receive unredacted PII through profile, preview, download, search, or AI paths.

---

## Phase 6: User Story 3 - Search And Ask Questions About One Client (Priority: P1)

**Goal**: Processor and Supervisor users can search and ask evidence-based questions within one client profile.

**Independent Test**: Link documents/emails to one client, ask a client-level question, verify citations, no-evidence behavior, conflict handling, and decision refusal.

### Tests For User Story 3

- [x] T080 [P] [US3] Add client search API tests in `backend/src/test/java/com/ikms/search/ClientSearchTest.java`
- [x] T081 [P] [US3] Add RAG answer guardrail tests in `backend/src/test/java/com/ikms/ai/ClientQuestionAnsweringTest.java`
- [x] T082 [P] [US3] Add search and Q&A UI tests in `frontend/src/features/search/ClientSearchAsk.test.tsx`

### Implementation For User Story 3

- [x] T083 [P] [US3] Create EmbeddingChunk and AIInteraction entities in `backend/src/main/java/com/ikms/ai/`
- [x] T084 [US3] Extend migration for embedding chunks and AI interactions in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [x] T085 [US3] Implement chunking and embedding pipeline in `backend/src/main/java/com/ikms/ai/EmbeddingIndexService.java`
- [x] T086 [US3] Implement keyword, metadata, and vector retrieval in `backend/src/main/java/com/ikms/search/ClientSearchService.java`
- [x] T087 [US3] Implement security-trimmed RAG context assembly in `backend/src/main/java/com/ikms/ai/RagContextService.java`
- [x] T088 [US3] Implement no-evidence, citation, conflict, and decision-refusal logic in `backend/src/main/java/com/ikms/ai/ClientQuestionAnsweringService.java`
- [x] T089 [US3] Implement search and ask endpoints in `backend/src/main/java/com/ikms/search/ClientSearchController.java`
- [x] T090 [US3] Implement AI feedback endpoint in `backend/src/main/java/com/ikms/ai/AiFeedbackController.java`
- [x] T091 [US3] Implement search and ask API bindings in `frontend/src/api/search.ts`
- [x] T092 [US3] Implement client-scoped search and AI Q&A panel in `frontend/src/features/search/`
- [x] T093 [US3] Complete Search and AI Q&A code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us3-search-ai-review.md`

**Checkpoint**: Client-level RAG works with citations and guardrails against no-evidence and prohibited decisions.

---

## Phase 7: User Story 5 - Configure Broker Knowledge Rules (Priority: P2)

**Goal**: Administrators can configure document types, metadata labels, PII flags, intake sources, review behavior, users, roles, and AI provider settings.

**Independent Test**: Configure document type, mark metadata PII, configure mailbox and shared folder, set review mode, and confirm new intake follows settings.

### Tests For User Story 5

- [x] T094 [P] [US5] Add admin configuration API tests in `backend/src/test/java/com/ikms/admin/AdminConfigurationTest.java`
- [x] T095 [P] [US5] Add admin UI tests in `frontend/src/features/admin/AdminConfiguration.test.tsx`

### Implementation For User Story 5

- [x] T096 [P] [US5] Create DocumentType and MetadataField entities in `backend/src/main/java/com/ikms/config/domain/`
- [x] T097 [P] [US5] Create intake and AI setting entities in `backend/src/main/java/com/ikms/config/domain/`
- [x] T098 [US5] Extend migration for configuration tables in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [x] T099 [US5] Implement document type and metadata configuration services in `backend/src/main/java/com/ikms/config/KnowledgeConfigurationService.java`
- [x] T100 [US5] Implement shared folder, IMAP, review mode, and AI provider settings services in `backend/src/main/java/com/ikms/config/IntakeAiConfigurationService.java`
- [x] T101 [US5] Implement admin configuration endpoints in `backend/src/main/java/com/ikms/config/AdminConfigurationController.java`
- [x] T102 [US5] Implement admin API bindings in `frontend/src/api/admin.ts`
- [x] T103 [US5] Implement admin configuration screens in `frontend/src/features/admin/`
- [x] T104 [US5] Complete Administration code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us5-administration-review.md`

**Checkpoint**: Administrator can configure V1 broker rules without code changes.

---

## Phase 8: User Story 6 - Audit And Govern System Activity (Priority: P2)

**Goal**: Authorized users can search and export audit logs for authentication, intake, review, document access, PII access, configuration, and AI activity.

**Independent Test**: Perform representative actions, filter audit logs by user/date/action/client, and export CSV.

### Tests For User Story 6

- [x] T105 [P] [US6] Add audit search and export tests in `backend/src/test/java/com/ikms/audit/AuditExportTest.java`
- [x] T106 [P] [US6] Add audit UI tests in `frontend/src/features/audit/AuditPage.test.tsx`

### Implementation For User Story 6

- [x] T107 [US6] Finalize AuditLog entity and indexes in `backend/src/main/java/com/ikms/audit/AuditLog.java`
- [x] T108 [US6] Extend migration for audit retention and search indexes in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [x] T109 [US6] Implement audit search service in `backend/src/main/java/com/ikms/audit/AuditSearchService.java`
- [x] T110 [US6] Implement audit CSV export in `backend/src/main/java/com/ikms/audit/AuditExportService.java`
- [x] T111 [US6] Implement audit endpoints in `backend/src/main/java/com/ikms/audit/AuditController.java`
- [x] T112 [US6] Implement audit API bindings in `frontend/src/api/audit.ts`
- [x] T113 [US6] Implement audit search and export UI in `frontend/src/features/audit/`
- [x] T114 [US6] Complete Audit and Governance code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us6-audit-governance-review.md`

**Checkpoint**: Audit activity is searchable and exportable for governance review.

---

## Phase 9: Polish And Cross-Cutting Validation

**Purpose**: Confirm the whole V1 behaves as specified and is ready for implementation handoff or demo.

- [x] T115 [P] Add Playwright quickstart scenario coverage in `frontend/tests/quickstart.spec.ts`
- [x] T116 [P] Add backend seed data for local validation in `backend/src/main/resources/db/dev/V900__dev_seed.sql`
- [x] T117 Run and document quickstart validation results in `docs/13-worklog/worklog.md`
- [x] T118 Update AI handoff with built status and remaining gaps in `docs/11-handoff/ai-handoff.md`
- [x] T119 Update changelog for implemented feature slices in `docs/14-release/changelog.md`
- [x] T120 Review security, audit, PII, and guardrail coverage against `docs/08-security/security-baseline.md`
- [x] T121 Complete release readiness review using `docs/14-release/release-readiness-review-template.md` and save results in `docs/14-release/release-readiness-review.md`

---

## Dependencies And Execution Order

### Phase Dependencies

- Setup has no dependencies.
- Foundational depends on Setup and blocks all user stories.
- Pre-Implementation Hardening depends on Foundational and blocks broad document access, search, RAG, and retention-sensitive flows.
- P1 stories should be delivered before P2 stories.
- Polish depends on the desired story set being complete.

### User Story Dependencies

- US1 is the MVP and should be built first after Foundational and applicable Phase 2A tasks.
- US2 depends on foundational storage/security and benefits from US1 client profile.
- US4 security trimming and PII controls must be implemented before US3 is enabled for Processor users.
- US3 depends on US1 client profile, US2 indexed knowledge, and US4 security/PII controls.
- US5 can start after foundation, but is most useful after US2 exposes configurable intake/review points.
- US6 can start after foundation, but final validation needs events emitted by US1-US5.

### Parallel Opportunities

- Setup tasks T003-T005 can run in parallel.
- Foundational tasks T020-T021 can run in parallel with non-conflicting backend work.
- Test tasks within each story can run in parallel.
- Backend entity tasks in each story marked [P] can run in parallel.
- Frontend API bindings and page components can proceed once matching backend contract names are stable.

## Parallel Example: User Story 2

```text
Task: "Add document upload and duplicate tests in backend/src/test/java/com/ikms/document/DocumentUploadTest.java"
Task: "Add review queue workflow tests in backend/src/test/java/com/ikms/review/ReviewQueueWorkflowTest.java"
Task: "Add intake UI tests in frontend/src/features/intake/IntakePage.test.tsx"
Task: "Add review queue UI tests in frontend/src/features/intake/ReviewQueue.test.tsx"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 2A tasks that apply to US1 and authentication.
3. Complete US1 client profile.
4. Stop and validate Client creation, CSV import, temporary ClientID replacement, notes, and profile sections.

### Practical V1 Increment

1. Add US2 intake and review.
2. Add US4 PII controls before broad document access and before Processor search/RAG use.
3. Add US3 search and client-level Q&A after security trimming is active.
4. Add US5 administration.
5. Add US6 audit search/export.

### Validation

Use `specs/001-insurance-broker-ikms/quickstart.md` as the acceptance validation guide after each completed slice.

## Phase 10: Convergence

- [x] T122 Implement note update and delete or soft-delete flows, including REST endpoints, UI actions, and audit history, per FR-007 (missing)
- [x] T123 Expand review queue resolution to capture and persist document type selection and metadata correction during manual review, per FR-017, FR-019, FR-021, and US2/AC2 (partial)
- [x] T124 Persist configured metadata values and PII field flags on client knowledge items, and drive masking/redaction decisions from those flags instead of treating all linked content as PII, per FR-018, FR-033, FR-034, and FR-035 (contradicts)
- [x] T125 Apply configured review modes and low-confidence thresholds in shared-folder and IMAP intake routing so review behavior follows administrator settings, per FR-020, FR-022, and US5/AC3 (missing)
- [ ] T126 Replace placeholder shared-folder, IMAP, extraction, and classification logic with configured provider-backed intake processing that supports production PDF/DOCX/email handling and English/German content workflows, per FR-009, FR-010, FR-019, and FR-040 (historical partial; completed in follow-up task T131 except for external OCR service integration)
- [ ] T127 Implement metadata and semantic/vector retrieval using persisted embedding data, and use that retrieval in client-scoped RAG context assembly instead of keyword-only search, per FR-023 and plan: PostgreSQL/pgvector retrieval (historical partial; completed in follow-up task T132 with provider-backed embeddings and pgvector query path)
- [x] T128 Implement evidence conflict detection and source-traceable AI answer assembly based on contradictory facts rather than citation count alone, per FR-027 and SC-005 (partial)
- [x] T129 Detect, flag, and audit prompt injection risk during ingestion or retrieval and prevent flagged content from influencing AI answers, per Edge Cases and Constitution Security And AI Governance (missing)
- [x] T130 Persist operational retention and legal-hold workflow state beyond audit-only approval checks, including controlled delete/anonymize execution paths, per FR-039 (partial)
- [x] T131 Add real OCR/PDF/DOCX extraction adapters and mailbox/shared-folder content parsing driven by configured providers, replacing UTF-8/fallback extraction stubs end-to-end
- [x] T132 Generate and persist real embeddings, then replace token-overlap chunk ranking with pgvector similarity retrieval in client search and RAG context assembly
- [x] T133 Add dedicated embedding-model configuration across admin settings, provider settings, and embedding generation so vector indexing does not depend on the chat/classification model
- [x] T134 Replace character-based chunking with context-preserving semantic chunks that persist chunk metadata such as chunk index, token count, source title/section, language, page number, and retrieval summary for hybrid search
- [x] T135 Add page-aware PDF evidence provenance, local chunk-neighbor context expansion, and page/location-aware AI citations so cross-document client conversations can cite document name and page/location metadata

## Phase 11: Convergence

- [x] T136 Add administrator-facing AI/OCR provider validation, timeout/error handling, and audit coverage so configured endpoints, credentials, and required models can be verified before use, per US5/AC4, FR-037, and FR-040a (missing)
- [x] T137 Add OCR-provider-backed extraction for scanned or image-only PDFs, retain page-aware provenance for OCR segments, and route low-confidence OCR results to review, per US2/AC5, FR-019a, FR-020, and FR-040 (partial)
- [x] T138 Replace rule-based client answer assembly with provider-generated, evidence-constrained answer synthesis while preserving refusal, no-evidence, conflict, and citation behavior, per US3/AC1, US3/AC2, US3/AC3, FR-025b, FR-026, FR-027, and SC-008 (partial)
- [x] T139 Harden hybrid retrieval fallback and observability by surfacing provider degradation, retrieval path selection, and citation-quality validation for client-scoped cross-document conversations, per FR-023, FR-025a, FR-027a, SC-003a, and SC-005 (partial)

## Phase 12: Enterprise Document Viewer

- [x] T140 Create a reusable enterprise document viewer architecture for Search, Customer360, Review, and future assistant/admin surfaces, including shared toolbar, viewer states, thumbnails rail, and evidence panel shells
- [x] T141 Add non-persistent extension-layer contracts for OCR overlays, search highlights, and annotations so future document intelligence features can attach without route or API redesign
- [x] T142 Replace the one-off Review detail preview with the shared enterprise document viewer while preserving existing review workflow controls and metadata editing
- [x] T143 Integrate the shared enterprise document viewer into Customer360 document interactions and Search result evidence workflows using only existing APIs and available demo data
- [x] T144 Add frontend validation coverage for enterprise document viewer states, workspace integrations, and responsive drawer/sheet behavior

## Phase 13: Frontend Task List - AI Evidence Workspace

- [x] T145 Create reusable AI Evidence Workspace UI primitives for confidence, section headers, AI summaries, evidence cards, metadata groups, and panel composition in `frontend/src/app/components/document-viewer/`
- [x] T146 Refactor the shared enterprise viewer evidence panel into collapsible operational sections with accessible keyboard and screen-reader support, using only existing frontend data
- [x] T147 Re-map Review, Customer360, and Search viewer evidence payloads into the new AI Evidence Workspace structure with visual states for verified, needs review, missing, read-only, and available confidence hints
- [x] T148 Add future-facing evidence navigation hooks and presentation-only placeholders for jump-to-page, jump-to-region, and future highlight navigation without changing routes or APIs
- [x] T149 Add or update frontend tests for AI Evidence Workspace behavior, confidence rendering, collapsible sections, and integrated viewer usage across Review, Customer360, and Search

## Phase 14: Frontend Task List - Enterprise AI Assistant Workspace

- [x] T150 Create reusable Enterprise AI Assistant UI primitives for toolbar, conversation view, messages, thinking state, suggested questions, evidence references, and source chips in `frontend/src/app/components/assistant-panel/`
- [x] T151 Implement a reusable AssistantPanel container with desktop panel, tablet drawer, and mobile bottom-sheet presentation patterns using only existing frontend state and APIs
- [x] T152 Integrate the assistant workspace into Search, Customer360, and Review Detail using current workspace context only, with placeholders for unavailable conversation/history states and no fabricated AI content
- [x] T153 Add future-facing evidence and source reference contracts for jump-to-page, jump-to-region, jump-to-metadata, jump-to-customer, jump-to-policy-reference metadata, and source entity chips without changing routes or backend APIs
- [x] T154 Add or update frontend tests for assistant panel states, accessibility hooks, suggested question rendering, and embedded workspace integrations across Search, Customer360, and Review Detail

## Phase 15: Backend Task List - Enterprise Knowledge Retrieval & AI Orchestration

- [x] T155 Add orchestration-phase backend architecture notes and sequence design for authorization, intent detection, query planning, hybrid retrieval, ranking, context building, LLM execution, citation building, and guardrails in `docs/06-architecture/enterprise-ai-orchestration.md`
- [x] T156 Add backend integration and contract tests for orchestration flows, including search, ask, summarize, explain, compare, extract, and validate endpoints plus permission-aware guardrail cases in `backend/src/test/java/com/ikms/ai/EnterpriseAiOrchestrationTest.java`
- [x] T157 Extend persistence and migrations for conversation history, retrieval traces, citation jump targets, orchestration metrics, evaluation results, and provider execution telemetry in `backend/src/main/resources/db/migration/`
- [x] T158 Implement shared orchestration domain contracts and pipeline services for intent detection, query planning, hybrid retrieval coordination, evidence ranking, context assembly, citation building, and grounding validation in `backend/src/main/java/com/ikms/ai/orchestration/`
- [x] T159 Expand hybrid retrieval to support lexical, vector, metadata, entity, relationship, policy, claim, customer, document, and version search with permission-aware multi-document retrieval in `backend/src/main/java/com/ikms/search/`
- [x] T160 Implement enterprise context builder and token-budget management for system prompts, user context, retrieved evidence, metadata, conversation history, and provider-specific request shaping in `backend/src/main/java/com/ikms/ai/context/`
- [x] T161 Implement provider-agnostic LLM orchestration with streaming hooks, retries, timeouts, fallback models, provider failover, and future local-model adapters in `backend/src/main/java/com/ikms/ai/provider/`
- [x] T162 Extend citation, guardrail, and audit services to return document/page/chunk/section/confidence/evidence metadata with future jump target IDs while enforcing PII masking, prompt-injection blocking, hallucination mitigation, token limits, and grounding checks in `backend/src/main/java/com/ikms/ai/`
- [x] T163 Implement enterprise AI API contracts and controllers for search, ask, summarize, explain, compare, extract, and validate while reusing existing endpoints/contracts where possible in `backend/src/main/java/com/ikms/search/` and `backend/src/main/java/com/ikms/ai/`
- [x] T164 Add evaluation, benchmarking, and operational documentation for latency, retrieval precision, grounding score, citation coverage, answer quality, and feedback capture in `backend/src/test/java/com/ikms/ai/`, `docs/09-testing/`, and `docs/13-worklog/`

## Phase 16: Frontend Task List - Customer-Centric Architecture Consistency Correction

- [x] T165 Audit Phase 1 through Phase 4 frontend architecture, shared types, route assumptions, and UI documentation for any policy/claim ownership implications that violate the customer-centric IKMS boundary
- [x] T166 Correct frontend-only type names, assistant/source abstractions, and compatibility notes so policy and claim remain external references or metadata rather than IKMS-owned entities
- [x] T167 Update Customer360, Search, Review, viewer, and assistant labels or helper text to consistently describe policy and claim data as references, metadata, or related context without changing routes or workflows
- [x] T168 Add explicit architecture notes to frontend-facing UI and architecture documentation stating that policy and claim remain external references or metadata while the broker management system stays the system of record
- [x] T169 Run frontend validation for the architecture-alignment pass and record compatibility-preserving outcomes without introducing Policy360, Claim360, or standalone policy/claim workspaces

## Phase 17: Backend Task List - Customer-Centric Enterprise RAG & Knowledge Retrieval Platform

- [x] T170 Review and update backend architecture, search design, API contract, orchestration notes, and operational documentation so Customer remains the primary knowledge context and Policy/Claim are documented only as structured Business Reference Fields in `docs/06-architecture/`, `docs/10-operations/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T171 Add backend contract and integration tests for customer ask, global ask, business-reference search, evidence expansion, conversation continuation, streaming, timeout, and permission-leakage cases in `backend/src/test/java/com/ikms/ai/` and `backend/src/test/java/com/ikms/search/`
- [x] T172 Implement provider-independent business-reference extraction and typed query planning for customer scope, source types, document types, date ranges, business reference fields, version preference, evidence granularity, and sort order in `backend/src/main/java/com/ikms/ai/orchestration/`
- [x] T173 Refactor search retrieval to a customer-centric enterprise retrieval platform supporting lexical, vector, structured-field, business-reference, date-filtered, document-type-filtered, version-aware, and conversation-aware retrieval without introducing Policy or Claim entities in `backend/src/main/java/com/ikms/search/`
- [x] T174 Introduce first-class indexed business reference fields, ACL/security fields, and reindex-safe migration support for customer knowledge search while preserving the existing document-centric source model in `backend/src/main/resources/db/migration/`, `backend/src/main/java/com/ikms/ai/`, and `backend/src/main/java/com/ikms/document/`
- [x] T175 Implement fusion, reranking, duplicate suppression, chunk lineage, source diversity, freshness handling, and customer-aware ranking for grounded enterprise retrieval in `backend/src/main/java/com/ikms/search/`
- [x] T176 Extend context building, streaming orchestration, cancellation, timeout, fallback-model handling, local-LLM integration points, and restricted-content notices for customer-centric enterprise RAG in `backend/src/main/java/com/ikms/ai/context/`, `backend/src/main/java/com/ikms/ai/provider/`, and `backend/src/main/java/com/ikms/ai/orchestration/`
- [x] T177 Harden enterprise guardrails and citation modeling for prompt-injection blocking, permission-aware retrieval, PII protection, grounding validation, citation-coverage checks, safe failure, and evidence-first business-reference explanations in `backend/src/main/java/com/ikms/ai/`
- [x] T178 Extend backend APIs for customer ask, global ask, knowledge search, evidence expansion, conversation continuation, and streaming while reusing existing routes/contracts where practical and without introducing Policy or Claim APIs in `backend/src/main/java/com/ikms/search/` and `backend/src/main/java/com/ikms/ai/`
- [x] T179 Add retrieval evaluation, latency benchmarks, business-reference accuracy checks, streaming performance coverage, migration/reindex guidance, and operational validation notes for the enterprise retrieval platform in `backend/src/test/java/com/ikms/ai/`, `backend/src/test/java/com/ikms/search/`, `docs/09-testing/`, and `docs/13-worklog/`

## Phase 18: Documentation Task List - Retrieval Implementation Status Alignment

- [x] T180 Update Phase 5A blueprint and customer-centric retrieval architecture docs to distinguish current PostgreSQL plus pgvector implementation, approved target architecture, and future OpenSearch roadmap in `docs/06-architecture/`
- [x] T181 Update AI/RAG, operations, evaluation, and decision-log documentation to align benchmark scope, projection lifecycle, reindexing, fallback retrieval, and current implementation status in `docs/07-ai-rag/`, `docs/09-testing/`, `docs/10-operations/`, and `docs/12-decisions/`
- [x] T182 Record the documentation-only retrieval-status alignment in the worklog and maintain the task tracker without changing code, APIs, schema, or business logic in `docs/13-worklog/worklog.md` and `specs/001-insurance-broker-ikms/tasks.md`

## Phase 19: Backend Task List - End-to-End Enterprise Retrieval Validation & Optimization

- [x] T183 Build a Phase 5C architecture conformance matrix and retrieval implementation validation report covering Phase 5A, current implementation documentation, API contracts, and T171 through T182 in `docs/06-architecture/`, `docs/09-testing/`, and `docs/14-release/`
- [x] T184 Create realistic customer-centric enterprise retrieval evaluation datasets and executable benchmark fixtures for Business Reference extraction, query planning, retrieval quality, grounding, authorization, and degraded-mode behavior in `backend/src/test/java/com/ikms/ai/`, `backend/src/test/java/com/ikms/search/`, and `backend/src/test/java/com/ikms/security/`
- [x] T185 Extend retrieval validation coverage to measure Precision@K, Recall@K, MRR, NDCG, Hit Rate, fusion contribution, reranking contribution, version awareness, source diversity, and Business Reference relevance for PostgreSQL plus pgvector retrieval in `backend/src/test/java/com/ikms/search/` and `backend/src/test/java/com/ikms/ai/`
- [x] T186 Extend context, citation, and grounding validation coverage for token budgeting, duplicate suppression, chunk stitching, evidence density, citation correctness, unsupported-claim handling, and insufficient-evidence behavior in `backend/src/test/java/com/ikms/ai/` and `backend/src/main/java/com/ikms/ai/context/`
- [x] T187 Extend authorization, guardrail, streaming, and failure-path validation for customer isolation, restricted content, redaction boundaries, prompt injection, cancellation, timeout, fallback, stale projections, and partial retrieval in `backend/src/test/java/com/ikms/ai/`, `backend/src/test/java/com/ikms/security/`, and `backend/src/test/java/com/ikms/search/`
- [x] T188 Apply only evidence-based backend retrieval optimizations surfaced by Phase 5C validation, including query-path efficiency, ranking/context tuning, and observability-safe diagnostics, in `backend/src/main/java/com/ikms/search/`, `backend/src/main/java/com/ikms/ai/context/`, and `backend/src/main/java/com/ikms/ai/`
- [x] T189 Add Phase 5C performance, observability, release-readiness, and known-limitations documentation for the current PostgreSQL plus pgvector enterprise retrieval implementation in `docs/09-testing/`, `docs/10-operations/`, `docs/14-release/`, and `docs/13-worklog/`
- [x] T190 Run applicable backend unit tests, integration tests, retrieval evaluation suites, authorization tests, API contract tests, performance benchmarks, and build validation, then record the results in `docs/13-worklog/worklog.md` and `specs/001-insurance-broker-ikms/tasks.md`

## Phase 20: Customer Knowledge Timeline & Related Knowledge

- [x] T191 Define the Phase 6 customer knowledge timeline and related-knowledge architecture, typed contracts, event taxonomy, persistence strategy, and API surface without introducing Policy or Claim entities in `docs/06-architecture/`, `docs/10-operations/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T192 Implement backend timeline event contracts, filters, cursor pagination, and customer-scoped timeline assembly from supported knowledge sources, audit data, review data, and business-reference metadata in `backend/src/main/java/com/ikms/client/`, `backend/src/main/java/com/ikms/audit/`, `backend/src/main/java/com/ikms/review/`, and related packages
- [x] T193 Implement deterministic and similarity-based related-knowledge derivation, duplicate detection, version-lineage relationships, explainable ranking, and permission-aware response assembly using the existing PostgreSQL plus pgvector architecture in `backend/src/main/java/com/ikms/search/`, `backend/src/main/java/com/ikms/document/`, and related packages
- [x] T194 Extend backend APIs for customer knowledge timeline, related knowledge, source-level related navigation, viewer integrations, and assistant timeline context while preserving the customer-centric boundary and existing routes/contracts where practical in `backend/src/main/java/com/ikms/client/`, `backend/src/main/java/com/ikms/document/`, and `backend/src/main/java/com/ikms/ai/`
- [x] T195 Integrate the real API-backed customer knowledge timeline and related-knowledge capability into Customer360, the enterprise document viewer, and the assistant panel without redesigning existing workspaces in `frontend/src/features/clients/`, `frontend/src/app/components/document-viewer/`, `frontend/src/app/components/assistant-panel/`, and related API clients
- [x] T196 Add backend and frontend tests for timeline ordering, filtering, pagination, duplicate-event handling, relationship derivation, authorization trimming, viewer navigation, assistant context, and the invariant that Policy and Claim remain Business Reference Fields rather than entities in `backend/src/test/java/`, `frontend/src/features/`, and `frontend/src/app/components/`
- [x] T197 Update architecture, API, Customer360, viewer, assistant, security, operations, test-strategy, task-tracking, and worklog documentation for Phase 6 and record validation results in `docs/`, `specs/001-insurance-broker-ikms/tasks.md`, and `docs/13-worklog/worklog.md`

## Phase 21: Cross-Workspace Timeline And Related-Knowledge Integration

- [x] T198 Consolidate shared frontend customer-knowledge API helpers, query keys, and reusable knowledge contracts for timeline, related knowledge, source context, and document versions in `frontend/src/api/` and shared UI-facing types
- [x] T199 Integrate shared related-knowledge and version-history context into Search selection, Search viewer flows, and Search assistant context mapping without introducing row-level N+1 requests or redesigning Search in `frontend/src/features/search/` and shared viewer/assistant components
- [x] T200 Integrate shared related-knowledge, compact recent customer knowledge, and version-history context into Review Queue and Review Detail while preserving queue-first and review-first workflows in `frontend/src/features/intake/review/` and shared viewer/assistant components
- [x] T201 Align all document-viewer entry points to one related-knowledge and version-history evidence shape, add focused frontend/backend validation for lazy loading and restricted-source handling, and update docs/worklog/task tracking for Phase 6B in `frontend/src/app/components/document-viewer/`, `frontend/src/app/components/assistant-panel/`, `frontend/src/features/`, `backend/src/test/java/`, and `docs/`

## Phase 22: Enterprise Document Processing And Human Review Platform

- [x] T202 Define the Phase 7 processing architecture, stage responsibilities, retry/failure handling, audit/metrics expectations, and product-boundary guidance for OCR, classification, extraction, validation, review, and publishing in `docs/06-architecture/`, `docs/10-operations/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T203 Introduce backend processing-job domain models, persistence, API contracts, and orchestration services for document-processing lifecycle state, retry handling, review waiting, rejection, approval, and publish completion in `backend/src/main/java/com/ikms/document/`, `backend/src/main/resources/db/migration/`, and `backend/src/main/java/com/ikms/review/`
- [x] T204 Refactor OCR, classification, metadata extraction, Business Reference extraction, and confidence calculation into modular processing-stage outputs with reviewer-visible explanations and source lineage in `backend/src/main/java/com/ikms/worker/extract/`, `backend/src/main/java/com/ikms/ai/`, and `backend/src/main/java/com/ikms/document/`
- [x] T205 Implement configurable validation rules, processing findings, duplicate/reprocessed detection, reviewer correction traceability, and approval-time publishing hooks that refresh timeline, related knowledge, and retrieval projections without bypassing Human Review in `backend/src/main/java/com/ikms/review/`, `backend/src/main/java/com/ikms/search/`, `backend/src/main/java/com/ikms/client/`, and related packages
- [x] T206 Extend Review APIs, frontend review surfaces, tests, and documentation to expose processing jobs, confidence, validation findings, reviewer comments, retry controls, and reviewer-assistance context while preserving the existing Review workspace architecture in `backend/src/main/java/com/ikms/review/`, `frontend/src/features/intake/review/`, `backend/src/test/java/`, `frontend/src/features/`, and `docs/`

## Phase 23: Enterprise Knowledge Quality And Data Stewardship Platform

- [x] T207 Define the Phase 8 knowledge-quality architecture, quality lifecycle, scoring dimensions, stewardship flow, recommendation model, and product-boundary guidance in `docs/06-architecture/`, `docs/10-operations/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T208 Introduce backend knowledge-quality domain models, persistence, scoring services, issue-detection services, and API contracts for customer quality snapshots, issue queues, stewardship actions, and bulk operations in `backend/src/main/java/com/ikms/`, `backend/src/main/resources/db/migration/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T209 Implement explainable knowledge-quality evaluation for metadata completeness, Business Reference validation, customer linkage, duplicate knowledge, timeline quality, version quality, retrieval readiness, and AI quality in `backend/src/main/java/com/ikms/client/`, `backend/src/main/java/com/ikms/document/`, `backend/src/main/java/com/ikms/search/`, `backend/src/main/java/com/ikms/review/`, and related packages
- [x] T210 Implement stewardship workflows, revalidation, publish/reindex hooks, recommendation generation, authorization, audit coverage, and bulk correction/recheck/reindex actions without bypassing human validation in `backend/src/main/java/com/ikms/`, `backend/src/main/java/com/ikms/audit/`, `backend/src/main/java/com/ikms/security/`, and related packages
- [x] T211 Add a Knowledge Quality workspace, steward queue, customer quality panels, indicators, bulk-correction dialogs, and tests using existing enterprise UI patterns in `frontend/src/features/`, `frontend/src/api/`, `frontend/src/app/components/`, `docs/`, and `docs/13-worklog/worklog.md`

## Phase 24: Enterprise Security, Governance & Compliance Platform

- [x] T212 Define Phase 10 security and governance architecture, ABAC augmentation rules, lifecycle states, and compliance boundaries without introducing Policy or Claim entities in `docs/06-architecture/`, `docs/08-security/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T213 Add backend governance persistence, migrations, and domain models for information classification, lifecycle state, retention policy metadata, legal hold tracking, ABAC user/document attributes, AI governance settings, and export controls in `backend/src/main/resources/db/migration/`, `backend/src/main/java/com/ikms/security/`, `backend/src/main/java/com/ikms/document/`, `backend/src/main/java/com/ikms/retention/`, and related packages
- [x] T214 Implement additive RBAC-plus-ABAC enforcement, governance-aware search/AI/document access trimming, export governance, and audit enrichment while preserving existing authorization compatibility in `backend/src/main/java/com/ikms/security/`, `backend/src/main/java/com/ikms/search/`, `backend/src/main/java/com/ikms/ai/`, `backend/src/main/java/com/ikms/document/`, and `backend/src/main/java/com/ikms/audit/`
- [x] T215 Extend administration and governance APIs for classification policies, retention policies, legal holds, AI governance, compliance reporting, and security settings using existing administration patterns in `backend/src/main/java/com/ikms/config/`, `backend/src/main/java/com/ikms/governance/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T216 Integrate governance administration, viewer/export restrictions, and security policy visibility into the frontend Administration and document-viewer experiences without redesigning existing workspaces in `frontend/src/features/admin/`, `frontend/src/api/`, and `frontend/src/app/components/document-viewer/`
- [x] T217 Add backend and frontend validation for ABAC, classification enforcement, legal hold, retention eligibility, AI governance, compliance reporting, and export restrictions, then record the results in `docs/13-worklog/worklog.md` and this task tracker

## Phase 25: Enterprise Administration & Operations Platform (Product Phase 9)

- [x] T218 Define the enterprise operations architecture covering jobs, schedulers, queues, workers, retries, caches, diagnostics, recovery boundaries, and extension points in `docs/06-architecture/`, `docs/10-operations/`, and `specs/001-insurance-broker-ikms/contracts/`
- [x] T219 Add backend operations persistence, migrations, contracts, and services for reusable background jobs, queue state, scheduler state/history, operational metrics, cache administration, and audit-ready operator actions in `backend/src/main/resources/db/migration/`, `backend/src/main/java/com/ikms/operations/`, and related packages
- [x] T220 Implement operations APIs and authorization for jobs, queues, schedulers, reindex, embeddings, OCR, AI operations, health, diagnostics, cache control, and operational audit events using existing backend API patterns in `backend/src/main/java/com/ikms/operations/`, `backend/src/main/java/com/ikms/security/`, and `backend/src/main/java/com/ikms/audit/`
- [x] T221 Integrate the Administration workspace with Operations, Queues, Background Jobs, Scheduler, Embeddings, OCR, AI Operations, Cache, Health, and Diagnostics modules using existing EntityGrid, WorkspaceToolbar, RightContextPanel, StatusBadge, and responsive patterns in `frontend/src/features/admin/`, `frontend/src/api/`, and shared app components
- [x] T222 Add backend and frontend validation for operational jobs, queue management, scheduler actions, health/diagnostics, cache administration, API authorization, and administration UI flows, then record results in `docs/13-worklog/worklog.md` and this task tracker

## Phase 26: Enterprise Platform Readiness & Operational Excellence (Product Phase 11)

- [x] T223 Add a reusable observability and request-context foundation for request IDs, correlation IDs, background job IDs, AI interaction IDs, processing job IDs, review IDs, retrieval IDs, timeline request IDs, and search request IDs across backend APIs and major workflows in `backend/src/main/java/com/ikms/`, `frontend/src/api/`, and related tests
- [x] T224 Define a platform-neutral alerting framework with standardized alert identifiers, severities, categories, thresholds, suppression metadata, escalation levels, and operator guidance without integrating external notification systems in `backend/src/main/java/com/ikms/operations/`, `docs/10-operations/`, and related documentation
- [x] T225 Perform repository architecture, platform consistency, technical debt, and repository quality reviews; apply only low-risk cleanup and consistency fixes; and document larger refactoring opportunities in `docs/06-architecture/`, `docs/06-ui/`, `docs/09-testing/`, `docs/10-operations/`, `docs/14-release/`, and related code/docs
- [x] T226 Produce final enterprise operational runbooks, go-live readiness guidance, architecture validation, documentation cross-reference cleanup, worklog updates, and final release-candidate readiness assessment while preserving the boundary that Policy and Claim remain Business Reference Fields only in `docs/10-operations/`, `docs/14-release/`, `docs/13-worklog/worklog.md`, and this task tracker
- [x] T227 Run repository-supported backend/frontend validation for the Phase 11 readiness changes, record exact commands and results, and complete final task tracking for the v1.0 release-candidate recommendation in `docs/13-worklog/worklog.md` and this task tracker
