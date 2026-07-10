# PII Protection Code Review

## Review Metadata

- Slice: User Story 4 PII protection
- Reviewer: Codex
- Date: 2026-07-10
- Related tasks: T070-T079
- Related user story: US4 - Protect PII With Role-Based Access
- Related requirements: FR-024, FR-025, FR-026, FR-030
- Related tests: `backend/src/test/java/com/ikms/security/PiiAccessControlTest.java`, `backend/src/test/java/com/ikms/document/RedactedDocumentAccessTest.java`, `frontend/src/features/clients/PiiVisibility.test.tsx`
- Outcome: Approved with follow-up tasks

## Scope Reviewed

- Files changed: backend PII masking, security trim enforcement, document redaction/access endpoints, frontend client knowledge permission states
- APIs changed: `GET /api/clients/{clientId}`, `GET /api/clients/{clientId}/emails`, `GET /api/clients/{clientId}/documents`, `GET /api/documents/{documentId}/preview`, `GET /api/documents/{documentId}/download`
- Data model or migration changes: none
- UI changes: processor masked profile/email data and redacted document actions; supervisor original document actions
- Configuration changes: none

## Constitution Gates

- Client-centric V1 scope respected: Yes
- Monolithic single-tenant simplicity preserved: Yes
- Evidence-before-answer preserved: Yes
- Security/PII before retrieval enforced: Yes for current client profile, document access, and trim helpers
- Original files preserved: Yes
- Audit coverage present: Yes
- Tests trace to requirements: Yes

## Functional Review

- Acceptance scenarios covered: processor masked access, supervisor original access, redacted preview/download routing, trimmed search/AI helper behavior
- Edge cases covered: missing redacted variant denies processor original access
- Error handling reviewed: forbidden access returns API error shape through shared handler
- Role behavior reviewed: processor vs supervisor separation is explicit in both backend and frontend

## Security, PII, Audit, And AI Review

- Authentication and authorization: existing permissions reused; no parallel auth path added
- PII masking/redaction: client profile and email summaries masked when `VIEW_PII` is absent
- Original file access: enforced through `DocumentAccessController`
- Security trimming before retrieval: trim helpers now support masked search and AI context strings
- Audit events: preview/download success and denied access events added
- AI guardrails and citations: no change beyond trim support
- Prompt injection handling: no change
- Retention/deletion/anonymization: no change

## Test Evidence

- Unit tests: masking and trim behavior
- Integration tests: Spring context and Docker-backed smoke path still pass
- Contract tests: redacted/original document access responses
- UI tests: processor/supervisor client knowledge visibility
- Quickstart or manual validation: `mvn test`, `npm test -- --run`, `npm run build`

## Findings

| ID | Severity | Area | Finding | Required Action |
| --- | --- | --- | --- | --- |
| CR-001 | Low | Redaction quality | Current redaction generation is a placeholder text derivative rather than real PDF/DOCX redaction. | Replace the adapter with file-format-aware redaction before production use. |
| CR-002 | Low | PII detection | Current `containsPii` inference is based on client linkage for document summaries, which is conservative but coarse. | Introduce metadata-driven PII flags when configuration entities arrive. |

## Decision

Review decision:
Approved with follow-up tasks.

Follow-up tasks:
- Replace placeholder redaction output with format-aware redaction.
- Move from coarse linked-client PII inference to explicit metadata/config flags.

Reviewer notes:
The slice is coherent as a V1 enforcement layer: processor users get masked metadata and only redacted document actions, while supervisor users keep original access and those accesses are audited.
