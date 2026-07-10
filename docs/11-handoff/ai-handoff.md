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
- Task count: 121

## Next Recommended Task

Local scaffold work for Phase 1 setup exists and should now be treated as the current baseline.

Current implementation checkpoint:

- `T001-T064` are completed and reflected in `specs/001-insurance-broker-ikms/tasks.md`.
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
- Intake and review queue UI baseline pages now exist in `frontend/src/features/intake/`.
- Duplicate detection, manual upload/original preservation, and document versioning rules now exist in `backend/src/main/java/com/ikms/document/`.
- Extraction/classification adapters, shared folder + IMAP workers, email attachment linking, and review queue endpoints now exist in `backend/src/main/java/com/ikms/ai/`, `backend/src/main/java/com/ikms/worker/`, `backend/src/main/java/com/ikms/email/`, and `backend/src/main/java/com/ikms/review/`.
- The current intake/review checkpoint still does not include frontend intake API bindings, live upload/review UI behavior, client knowledge display, or the US2 review artifact.

Start the next session by reviewing `git status`, confirming the US1 client profile slice is committed, then continue with intake and review.

Recommended first implementation slice:

- Next branch target: continue `T065-T069` for User Story 2 intake and review.
- Start with `T065-T068` for frontend intake/review API bindings, live upload and review UI behavior, and client knowledge display.
- Then complete `T069` with the intake/review code review artifact.
