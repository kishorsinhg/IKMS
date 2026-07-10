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

- `T001-T022` are completed and reflected in `specs/001-insurance-broker-ikms/tasks.md`.
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

Start the next session by reviewing `git status`, confirming the foundation slice is committed, then continue with pre-implementation hardening.

Recommended first implementation slice:

- Next branch target: `T023-T033` for pre-implementation hardening, beginning with CSV import and authentication-governance coverage
- T023-T033: pre-implementation hardening and Hardening review
- T034-T047: US1 MVP client profile and Client Profile review
