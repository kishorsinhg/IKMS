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

Review `specs/001-insurance-broker-ikms/tasks.md`, then start implementation from Phase 1 setup.

Recommended first implementation slice:

- T001-T008: project setup
- T009-T022: foundational backend/frontend/security/storage/audit scaffolding and Foundation review
- T023-T033: pre-implementation hardening and Hardening review
- T034-T047: US1 MVP client profile and Client Profile review
