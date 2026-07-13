# Feature Specification: Insurance Broker IKMS V1 Requirements Baseline

**Feature Branch**: `001-insurance-broker-ikms`

**Created**: 2026-07-08

**Status**: Draft

**Input**: User description: "Insurance broker IKMS V1 requirements baseline"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build A Client Knowledge Profile (Priority: P1)

An authorized user can find or create an insurance broker client profile and view all linked client knowledge in separate sections for documents, emails, notes, and AI assistance. The broker's existing business system remains the source of truth; this system organizes knowledge and evidence around the client.

**Why this priority**: The client profile is the center of the product. Without a reliable client profile, document intake, review, search, and AI Q&A have no safe business context.

**Independent Test**: Can be tested by creating a client with a temporary ClientID, replacing it with an actual ClientID, adding a note, linking a document, and confirming the client profile shows the linked knowledge in the correct sections.

**Acceptance Scenarios**:

1. **Given** an authorized user creates a new client without an actual ClientID, **When** the user saves the profile, **Then** the system assigns a temporary ClientID and marks it as temporary.
2. **Given** a client has a temporary ClientID, **When** an authorized user replaces it with the broker's actual ClientID, **Then** the system enforces uniqueness and records the change in audit history.
3. **Given** a client has linked documents, emails, and notes, **When** a user opens the client profile, **Then** the profile displays separate sections for client details, documents, emails, notes, AI Q&A, and audit/activity.

---

### User Story 2 - Ingest And Review Client Knowledge (Priority: P1)

An Indexer can process incoming PDF, DOCX, and email knowledge from manual upload, shared folder intake, and configured mailboxes. Items that cannot be confidently linked to a client are routed to an unlinked review queue for manual linking and correction.

**Why this priority**: The system's value depends on reliably converting scattered documents and emails into searchable client knowledge while avoiding incorrect client links.

**Independent Test**: Can be tested by uploading a PDF without selecting a client, confirming it enters the unlinked review queue, manually linking it to a client, correcting metadata, and releasing it to the client profile.

**Acceptance Scenarios**:

1. **Given** a supported file is uploaded without a client, **When** the system cannot confidently match it to a client, **Then** it appears in the unlinked review queue.
2. **Given** an Indexer opens an unlinked item, **When** the Indexer selects a client, assigns or confirms document type, and approves metadata, **Then** the item is linked to that client and appears in the client profile.
3. **Given** an intake item has the exact same file hash as an existing stored file, **When** the item is processed, **Then** it is blocked or skipped as a duplicate and the event is audited.
4. **Given** an email has supported attachments, **When** the mailbox intake processes the email, **Then** the email is stored as an email item and supported attachments are stored as linked document items under the same parent email.
5. **Given** a scanned or image-only PDF cannot yield reliable text from built-in parsing, **When** intake processing runs, **Then** the system uses the configured OCR provider and routes low-confidence OCR output to review.

---

### User Story 3 - Search And Ask Questions About One Client (Priority: P1)

A Processor or Supervisor can search within one selected client profile and ask evidence-based questions about that client. AI answers use only authorized client knowledge and include source citations.

**Why this priority**: The primary business outcome is faster retrieval and summarization of client information without exposing unauthorized data or unsupported answers.

**Independent Test**: Can be tested by linking documents and emails to one client, asking a question from that client profile, and verifying the answer cites only that client's authorized sources.

**Acceptance Scenarios**:

1. **Given** a user opens a client profile, **When** the user asks "What recent renewal information is available?", **Then** the answer is generated only from that client's authorized documents, emails, notes, and metadata.
2. **Given** no supporting evidence exists for a question, **When** the user asks the question, **Then** the system responds that available authorized information does not contain supporting evidence.
3. **Given** conflicting evidence exists, **When** the user asks a related question, **Then** the system identifies the conflict and cites the conflicting sources.
4. **Given** a user asks the AI to approve, reject, underwrite, cancel, or decide a claim or policy, **When** the AI processes the request, **Then** it refuses the decision-making request and reminds the user to follow organizational procedures.
5. **Given** an AI answer cites a document-backed source, **When** the answer or source list is displayed, **Then** each citation includes the source name and the page number or nearest available source-location metadata.
6. **Given** relevant evidence is split across multiple documents, emails, or notes linked to the same client, **When** the user asks a question, **Then** the system may assemble one answer from those multiple client-authorized sources and cite each contributing source.

---

### User Story 4 - Protect PII With Role-Based Access (Priority: P1)

Processor users can work with client knowledge through masked fields and redacted previews/downloads, while Supervisor users can access PII and original documents when authorized. All sensitive access is audited.

**Why this priority**: The product handles regulated insurance information. PII protection and safe AI retrieval are mandatory for trust and compliance.

**Independent Test**: Can be tested by marking metadata as PII, logging in as Processor and Supervisor, and verifying that Processor receives only masked/redacted content while Supervisor can view original content.

**Acceptance Scenarios**:

1. **Given** a document contains PII, **When** a Processor previews or downloads it, **Then** the Processor receives a redacted version only.
2. **Given** redaction is unavailable or fails, **When** a Processor attempts to access a PII document, **Then** access to the original is blocked.
3. **Given** a Supervisor opens the same document, **When** the Supervisor has PII permission, **Then** the Supervisor can preview or download the original and the access is audited.
4. **Given** a Processor asks AI a question, **When** relevant evidence contains PII, **Then** the AI context and answer are filtered or redacted before the answer is produced.

---

### User Story 5 - Configure Broker Knowledge Rules (Priority: P2)

An Administrator can configure users, roles, intake sources, document types, metadata labels, PII flags, review behavior, and AI-provider settings without changing the broker's source-of-truth business system.

**Why this priority**: Small brokers need configuration flexibility, but V1 should avoid complex generic entity management.

**Independent Test**: Can be tested by configuring a document type, marking a metadata field as PII, configuring one intake mailbox and one shared folder path, and confirming new intake uses those settings.

**Acceptance Scenarios**:

1. **Given** an Administrator configures a document type, **When** a new document is processed, **Then** the document can be classified into that configured type.
2. **Given** an Administrator marks a metadata field as PII, **When** a Processor views the document metadata, **Then** the field is masked or hidden according to PII rules.
3. **Given** an Administrator configures a review mode, **When** new intake items are processed, **Then** the system routes items to review according to the configured mode and confidence rules.
4. **Given** an Administrator configures AI or OCR provider settings, **When** the Administrator validates or saves the configuration, **Then** the system confirms whether the configured endpoint, credentials, and required models are usable and records the outcome.

---

### User Story 6 - Audit And Govern System Activity (Priority: P2)

An Administrator or authorized Supervisor can search and export audit logs covering authentication, intake, review, document access, PII access, configuration changes, and AI activity.

**Why this priority**: Auditability is required for regulated broker operations and for trustworthy AI-assisted retrieval.

**Independent Test**: Can be tested by performing representative actions, filtering the audit log by user/date/action, and exporting the results.

**Acceptance Scenarios**:

1. **Given** a user asks a client-level AI question, **When** the answer is generated, **Then** the system records the user, client, question, sources used, timestamp, and whether PII-enabled context was used.
2. **Given** a user previews or downloads a document, **When** the action completes or is denied, **Then** the action is recorded in the audit log.
3. **Given** an authorized user filters audit logs, **When** the user exports the filtered results, **Then** a CSV file is produced containing the matching audit records.

### Edge Cases

- Incoming file has unsupported type: reject or mark unsupported without creating a searchable document.
- Same file is manually uploaded twice: block as duplicate and show that a duplicate already exists if the user has permission to know.
- Same file arrives by email or shared folder more than once: skip as duplicate and audit the skip.
- File is a new version of an existing document but has a different hash: allow authorized user to add it as a new version and mark it current.
- OCR or text extraction fails: retain original file, mark processing failed, and route to review or retry.
- Client match confidence is low or multiple clients match: route to unlinked review queue.
- AI provider is unavailable: search and stored content remain usable; AI features show unavailable/retry status.
- Prompt injection is detected in a document or chunk: flag the item, audit the event, and prevent malicious instructions from influencing AI answers.
- Redaction fails or confidence is unacceptable: block Processor access to original content.
- User asks cross-client AI question: refuse or require the user to select one client profile.
- User asks AI for claim, underwriting, policy cancellation, fraud, or approval decision: refuse decision-making request.
- Right-to-be-forgotten or retention request is received: support controlled deletion or anonymization workflow subject to legal retention rules.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a fixed Client master profile for V1, supporting individual and business clients.
- **FR-002**: System MUST maintain an internal client identifier and a business ClientID for each client.
- **FR-003**: System MUST generate a temporary ClientID when a client is created before the actual ClientID is known.
- **FR-004**: System MUST allow authorized users to replace a temporary ClientID with an actual unique ClientID and audit the change.
- **FR-005**: System MUST support manual client creation and client import from CSV with validation and duplicate warnings.
- **FR-006**: System MUST organize client knowledge into separate profile sections for documents, emails, notes, AI Q&A, and audit/activity.
- **FR-007**: System MUST support simple manual notes linked to a client, with create/update/delete or soft-delete audit history.
- **FR-008**: System MUST support manual upload of PDF and DOCX files.
- **FR-009**: System MUST support intake from one or more configured shared folder paths.
- **FR-010**: System MUST support intake from one or more configured IMAP mailboxes.
- **FR-011**: System MUST store email body, subject, sender, recipients, received date, message identifier, and attachment metadata.
- **FR-012**: System MUST store supported email attachments as document items linked to the parent email.
- **FR-013**: System MUST preserve original uploaded, scanned, or attached files unchanged.
- **FR-014**: System MUST support document versioning, with current version shown by default and older versions retained for authorized users.
- **FR-015**: System MUST block manual upload of an exact duplicate file hash and skip exact duplicates from automated intake.
- **FR-016**: System MUST support configurable document types for insurance broker use.
- **FR-017**: System MUST capture fixed insurance-oriented metadata plus predefined additional metadata fields.
- **FR-018**: System MUST allow Administrators to label additional metadata fields and mark metadata fields as PII.
- **FR-019**: System MUST classify supported documents and extract metadata for review or approval.
- **FR-019a**: System MUST support OCR-backed extraction for scanned or image-only supported documents when built-in text parsing does not produce reliable text.
- **FR-020**: System MUST route items with low-confidence client match, classification, extraction, or PII handling to review.
- **FR-021**: System MUST provide an unlinked review queue where Indexers can link items to clients, correct metadata, and release approved items.
- **FR-022**: System MUST allow configurable human review modes: full review, confidence-based review, sampling review, and straight-through processing for high-confidence items with exceptions.
- **FR-023**: System MUST provide keyword, metadata, and semantic search within a selected client profile.
- **FR-024**: System MUST provide client-level AI Q&A only; global cross-client AI Q&A is out of scope for V1.
- **FR-025**: System MUST ensure AI answers cite supporting source documents, emails, notes, or chunks.
- **FR-025a**: System MUST persist and return citation provenance metadata sufficient to show document name plus page number or nearest available source-location details for document-backed evidence.
- **FR-025b**: System MUST generate client-level answers from retrieved authorized evidence using the configured AI provider when available, while preserving refusal, no-evidence, and failure-safe fallback behavior.
- **FR-026**: System MUST respond with no supporting evidence found when authorized evidence is unavailable.
- **FR-027**: System MUST identify and present conflicting evidence when relevant sources disagree.
- **FR-027a**: System MUST support client-scoped evidence assembly across multiple linked documents, emails, and notes while preventing cross-client retrieval during search and AI Q&A.
- **FR-028**: System MUST refuse requests for business decision-making, including claim approval, underwriting, fraud determination, policy cancellation, or other binding decisions.
- **FR-029**: System MUST support user feedback on AI answers, search results, and extraction quality.
- **FR-030**: System MUST support local username/password authentication with account status, role assignment, session timeout, failed-login tracking, and login audit.
- **FR-031**: System MUST support four roles in V1: Indexer, Processor, Supervisor, and Administrator.
- **FR-032**: System MUST enforce role-based access before search retrieval, document preview, document download, AI context assembly, and AI answer generation.
- **FR-033**: System MUST mask or hide PII from Processor users.
- **FR-034**: System MUST provide redacted preview and redacted download for Processor users when redaction is available.
- **FR-035**: System MUST block Processor access to original PII-containing files when redaction is unavailable or failed.
- **FR-036**: System MUST allow Supervisor users with PII permission to preview and download original files.
- **FR-037**: System MUST audit all significant activity, including login, failed login, intake, review, metadata edits, duplicate detection, document access, PII access, AI activity, and configuration changes.
- **FR-038**: System MUST support audit log search and CSV export by authorized users.
- **FR-039**: System MUST support configurable retention policies and controlled deletion or anonymization workflow when legally required.
- **FR-040**: System MUST support English UI and English/German content processing for OCR, search, embeddings, and AI Q&A.
- **FR-040a**: System MUST allow administrators to validate configured AI/OCR provider connectivity and required model availability before relying on those settings in intake, search, or AI Q&A flows.

### Key Entities *(include if feature involves data)*

- **Client**: The fixed master profile for V1. Represents an individual or business insurance broker client. Includes internal ID, business ClientID, temporary/actual ClientID status, client type, display name, contact fields, address fields, status, and extension fields.
- **Document**: A client-linked knowledge item representing a logical document. Holds document type, client link, parent email link when applicable, current version, metadata, processing status, review status, and audit references.
- **Document Version**: A preserved original file version with its own file hash, extraction results, metadata snapshot, redaction status, and current/previous state.
- **Email**: An intake and knowledge item from a configured mailbox. Stores message metadata, body content, client link, processing status, and attachment relationships.
- **Note**: A manually entered text note linked to a client with creator, timestamp, update history, and audit status.
- **Document Type**: Administrator-configured classification category with labels, description, active status, metadata expectations, and extraction/review guidance.
- **Metadata Field/Value**: Fixed and predefined additional fields used for search, extraction, filtering, PII marking, and redaction.
- **Review Queue Item**: Intake or processing item requiring human review due to missing link, low confidence, duplicate uncertainty, extraction issue, redaction issue, or prompt injection risk.
- **User**: Authenticated application user with account status and one or more roles.
- **Role**: Permission grouping for Indexer, Processor, Supervisor, and Administrator.
- **Audit Log**: Immutable event record for security, compliance, AI traceability, and operational review.
- **AI Interaction**: Record of client-level AI question, retrieved evidence, source citations, user, timestamp, and PII-context status.
- **Embedding Chunk**: Searchable semantic content segment derived from authorized document text, email body, or note content, linked to client, source, version, sensitivity status, page/location provenance, and retrieval metadata used for hybrid search and AI citations.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Authorized users can locate a known client and open the client profile in under 10 seconds for at least 95% of normal searches.
- **SC-002**: Authorized users can upload or intake supported files and see them either linked to a client or placed in review within the configured processing SLA for at least 95% of items.
- **SC-003**: At least 95% of AI answers include one or more source citations or explicitly state that no supporting evidence was found.
- **SC-003a**: At least 95% of document-backed AI citations include the document name and page number or nearest available location metadata.
- **SC-004**: Unauthorized users receive zero unredacted PII in client profile fields, document previews/downloads, search snippets, or AI answers during role-based access testing.
- **SC-005**: 100% of AI Q&A interactions are traceable to user, client, question, timestamp, and source evidence used.
- **SC-006**: Exact duplicate file uploads are detected by hash and blocked or skipped in 100% of duplicate test cases.
- **SC-007**: Indexers can resolve an unlinked review item by selecting a client, correcting metadata, and releasing it to the client profile without administrator assistance.
- **SC-008**: Users can ask a client-level question over English or German source content and receive an answer in the question language or a clear no-evidence response.
- **SC-009**: The system refuses 100% of tested claim approval, underwriting, fraud, cancellation, and other business decision requests.
- **SC-010**: Audit logs can be filtered and exported for a selected date range, user, action type, and client.

## Assumptions

- V1 is a single-tenant deployment for one insurance broker, either on broker-controlled infrastructure or private cloud.
- The broker's existing business system remains the source of truth for policy, claim, and administrative records.
- Policy number, claim number, carrier, product type, dates, amounts, and similar values are searchable metadata, not authoritative managed policy or claim records in V1.
- Supported V1 content types are PDF, DOCX, and email bodies; email attachments are processed only when they are supported file types.
- Shared folder scanners are expected to output PDF or DOCX files for V1.
- The user interface is English only in V1.
- Source documents and user questions may be English or German.
- AI/OCR provider access is available through an administrator-configured external provider setting.
- Administrators configure metadata labels, PII flags, review behavior, document types, shared folder paths, and IMAP mailboxes.
- Processor users need working access to knowledge through masked fields and redacted copies, not original PII-containing files.
- Supervisor users can access PII and originals when authorized.
- Right-to-be-forgotten and retention actions require controlled administrative workflow and must account for legal retention obligations.
