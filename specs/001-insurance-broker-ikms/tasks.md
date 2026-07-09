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
- [ ] T013 Implement local user, role, and permission model in `backend/src/main/java/com/ikms/security/domain/`
- [ ] T014 Implement Spring Security username/password authentication in `backend/src/main/java/com/ikms/security/`
- [ ] T015 Implement role permission checks for Indexer, Processor, Supervisor, and Administrator in `backend/src/main/java/com/ikms/security/PermissionService.java`
- [ ] T016 Implement file storage abstraction for originals and redacted files in `backend/src/main/java/com/ikms/storage/FileStorageService.java`
- [ ] T017 Implement database-backed configuration registry in `backend/src/main/java/com/ikms/config/AppSettingService.java`
- [ ] T018 Implement frontend API client foundation in `frontend/src/api/client.ts`
- [ ] T019 Implement frontend route shell and protected route handling in `frontend/src/app/`
- [ ] T020 [P] Add backend Testcontainers setup for PostgreSQL/pgvector in `backend/src/test/java/com/ikms/support/PostgresIntegrationTest.java`
- [ ] T021 [P] Add frontend test setup in `frontend/src/test/setup.ts`
- [ ] T022 Complete Foundation code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/foundation-review.md`

**Checkpoint**: Authentication, authorization hooks, storage abstraction, migration framework, audit writer, and app shell are ready.

---

## Phase 2A: Pre-Implementation Hardening

**Purpose**: Close pre-implementation gaps found during constitution and artifact review.

**Critical**: Complete these tasks before broad document access, search, RAG, or administrative workflows are considered implementation-ready.

- [ ] T023 Add explicit CSV client import tests in `backend/src/test/java/com/ikms/client/ClientImportTest.java`
- [ ] T024 [US1] Implement CSV client import validation, duplicate warnings, and audit events in `backend/src/main/java/com/ikms/client/ClientImportService.java`
- [ ] T025 [US1] Add CSV import API and UI entry points in `backend/src/main/java/com/ikms/client/ClientImportController.java` and `frontend/src/features/clients/import/`
- [ ] T026 Add account status, failed-login tracking, lockout, session timeout, and login audit tests in `backend/src/test/java/com/ikms/security/AuthenticationGovernanceTest.java`
- [ ] T027 Extend authentication implementation for account status, failed-login tracking, lockout, session timeout, and login audit in `backend/src/main/java/com/ikms/security/`
- [ ] T028 Add foundation-level security trimming tests for search, preview, download, and AI context assembly in `backend/src/test/java/com/ikms/security/SecurityTrimBoundaryTest.java`
- [ ] T029 Implement shared security-trim contract before document access, search retrieval, and AI context assembly in `backend/src/main/java/com/ikms/security/SecurityTrimService.java`
- [ ] T030 Add retention, legal hold, deletion, and anonymization tests in `backend/src/test/java/com/ikms/retention/RetentionWorkflowTest.java`
- [ ] T031 Implement retention policy, legal hold, controlled deletion, and anonymization workflow services in `backend/src/main/java/com/ikms/retention/`
- [ ] T032 Add lightweight performance/SLA validation for client profile open and intake routing in `backend/src/test/java/com/ikms/performance/V1SlaValidationTest.java`
- [ ] T033 Complete Pre-Implementation Hardening code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/hardening-review.md`

**Checkpoint**: FR-005, FR-030, FR-032, FR-039, SC-001, and SC-002 have explicit implementation and validation coverage.

---

## Phase 3: User Story 1 - Build A Client Knowledge Profile (Priority: P1) MVP

**Goal**: Authorized users can create/find clients, handle temporary ClientID replacement, add notes, and view the client profile sections.

**Independent Test**: Create a client with temporary ClientID, replace it with actual ClientID, add a note, link sample client knowledge, and confirm the profile sections render.

### Tests For User Story 1

- [ ] T034 [P] [US1] Add client API contract tests in `backend/src/test/java/com/ikms/client/ClientControllerContractTest.java`
- [ ] T035 [P] [US1] Add client profile UI tests in `frontend/src/features/clients/ClientProfile.test.tsx`
- [ ] T036 [P] [US1] Add note API tests in `backend/src/test/java/com/ikms/note/NoteControllerContractTest.java`

### Implementation For User Story 1

- [ ] T037 [P] [US1] Create Client JPA entity and repository in `backend/src/main/java/com/ikms/client/`
- [ ] T038 [P] [US1] Create Note JPA entity and repository in `backend/src/main/java/com/ikms/note/`
- [ ] T039 [US1] Extend baseline migration for clients and notes in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [ ] T040 [US1] Implement temporary ClientID generation and uniqueness validation in `backend/src/main/java/com/ikms/client/ClientIdService.java`
- [ ] T041 [US1] Implement client profile service in `backend/src/main/java/com/ikms/client/ClientService.java`
- [ ] T042 [US1] Implement note service with audit events in `backend/src/main/java/com/ikms/note/NoteService.java`
- [ ] T043 [US1] Implement client and note REST endpoints in `backend/src/main/java/com/ikms/client/ClientController.java`
- [ ] T044 [US1] Implement client search and profile API bindings in `frontend/src/api/clients.ts`
- [ ] T045 [US1] Implement client list, create/edit, and profile pages in `frontend/src/features/clients/`
- [ ] T046 [US1] Implement notes section in `frontend/src/features/clients/notes/`
- [ ] T047 [US1] Complete Client Profile code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us1-client-profile-review.md`

**Checkpoint**: Client profile MVP is usable without document intake, AI, or advanced administration.

---

## Phase 4: User Story 2 - Ingest And Review Client Knowledge (Priority: P1)

**Goal**: Indexers can intake PDF/DOCX/email knowledge, preserve originals, detect duplicates, and resolve unlinked review items.

**Independent Test**: Upload a PDF without a client, see it in review, manually link it to a client, correct metadata, approve it, and see it on the client profile.

### Tests For User Story 2

- [ ] T048 [P] [US2] Add document upload and duplicate tests in `backend/src/test/java/com/ikms/document/DocumentUploadTest.java`
- [ ] T049 [P] [US2] Add review queue workflow tests in `backend/src/test/java/com/ikms/review/ReviewQueueWorkflowTest.java`
- [ ] T050 [P] [US2] Add intake UI tests in `frontend/src/features/intake/IntakePage.test.tsx`
- [ ] T051 [P] [US2] Add review queue UI tests in `frontend/src/features/intake/ReviewQueue.test.tsx`

### Implementation For User Story 2

- [ ] T052 [P] [US2] Create Document and DocumentVersion entities in `backend/src/main/java/com/ikms/document/`
- [ ] T053 [P] [US2] Create Email entity in `backend/src/main/java/com/ikms/email/`
- [ ] T054 [P] [US2] Create ReviewQueueItem entity in `backend/src/main/java/com/ikms/review/`
- [ ] T055 [US2] Extend migration for documents, versions, emails, and review queue in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [ ] T056 [US2] Implement hash-based duplicate detection in `backend/src/main/java/com/ikms/document/DuplicateDetectionService.java`
- [ ] T057 [US2] Implement manual upload and original preservation in `backend/src/main/java/com/ikms/document/DocumentUploadService.java`
- [ ] T058 [US2] Implement document versioning rules in `backend/src/main/java/com/ikms/document/DocumentVersionService.java`
- [ ] T059 [US2] Implement text extraction adapter for PDF/DOCX in `backend/src/main/java/com/ikms/worker/extract/TextExtractionService.java`
- [ ] T060 [US2] Implement AI classification and metadata extraction adapter in `backend/src/main/java/com/ikms/ai/ClassificationService.java`
- [ ] T061 [US2] Implement shared folder polling worker in `backend/src/main/java/com/ikms/worker/intake/SharedFolderIntakeWorker.java`
- [ ] T062 [US2] Implement IMAP mailbox intake worker in `backend/src/main/java/com/ikms/worker/intake/ImapIntakeWorker.java`
- [ ] T063 [US2] Implement email attachment-to-document linking in `backend/src/main/java/com/ikms/email/EmailAttachmentService.java`
- [ ] T064 [US2] Implement review queue services and endpoints in `backend/src/main/java/com/ikms/review/`
- [ ] T065 [US2] Implement document, email, and review queue API bindings in `frontend/src/api/intake.ts`
- [ ] T066 [US2] Implement manual upload, intake status, and duplicate result UI in `frontend/src/features/intake/`
- [ ] T067 [US2] Implement review queue link/correct/approve UI in `frontend/src/features/intake/review/`
- [ ] T068 [US2] Add document and email sections to client profile in `frontend/src/features/clients/knowledge/`
- [ ] T069 [US2] Complete Intake and Review code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us2-intake-review.md`

**Checkpoint**: Intake and review create searchable client-linked knowledge records, even before RAG is enabled.

---

## Phase 5: User Story 4 - Protect PII With Role-Based Access (Priority: P1)

**Goal**: Processor users receive masked/redacted access only, while authorized Supervisors can access originals and PII.

**Independent Test**: Mark metadata as PII, compare Processor and Supervisor preview/download/search/AI behavior, and confirm PII access is audited.

### Tests For User Story 4

- [ ] T070 [P] [US4] Add PII masking and permission tests in `backend/src/test/java/com/ikms/security/PiiAccessControlTest.java`
- [ ] T071 [P] [US4] Add redacted preview/download tests in `backend/src/test/java/com/ikms/document/RedactedDocumentAccessTest.java`
- [ ] T072 [P] [US4] Add Processor/Supervisor UI permission tests in `frontend/src/features/clients/PiiVisibility.test.tsx`

### Implementation For User Story 4

- [ ] T073 [US4] Implement metadata PII masking service in `backend/src/main/java/com/ikms/security/PiiMaskingService.java`
- [ ] T074 [US4] Implement document redaction adapter and failure states in `backend/src/main/java/com/ikms/document/DocumentRedactionService.java`
- [ ] T075 [US4] Enforce redacted preview/download routing in `backend/src/main/java/com/ikms/document/DocumentAccessController.java`
- [ ] T076 [US4] Enforce PII filtering before search retrieval and AI context assembly in `backend/src/main/java/com/ikms/security/SecurityTrimService.java`
- [ ] T077 [US4] Add PII/original access audit events in `backend/src/main/java/com/ikms/audit/AuditService.java`
- [ ] T078 [US4] Add frontend masked-field and redacted-action states in `frontend/src/features/clients/knowledge/`
- [ ] T079 [US4] Complete PII Protection code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us4-pii-protection-review.md`

**Checkpoint**: Processor cannot receive unredacted PII through profile, preview, download, search, or AI paths.

---

## Phase 6: User Story 3 - Search And Ask Questions About One Client (Priority: P1)

**Goal**: Processor and Supervisor users can search and ask evidence-based questions within one client profile.

**Independent Test**: Link documents/emails to one client, ask a client-level question, verify citations, no-evidence behavior, conflict handling, and decision refusal.

### Tests For User Story 3

- [ ] T080 [P] [US3] Add client search API tests in `backend/src/test/java/com/ikms/search/ClientSearchTest.java`
- [ ] T081 [P] [US3] Add RAG answer guardrail tests in `backend/src/test/java/com/ikms/ai/ClientQuestionAnsweringTest.java`
- [ ] T082 [P] [US3] Add search and Q&A UI tests in `frontend/src/features/search/ClientSearchAsk.test.tsx`

### Implementation For User Story 3

- [ ] T083 [P] [US3] Create EmbeddingChunk and AIInteraction entities in `backend/src/main/java/com/ikms/ai/`
- [ ] T084 [US3] Extend migration for embedding chunks and AI interactions in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [ ] T085 [US3] Implement chunking and embedding pipeline in `backend/src/main/java/com/ikms/ai/EmbeddingIndexService.java`
- [ ] T086 [US3] Implement keyword, metadata, and vector retrieval in `backend/src/main/java/com/ikms/search/ClientSearchService.java`
- [ ] T087 [US3] Implement security-trimmed RAG context assembly in `backend/src/main/java/com/ikms/ai/RagContextService.java`
- [ ] T088 [US3] Implement no-evidence, citation, conflict, and decision-refusal logic in `backend/src/main/java/com/ikms/ai/ClientQuestionAnsweringService.java`
- [ ] T089 [US3] Implement search and ask endpoints in `backend/src/main/java/com/ikms/search/ClientSearchController.java`
- [ ] T090 [US3] Implement AI feedback endpoint in `backend/src/main/java/com/ikms/ai/AiFeedbackController.java`
- [ ] T091 [US3] Implement search and ask API bindings in `frontend/src/api/search.ts`
- [ ] T092 [US3] Implement client-scoped search and AI Q&A panel in `frontend/src/features/search/`
- [ ] T093 [US3] Complete Search and AI Q&A code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us3-search-ai-review.md`

**Checkpoint**: Client-level RAG works with citations and guardrails against no-evidence and prohibited decisions.

---

## Phase 7: User Story 5 - Configure Broker Knowledge Rules (Priority: P2)

**Goal**: Administrators can configure document types, metadata labels, PII flags, intake sources, review behavior, users, roles, and AI provider settings.

**Independent Test**: Configure document type, mark metadata PII, configure mailbox and shared folder, set review mode, and confirm new intake follows settings.

### Tests For User Story 5

- [ ] T094 [P] [US5] Add admin configuration API tests in `backend/src/test/java/com/ikms/admin/AdminConfigurationTest.java`
- [ ] T095 [P] [US5] Add admin UI tests in `frontend/src/features/admin/AdminConfiguration.test.tsx`

### Implementation For User Story 5

- [ ] T096 [P] [US5] Create DocumentType and MetadataField entities in `backend/src/main/java/com/ikms/config/domain/`
- [ ] T097 [P] [US5] Create intake and AI setting entities in `backend/src/main/java/com/ikms/config/domain/`
- [ ] T098 [US5] Extend migration for configuration tables in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [ ] T099 [US5] Implement document type and metadata configuration services in `backend/src/main/java/com/ikms/config/KnowledgeConfigurationService.java`
- [ ] T100 [US5] Implement shared folder, IMAP, review mode, and AI provider settings services in `backend/src/main/java/com/ikms/config/IntakeAiConfigurationService.java`
- [ ] T101 [US5] Implement admin configuration endpoints in `backend/src/main/java/com/ikms/config/AdminConfigurationController.java`
- [ ] T102 [US5] Implement admin API bindings in `frontend/src/api/admin.ts`
- [ ] T103 [US5] Implement admin configuration screens in `frontend/src/features/admin/`
- [ ] T104 [US5] Complete Administration code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us5-administration-review.md`

**Checkpoint**: Administrator can configure V1 broker rules without code changes.

---

## Phase 8: User Story 6 - Audit And Govern System Activity (Priority: P2)

**Goal**: Authorized users can search and export audit logs for authentication, intake, review, document access, PII access, configuration, and AI activity.

**Independent Test**: Perform representative actions, filter audit logs by user/date/action/client, and export CSV.

### Tests For User Story 6

- [ ] T105 [P] [US6] Add audit search and export tests in `backend/src/test/java/com/ikms/audit/AuditExportTest.java`
- [ ] T106 [P] [US6] Add audit UI tests in `frontend/src/features/audit/AuditPage.test.tsx`

### Implementation For User Story 6

- [ ] T107 [US6] Finalize AuditLog entity and indexes in `backend/src/main/java/com/ikms/audit/AuditLog.java`
- [ ] T108 [US6] Extend migration for audit retention and search indexes in `backend/src/main/resources/db/migration/V001__baseline_schema.sql`
- [ ] T109 [US6] Implement audit search service in `backend/src/main/java/com/ikms/audit/AuditSearchService.java`
- [ ] T110 [US6] Implement audit CSV export in `backend/src/main/java/com/ikms/audit/AuditExportService.java`
- [ ] T111 [US6] Implement audit endpoints in `backend/src/main/java/com/ikms/audit/AuditController.java`
- [ ] T112 [US6] Implement audit API bindings in `frontend/src/api/audit.ts`
- [ ] T113 [US6] Implement audit search and export UI in `frontend/src/features/audit/`
- [ ] T114 [US6] Complete Audit and Governance code review using `docs/templates/code-review-template.md` and save results in `docs/09-testing/reviews/us6-audit-governance-review.md`

**Checkpoint**: Audit activity is searchable and exportable for governance review.

---

## Phase 9: Polish And Cross-Cutting Validation

**Purpose**: Confirm the whole V1 behaves as specified and is ready for implementation handoff or demo.

- [ ] T115 [P] Add Playwright quickstart scenario coverage in `frontend/tests/quickstart.spec.ts`
- [ ] T116 [P] Add backend seed data for local validation in `backend/src/main/resources/db/dev/V900__dev_seed.sql`
- [ ] T117 Run and document quickstart validation results in `docs/13-worklog/worklog.md`
- [ ] T118 Update AI handoff with built status and remaining gaps in `docs/11-handoff/ai-handoff.md`
- [ ] T119 Update changelog for implemented feature slices in `docs/14-release/changelog.md`
- [ ] T120 Review security, audit, PII, and guardrail coverage against `docs/08-security/security-baseline.md`
- [ ] T121 Complete release readiness review using `docs/14-release/release-readiness-review-template.md` and save results in `docs/14-release/release-readiness-review.md`

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
