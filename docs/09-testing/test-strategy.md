# Test Strategy

## Test Levels

- Unit tests for domain rules and services.
- Integration tests for PostgreSQL, file storage, IMAP adapter, job queue, and AI provider abstraction.
- API tests for backend endpoints.
- UI tests for critical React workflows.
- Security tests for role and PII behavior.
- RAG tests for retrieval filtering and source citation.
- Code review gates for each completed SDS slice.

## Required Test Case Areas

- Authentication and roles.
- Client creation and CSV import.
- Temporary ClientID replacement.
- Manual document upload.
- Duplicate hash detection.
- Document versioning.
- Shared folder intake.
- IMAP email intake.
- Unlinked review queue.
- OCR/classification/extraction workflow.
- Metadata correction.
- Processor redaction behavior.
- Supervisor original access.
- Client-level RAG with citations.
- No-evidence response behavior.
- Decision boundary refusal.
- Prompt injection detection.
- Audit export.

## Traceability

Each test case should reference at least one requirement ID from `01-requirements/v1-srs.md`.

## Code Review

Use `docs/09-testing/code-review-strategy.md` and
`docs/templates/code-review-template.md` for slice review gates.
