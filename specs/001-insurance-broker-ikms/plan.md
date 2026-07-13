# Implementation Plan: Insurance Broker IKMS V1 Requirements Baseline

**Branch**: `001-insurance-broker-ikms` | **Date**: 2026-07-08 | **Spec**: `specs/001-insurance-broker-ikms/spec.md`

**Input**: Feature specification from `specs/001-insurance-broker-ikms/spec.md`

## Summary

Build a simple single-tenant Insurance Broker Intelligent Knowledge Management System centered on one fixed master profile: Client. The system ingests PDF, DOCX, email bodies, and supported email attachments from manual upload, shared folders, and IMAP; preserves originals; links knowledge to clients through AI-assisted processing plus human review; provides client-level search and RAG Q&A; and enforces role-based PII protection, redaction, guardrails, and auditability.

The V1 architecture is a monolithic Spring Boot backend with an embedded worker profile, a React frontend, PostgreSQL with pgvector, and file/object storage outside the database. AI/OCR/embedding providers are configurable, with Mistral Cloud used for initial testing.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.x, TypeScript, React 19 or current stable React line, SQL

**Primary Dependencies**: Spring Web, Spring Security, Spring Data JPA, Flyway or Liquibase, PostgreSQL JDBC, pgvector integration, IMAP mail library, Apache Tika or equivalent extraction support, React, Vite, TanStack Query, React Router, Mistral-compatible AI/OCR client abstraction

**Storage**: PostgreSQL with pgvector for structured data, metadata, audit records, and embeddings; filesystem or S3-compatible object storage for original and redacted files

**Testing**: JUnit 5, Spring Boot Test, Testcontainers for PostgreSQL/pgvector, frontend unit/component tests with Vitest and Testing Library, Playwright for key browser workflows, contract tests for API behavior

**Target Platform**: Single-tenant deployment on broker-controlled server or private cloud, Linux preferred for production; Windows developer workspace supported

**Project Type**: Web application with backend API, frontend UI, and background worker runtime in one monolithic codebase

**Performance Goals**: Open known client profiles in under 10 seconds for 95% of normal searches; route 95% of supported intake items to linked or review state within configured processing SLA; return client-scoped search results interactively for expected 20-25 user broker deployments

**Constraints**: Security trimming must occur before retrieval and LLM context assembly; AI answers must cite evidence or say no supporting evidence exists; document-backed evidence must retain page/location provenance for UI citations; provider-backed chat and OCR integrations must fail safely into review/no-evidence states without leaking unauthorized data; Processor users must never receive unredacted PII; original files must be preserved unchanged; no global cross-client AI Q&A in V1

**Scale/Scope**: One broker tenant, 20-25 users, client-centric knowledge store, PDF/DOCX/email intake, English UI, English/German content processing, configurable document types and fixed-plus-extension metadata fields

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The Spec Kit constitution is ratified at `.specify/memory/constitution.md` and is the authority for implementation gates. Supporting controls are also captured in `docs/00-governance/project-charter.md`, `docs/08-security/security-baseline.md`, and the imported SRS controls.

Pre-design gates:

- **Client-centric V1**: Pass. The plan uses fixed Client as the only master profile.
- **Monolithic simplicity**: Pass. Backend API and worker run from one Spring Boot codebase; no microservices.
- **Evidence before answer**: Pass. RAG is client-scoped, source-cited, and refuses no-evidence answers.
- **Security trimming before retrieval**: Pass. Access filters apply before search results, embedding retrieval, preview, download, and AI context assembly.
- **PII protection**: Pass. Processor receives masked metadata and redacted files only; Supervisor can access originals when permitted.
- **Original preservation**: Pass. Originals are immutable file versions; replacements are modeled as versions, not destructive overwrite.
- **Cost-aware artifacts**: Pass. Feature artifacts are short, focused, and split by purpose.

Post-design gates:

- **Client-centric V1**: Pass. Data model and contracts do not introduce generic master entities.
- **Monolithic simplicity**: Pass. Contracts assume one backend API and one worker profile.
- **Evidence before answer**: Pass. API contracts include citations, no-evidence response, conflict signaling, and decision refusal.
- **Security trimming before retrieval**: Pass. Contracts explicitly require authorized/redacted result variants.
- **PII protection**: Pass. Data model includes PII flags, redaction status, and audit events.
- **Original preservation**: Pass. Document Version owns immutable file hashes and storage pointers.
- **Cost-aware artifacts**: Pass. Artifacts reference each other instead of duplicating full content.

## Project Structure

### Documentation (this feature)

```text
specs/001-insurance-broker-ikms/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- api.md
|   `-- ui.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md
```

### Source Code (repository root)

```text
backend/
|-- src/main/java/
|   |-- .../config/
|   |-- .../security/
|   |-- .../client/
|   |-- .../document/
|   |-- .../email/
|   |-- .../note/
|   |-- .../review/
|   |-- .../search/
|   |-- .../ai/
|   |-- .../audit/
|   `-- .../worker/
|-- src/main/resources/
|   |-- db/migration/
|   `-- application.yml
`-- src/test/

frontend/
|-- src/
|   |-- app/
|   |-- api/
|   |-- components/
|   |-- features/
|   |   |-- clients/
|   |   |-- intake/
|   |   |-- search/
|   |   |-- admin/
|   |   `-- audit/
|   `-- test/
`-- tests/

infra/
|-- docker-compose.yml
`-- postgres/

docs/
|-- 00-governance/
|-- 08-security/
|-- 11-handoff/
`-- 13-worklog/
```

**Structure Decision**: Use a two-app monorepo (`backend`, `frontend`) with shared feature documentation under `specs`. The backend remains one deployable Spring Boot application with API and worker profiles; the frontend remains one React application. Infrastructure files stay separate so V1 deployment choices do not leak into business code.

## Complexity Tracking

No constitution violations are introduced. The only notable complexity is pgvector plus background processing inside the monolith; both are justified by core V1 requirements for semantic search, RAG, and automated intake without creating separate services.
