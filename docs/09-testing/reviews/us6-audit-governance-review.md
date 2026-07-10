# Audit And Governance Code Review

## Review Metadata

- Slice: User Story 6 audit and governance
- Reviewer: Codex
- Date: 2026-07-10
- Related tasks: T105-T114
- Related user story: US6 - Audit And Govern System Activity
- Related requirements: FR-033, FR-034, FR-035
- Related tests: `backend/src/test/java/com/ikms/audit/AuditExportTest.java`, `frontend/src/features/audit/AuditPage.test.tsx`
- Outcome: Approved with follow-up tasks

## Scope Reviewed

- Files changed: persisted audit model, search/export services, audit controller, audit API client, audit page
- APIs changed: `/api/audit`, `/api/audit/export`
- Data model or migration changes: finalized `audit_log` entity with actor username, retention horizon, and search indexes
- UI changes: audit search screen with filters, event list, and CSV preview/export flow
- Configuration changes: none beyond baseline schema refinement

## Constitution Gates

- Client-centric V1 scope respected: Yes
- Monolithic single-tenant simplicity preserved: Yes
- Evidence-before-answer preserved: N/A for this slice
- Security/PII before retrieval enforced: Yes, audit access stays permission-gated
- Original files preserved: Unchanged
- Audit coverage present: Yes, audit writes now persist to `audit_log`
- Tests trace to requirements: Yes

## Functional Review

- Acceptance scenarios covered: filter by actor, action, client, and export CSV
- Edge cases covered: null filters, empty details payloads, users without export permission
- Error handling reviewed: audit controller returns `403` on missing permissions through shared exception handling
- Role behavior reviewed: `VIEW_AUDIT` required for search, `EXPORT_AUDIT` required for export

## Security, PII, Audit, And AI Review

- Authentication and authorization: existing session auth reused
- PII masking/redaction: audit rows surface whether PII access occurred without exposing new original-content paths
- Original file access: unchanged
- Security trimming before retrieval: unchanged
- Audit events: existing emitters now persist into the searchable audit store
- AI guardrails and citations: unchanged
- Prompt injection handling: unchanged
- Retention/deletion/anonymization: retention horizon captured per audit row

## Test Evidence

- Unit tests: audit controller/export contract coverage
- Integration tests: Spring context and backend test suite pass
- Contract tests: `/api/audit` and `/api/audit/export` response shapes
- UI tests: audit page renders rows, applies filters, and previews exported CSV
- Quickstart or manual validation: `mvn test`, `npm test -- --run`, `npm run build`

## Findings

| ID | Severity | Area | Finding | Required Action |
| --- | --- | --- | --- | --- |
| CR-001 | Low | Query depth | Audit filtering is limited to actor/action/client/date and does not yet include category or outcome facets. | Extend search filters if governance reviewers need finer slicing. |
| CR-002 | Low | Export UX | The frontend currently previews exported CSV in-page instead of triggering a downloaded file artifact. | Add browser download behavior if operators require one-click export files. |

## Decision

Review decision:
Approved with follow-up tasks.

Follow-up tasks:
- Add richer audit filters such as category and outcome if governance review expands.
- Convert CSV preview into a real browser download flow when UX requirements demand it.

Reviewer notes:
This slice closes the main governance gap from foundation by moving audit activity from log-only emission into a persisted, searchable, exportable application path.
