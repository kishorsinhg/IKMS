# Audit Baseline

## Audit Scope

Audit logs shall cover:

- Login, logout, failed login
- Client create/update
- Temporary ClientID replacement
- Document upload/intake
- Document duplicate detection
- Document version creation
- Document preview/download
- Redacted preview/download
- Email intake
- Note create/update/delete
- OCR/classification/extraction processing status
- Metadata edits
- PII field access/reveal
- Review queue actions
- AI Q&A usage
- AI answer source citations
- Prompt injection detection
- Role/user/configuration changes
- Document type and PII flag changes

## Export

V1 shall support CSV audit export with filters:

- Date range
- User
- Action type
- Client
- Document/email/note

## AI Traceability

Each AI answer should be traceable to:

- User
- Client
- Question
- Retrieved source IDs/chunk IDs
- Whether PII-enabled context was used
- Answer timestamp
- Response ID

Avoid storing full AI answers indefinitely unless retention is explicitly approved.

