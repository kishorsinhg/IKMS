# Client Profile Review

## Review Metadata

- Slice: User Story 1 client profile MVP
- Reviewer: Codex
- Date: 2026-07-10
- Related tasks: `T034-T047`
- Related user story: US1 - Build A Client Knowledge Profile
- Related requirements: `FR-001`, `FR-002`, `FR-003`, `FR-004`, `FR-005`, `FR-006`, `FR-007`
- Related tests: `mvn test`, `npm test`, `npm run build`
- Outcome: Approved with follow-up tasks

## Scope Reviewed

- Files changed: client/note entities, client ID service, client/note services, client controller, migration, frontend client workspace/profile/notes flow, US1 contract tests
- APIs changed: `GET/POST/PATCH /api/clients`, `GET/POST /api/clients/{clientId}/notes`, `POST /api/clients/import`
- Data model or migration changes: `client` and `note` tables added to baseline migration
- UI changes: client workspace, client profile shell, notes section, client creation flow, import entry point
- Configuration changes: None

## Constitution Gates

- Client-centric V1 scope respected: Yes
- Monolithic single-tenant simplicity preserved: Yes
- Evidence-before-answer preserved: Yes; AI remains client-scoped and placeholder-only in this slice
- Security/PII before retrieval enforced: Existing trim contract retained; no retrieval bypass introduced
- Original files preserved: Not changed
- Audit coverage present: Client creation, client ID update, note creation, login, and import events audited
- Tests trace to requirements: Yes, at contract/UI/service baseline level

## Functional Review

- Acceptance scenarios covered: temporary ClientID generation, unique actual ClientID handling, client creation/search/open flow, note creation/listing, section rendering
- Edge cases covered: missing display name, blank note text, missing requested ClientID, duplicate actual ClientID rejection path in service layer
- Error handling reviewed: Yes
- Role behavior reviewed: Existing authenticated shell and permission-gated navigation retained

## Security, PII, Audit, And AI Review

- Authentication and authorization: Reused existing auth/session governance
- PII masking/redaction: No new PII bypass introduced
- Original file access: Not in scope
- Security trimming before retrieval: Contract remains in place for future document/search/AI slices
- Audit events: Client create, client ID change, note create
- AI guardrails and citations: Not in scope beyond UI section placeholder
- Prompt injection handling: Not in scope
- Retention/deletion/anonymization: Existing policy contract retained, not newly exercised

## Test Evidence

- Unit tests: client/note contract tests, existing backend service/policy tests
- Integration tests: existing pgvector smoke test remains available and skipped when Docker is unavailable
- Contract tests: `ClientControllerContractTest`, `NoteControllerContractTest`
- UI tests: `ClientProfile.test.tsx`, `App.test.tsx`
- Quickstart or manual validation: frontend build passes

## Findings

| ID | Severity | Area | Finding | Required Action |
| --- | --- | --- | --- | --- |
| US1-001 | Medium | Client Import | CSV import still validates/report rows but does not yet persist Client records into the new client domain. | Connect import rows to `ClientService` persistence in a follow-up slice. |
| US1-002 | Low | Notes | Notes currently support create/list only; edit/delete endpoints remain unimplemented even though the broader requirements mention note lifecycle audit history. | Extend note lifecycle operations when the next client-profile refinement is scheduled. |
| US1-003 | Low | Profile Coverage | Documents, emails, AI Q&A, and activity sections are intentionally placeholder panels because those user stories land in later slices. | Replace placeholders when US2, US3, and US6 are implemented. |

## Decision

Review decision: Approved with follow-up tasks

Follow-up tasks:

- Persist CSV imports into the real client model.
- Add note update/delete workflows and tests.
- Replace profile placeholders with real document, email, AI, and activity data in later slices.

Reviewer notes:

- The client profile is now a usable master anchor for later intake, search, and AI features.
