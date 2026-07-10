# Intake And Review Code Review

## Review Metadata

- Slice: User Story 2 intake and review
- Reviewer: Codex
- Date: 2026-07-10
- Related tasks: T048-T069
- Related user story: US2 - Ingest And Review Client Knowledge
- Related requirements: FR-008, FR-013, FR-015, FR-016, FR-017, FR-018
- Related tests: `backend/src/test/java/com/ikms/document/DocumentUploadTest.java`, `backend/src/test/java/com/ikms/review/ReviewQueueWorkflowTest.java`, `frontend/src/features/intake/IntakePage.test.tsx`, `frontend/src/features/intake/review/ReviewQueue.test.tsx`, `frontend/src/features/clients/ClientProfile.test.tsx`
- Outcome: Approved with follow-up tasks

## Scope Reviewed

- Files changed: intake/review backend services and controllers, frontend intake/review/client knowledge screens, related API bindings, and storage wiring
- APIs changed: `POST /api/documents/upload`, `GET /api/clients/{clientId}/documents`, `GET /api/clients/{clientId}/emails`, `GET/POST/PATCH /api/review-queue/*`
- Data model or migration changes: none in this closing slice beyond prior US2 schema work
- UI changes: live intake upload, duplicate outcome, review queue workflow actions, client profile document/email sections
- Configuration changes: local file storage root defaults to `${java.io.tmpdir}/ikms-storage`

## Constitution Gates

- Client-centric V1 scope respected: Yes
- Monolithic single-tenant simplicity preserved: Yes
- Evidence-before-answer preserved: Yes
- Security/PII before retrieval enforced: Not yet applicable to this slice; deferred to US4/US3 work
- Original files preserved: Yes
- Audit coverage present: Yes for upload and review actions
- Tests trace to requirements: Yes

## Functional Review

- Acceptance scenarios covered: Upload without client, review queue listing, client linking, metadata correction, approval, and profile visibility
- Edge cases covered: exact duplicate upload outcome, unsupported upload rejection path, empty review filters
- Error handling reviewed: upload validation and review metadata validation return API errors through the shared handler
- Role behavior reviewed: no new role split introduced in this slice

## Security, PII, Audit, And AI Review

- Authentication and authorization: Existing session foundation reused; no new endpoint-specific authorization added yet
- PII masking/redaction: Deferred to US4
- Original file access: originals stored through `FileStorageService`; no new preview/download exposure added
- Security trimming before retrieval: Deferred to US3/US4
- Audit events: duplicate upload, successful upload, client linking, metadata correction, approve, reject
- AI guardrails and citations: not part of this slice
- Prompt injection handling: not part of this slice
- Retention/deletion/anonymization: unchanged

## Test Evidence

- Unit tests: duplicate detection, document versioning, upload service
- Integration tests: Spring context and Docker-backed smoke path still pass
- Contract tests: document upload and review queue workflow response shapes
- UI tests: intake upload, review queue action path, client profile knowledge sections
- Quickstart or manual validation: `mvn test`, `npm test -- --run`, `npm run build`

## Findings

| ID | Severity | Area | Finding | Required Action |
| --- | --- | --- | --- | --- |
| CR-001 | Low | Review detail | Review item detail still exposes queue metadata only; extracted evidence and attachment context are not yet surfaced in the UI. | Expand review detail payload when richer inspection is needed. |
| CR-002 | Low | Intake status | Shared-folder and IMAP status cards are informative but not backed by live worker telemetry yet. | Add runtime intake status endpoints during admin/operations work. |

## Decision

Review decision:
Approved with follow-up tasks.

Follow-up tasks:
- Add richer document/email detail payloads for review inspection.
- Add live operational status endpoints for automated intake workers.

Reviewer notes:
The US2 slice is internally consistent now: manual upload reaches persisted storage, duplicate outcomes are surfaced in the UI, review actions round-trip to the backend, and approved knowledge is visible on the client profile.
