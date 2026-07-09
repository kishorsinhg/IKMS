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

- `T001-T008` are completed locally and should be kept in sync with `specs/001-insurance-broker-ikms/tasks.md`.
- Backend scaffold exists in `backend/` with Spring Boot app entrypoint, dependency management, test skeleton, and application config.
- Frontend scaffold exists in `frontend/` with React/Vite app entrypoint, route placeholder, test setup, and package scripts.
- Local PostgreSQL/pgvector runtime exists in `infra/docker-compose.yml`.
- Developer onboarding notes exist in `docs/10-operations/local-development.md`.
- The repository had been at the initial spec/artifacts commit before the scaffold checkpoint was committed.

Start the next session by reviewing `specs/001-insurance-broker-ikms/tasks.md` and the current git history, then continue with the foundational slice.

Recommended first implementation slice:

- T009-T022: foundational backend/frontend/security/storage/audit scaffolding and Foundation review
- T023-T033: pre-implementation hardening and Hardening review
- T034-T047: US1 MVP client profile and Client Profile review
