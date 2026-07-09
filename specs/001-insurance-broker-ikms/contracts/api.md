# API Contract: Insurance Broker IKMS V1

This is a behavioral API contract for planning. Exact OpenAPI schemas can be generated during implementation.

## Authentication

- `POST /api/auth/login`: username/password login, returns session or token.
- `POST /api/auth/logout`: ends session.
- `GET /api/auth/me`: returns current user, roles, and permission summary.

Rules:

- Login success and failure are audited.
- Account status, failed-login tracking, and session timeout apply.

## Clients

- `GET /api/clients?query=&status=`: search clients.
- `POST /api/clients`: create client; generates temporary ClientID if actual ID is omitted.
- `GET /api/clients/{clientId}`: retrieve client profile with role-filtered fields.
- `PATCH /api/clients/{clientId}`: update client profile.
- `POST /api/clients/import`: import clients from CSV with validation result.
- `GET /api/clients/{clientId}/activity`: retrieve client activity/audit summary.

Rules:

- ClientID must be unique.
- Temporary-to-actual ClientID replacement is audited.
- Processor sees masked PII fields.

## Notes

- `GET /api/clients/{clientId}/notes`: list notes.
- `POST /api/clients/{clientId}/notes`: create note.
- `PATCH /api/notes/{noteId}`: update note.
- `DELETE /api/notes/{noteId}`: soft-delete note.

Rules:

- Notes are client-level only.
- Note changes are audited.

## Documents And Emails

- `POST /api/documents/upload`: upload PDF or DOCX, optionally with selected client.
- `GET /api/clients/{clientId}/documents`: list client documents with filters.
- `GET /api/documents/{documentId}`: get document metadata and version summary.
- `GET /api/documents/{documentId}/preview`: preview original or redacted content based on role.
- `GET /api/documents/{documentId}/download`: download original or redacted content based on role.
- `POST /api/documents/{documentId}/versions`: add a new version.
- `GET /api/clients/{clientId}/emails`: list client emails.
- `GET /api/emails/{emailId}`: get email body, metadata, and linked attachments.

Rules:

- Processor preview/download returns redacted content only.
- Supervisor with permission can retrieve originals.
- If redaction failed, Processor original access is denied.
- Exact duplicates return a duplicate response and create audit records.

## Review Queue

- `GET /api/review-queue?status=&reason=`: list review items.
- `GET /api/review-queue/{itemId}`: get review item details.
- `POST /api/review-queue/{itemId}/link-client`: link item to client.
- `PATCH /api/review-queue/{itemId}/metadata`: correct metadata.
- `POST /api/review-queue/{itemId}/approve`: release reviewed item.
- `POST /api/review-queue/{itemId}/reject`: reject item with reason.

Rules:

- Indexer can resolve unlinked and low-confidence items.
- Review actions are audited.

## Search And AI Q&A

- `GET /api/clients/{clientId}/search?query=&filters=`: keyword, metadata, and semantic search within one client.
- `POST /api/clients/{clientId}/ask`: client-level RAG question.
- `POST /api/ai-interactions/{interactionId}/feedback`: feedback on AI answer quality.

AI answer statuses:

- `Answered`: answer includes citations.
- `NoEvidence`: authorized evidence was unavailable.
- `Refused`: user asked for a prohibited decision or cross-client answer.
- `Failed`: provider or processing failure.

Rules:

- Retrieval is scoped to one client.
- Authorization and PII filters run before retrieval and LLM context assembly.
- Answers cite sources, surface conflicts, and refuse claim/policy/underwriting/fraud decisions.

## Administration

- `GET/POST/PATCH /api/admin/users`
- `GET/POST/PATCH /api/admin/document-types`
- `GET/POST/PATCH /api/admin/metadata-fields`
- `GET/POST/PATCH /api/admin/intake/shared-folders`
- `GET/POST/PATCH /api/admin/intake/mailboxes`
- `GET/POST/PATCH /api/admin/review-settings`
- `GET/POST/PATCH /api/admin/ai-settings`
- `GET /api/admin/audit-logs`
- `GET /api/admin/audit-logs/export.csv`

Rules:

- Configuration changes are audited.
- Administrators can mark metadata fields as PII.
- Administrator business-data access remains permission-based.
