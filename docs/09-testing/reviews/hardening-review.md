# Pre-Implementation Hardening Review

## Review Metadata

- Slice: Pre-implementation hardening
- Reviewer: Codex
- Date: 2026-07-10
- Related tasks: `T023-T033`
- Related user story: Foundation hardening before client, intake, retrieval, and governance features
- Related requirements: `FR-005`, `FR-030`, `FR-032`, `FR-039`, `SC-001`, `SC-002`
- Related tests: `mvn test`, `npm test`, `npm run build`
- Outcome: Approved with follow-up tasks

## Scope Reviewed

- Files changed: CSV import validation/API/UI, authentication governance, security trim contract, retention workflow contract, SLA policy checks, task/handoff/worklog updates
- APIs changed: `POST /api/clients/import`
- Data model or migration changes: None
- UI changes: Client CSV import page and upload workflow
- Configuration changes: failed-login threshold and session-timeout defaults are now consumed operationally

## Constitution Gates

- Client-centric V1 scope respected: Yes
- Monolithic single-tenant simplicity preserved: Yes
- Evidence-before-answer preserved: Yes, via explicit pre-retrieval and pre-context security trim contract
- Security/PII before retrieval enforced: Yes, at policy-contract level
- Original files preserved: Not changed in this slice
- Audit coverage present: CSV import and login governance now emit audit events through the shared audit service
- Tests trace to requirements: Yes, for the hardening slice

## Functional Review

- Acceptance scenarios covered: CSV validation, duplicate warnings, login lockout/session timeout, PII access gating, retention workflow gating, SLA threshold checks
- Edge cases covered: missing CSV headers, duplicate in-file client IDs, invalid email/type/status values, login lockout threshold, legal hold denial
- Error handling reviewed: Yes
- Role behavior reviewed: Yes, for Processor and Supervisor boundary cases

## Security, PII, Audit, And AI Review

- Authentication and authorization: Failed-login counting, lockout, session timeout, and login audit added
- PII masking/redaction: Pre-document contract enforces redacted-only Processor behavior
- Original file access: Supervisor original access contract added
- Security trimming before retrieval: Added and covered by tests
- Audit events: CSV import, login success/failure, retention actions
- AI guardrails and citations: Retrieval precondition only in this slice; answer generation remains future work
- Prompt injection handling: Not implemented yet
- Retention/deletion/anonymization: Workflow contract added; persistence-backed execution remains future work

## Test Evidence

- Unit tests: backend security, retention, performance, client import tests
- Integration tests: existing pgvector smoke test remains available
- Contract tests: API path is covered indirectly through service/controller buildability; full endpoint tests remain future work
- UI tests: existing app-shell test still passes
- Quickstart or manual validation: frontend build passes

## Findings

| ID | Severity | Area | Finding | Required Action |
| --- | --- | --- | --- | --- |
| HR-001 | Medium | CSV Import | Import currently validates and reports rows but does not yet persist Client records because the Client domain is not implemented until the US1 slice. | Bind the import flow to real Client persistence during `T037-T045`. |
| HR-002 | Medium | Retention | Retention workflow is policy-level only and does not yet execute deletion or anonymization on stored entities/files. | Connect retention workflow to persisted records and storage once document/client entities exist. |
| HR-003 | Low | Security Trim | Trim decisions are contract-level and not yet wired into document/search/AI endpoints because those endpoints are not built yet. | Apply `SecurityTrimService` directly in the document, search, and AI slices. |

## Decision

Review decision: Approved with follow-up tasks

Follow-up tasks:

- Wire CSV import results to persisted Client entities in US1.
- Apply `SecurityTrimService` in preview, download, search, and AI flows.
- Connect retention decisions to real entity and file lifecycle handling.

Reviewer notes:

- This slice closes the architecture gap that would otherwise let later features invent inconsistent security and retention behavior.
