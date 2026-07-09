# Quickstart Validation Guide: Insurance Broker IKMS V1

This guide defines end-to-end validation scenarios for the V1 feature. It intentionally avoids implementation code.

## Prerequisites

- PostgreSQL with pgvector available.
- Backend configured with local username/password auth.
- File storage path configured for originals and redacted copies.
- One AI/OCR provider configured, initially Mistral Cloud.
- One shared folder path and one IMAP mailbox configured for intake tests.
- Test users for Indexer, Processor, Supervisor, and Administrator roles.

## Validation Scenario 1: Client Profile

1. Log in as a user allowed to create clients.
2. Create an individual client without actual ClientID.
3. Confirm a temporary ClientID is generated.
4. Replace it with an actual unique ClientID.
5. Add a manual note.
6. Open the client profile.

Expected result:

- Client profile shows Client Profile, Documents, Emails, Notes, AI Q&A, and Audit/Activity sections.
- ClientID replacement and note creation are audited.

## Validation Scenario 2: Manual Upload And Review

1. Upload a supported PDF without selecting a client.
2. Confirm the item enters the unlinked review queue if client match is unavailable or low confidence.
3. Log in as Indexer.
4. Link the item to a client, set document type, correct metadata, and approve it.
5. Open the client profile.

Expected result:

- The document appears under the selected client.
- Metadata is searchable.
- Review actions are audited.

## Validation Scenario 3: Email Intake

1. Send a test email to a configured IMAP mailbox with one supported attachment.
2. Run or wait for mailbox intake.
3. Open the linked client profile or review queue.

Expected result:

- Email body is stored as an email item.
- Supported attachment is stored as a linked document item.
- Email without attachment uses default email content/document type behavior.

## Validation Scenario 4: Duplicate Detection And Versioning

1. Upload the same file twice.
2. Upload a different file as a new version of an existing document.

Expected result:

- Exact duplicate hash is blocked or skipped and audited.
- Different hash is accepted as a new version when authorized.
- Original file versions remain preserved.

## Validation Scenario 5: Processor PII Protection

1. Mark a metadata field as PII.
2. Use or create a document containing PII.
3. Log in as Processor.
4. Open the client profile, preview the document, download the document, and ask an AI question.

Expected result:

- PII metadata is masked or hidden.
- Preview/download returns redacted content only.
- If redaction failed, original access is denied.
- AI answer does not expose unredacted PII.

## Validation Scenario 6: Supervisor Original Access

1. Log in as Supervisor with PII permission.
2. Open the same client and document from Scenario 5.
3. Preview and download the original.

Expected result:

- Supervisor can access original content.
- PII/original access is audited.

## Validation Scenario 7: Client-Level Search And Q&A

1. Link English and German source content to a client.
2. Search within that client.
3. Ask a question that has supporting evidence.
4. Ask a question with no supporting evidence.
5. Ask a prohibited decision-making question.

Expected result:

- Search returns only authorized client-scoped results.
- Evidence-backed answers include citations.
- No-evidence question returns a no-evidence response.
- Decision-making question is refused.

## Validation Scenario 8: Audit Export

1. Perform login, upload, review, search, AI Q&A, preview, download, PII access, and configuration actions.
2. Log in as an authorized audit user.
3. Filter audit logs by date, user, action, client, and PII access.
4. Export filtered results.

Expected result:

- Matching audit records are visible.
- CSV export contains the filtered audit records.
