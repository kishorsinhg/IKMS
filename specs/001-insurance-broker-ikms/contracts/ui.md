# UI Contract: Insurance Broker IKMS V1

## Primary Navigation

- Clients
- Review Queue
- Intake
- Search
- Administration
- Audit

Navigation visibility depends on role permissions.

## Client Profile

Sections:

- Client Profile
- Documents
- Emails
- Notes
- AI Q&A
- Audit/Activity

Rules:

- The profile is the central 360-degree view.
- Documents, emails, notes, search results, and AI answers stay within the selected client.
- Processor sees masked PII and redacted document actions.
- Supervisor can access original document actions when permitted.

## Review Queue

Expected controls:

- Filter by reason, status, source, date, and confidence.
- Open review item.
- Select or search client.
- Correct document type and metadata.
- Approve, reject, or leave pending.

Rules:

- Unlinked items must be resolvable without administrator assistance.
- Review outcome is visible and audited.

## Intake

Expected controls:

- Manual upload for PDF/DOCX.
- Display upload duplicate result.
- View shared-folder and IMAP intake status for authorized roles.

Rules:

- Unsupported files show clear rejected/unsupported state.
- Duplicate items show that duplicate already exists when user has permission to know.

## Search And AI Q&A

Expected controls:

- Client-scoped search box.
- Filters for document type, metadata, source, and date.
- Ask panel within client profile.
- Source citations attached to answers.
- Feedback control for answer/search quality.

Rules:

- Cross-client Q&A is not offered in V1.
- No-evidence and refusal responses are normal first-class states.
- Conflicting sources are shown with citations.

## Administration

Expected areas:

- Users and roles
- Document types
- Metadata labels and PII flags
- Shared folder paths
- IMAP mailboxes
- Review mode
- AI/OCR provider settings
- Retention settings

Rules:

- Configuration screens should be practical and compact for small broker administration.
- Configuration changes are audited.

## Audit

Expected controls:

- Filter by date range, user, action, outcome, client, and PII access.
- Export filtered results to CSV.

Rules:

- AI Q&A, search, preview, download, PII reveal, and denied access are traceable.
