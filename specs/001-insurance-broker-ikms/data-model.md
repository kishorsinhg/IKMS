# Data Model: Insurance Broker IKMS V1

## Client

Fixed V1 master profile for an individual or business client.

Fields:

- `id`: internal UUID or sequence identifier
- `clientId`: broker business ClientID, unique
- `clientIdTemporary`: true when system-generated temporary ID is still in use
- `clientType`: Individual or Business
- `displayName`, `legalName`
- `status`: Active, Inactive, Archived
- Primary and secondary email, phone, contact person, and address fields
- Extension fields: text, long text, number, date, datetime, boolean sets with administrator labels
- `createdAt`, `createdBy`, `updatedAt`, `updatedBy`

Rules:

- Temporary ClientID is generated when actual ClientID is unknown.
- Replacement of temporary ClientID must enforce uniqueness and create an audit event.
- Client is the only master profile in V1.

## Document

Logical document linked to one client and optionally to a parent email.

Fields:

- `id`
- `clientId`
- `parentEmailId`
- `documentTypeId`
- `title`
- `currentVersionId`
- `processingStatus`: IntakeReceived, Extracting, Classified, Indexed, Failed
- `reviewStatus`: NotRequired, PendingReview, Approved, Rejected
- `clientMatchConfidence`, `classificationConfidence`, `extractionConfidence`
- `createdAt`, `source`: ManualUpload, SharedFolder, EmailAttachment

Rules:

- Unsupported files are rejected or marked unsupported.
- Exact duplicate file hashes are blocked/skipped and audited.
- Current version is shown by default; older versions remain available to authorized users.

## Document Version

Immutable preserved file instance.

Fields:

- `id`, `documentId`, `versionNumber`
- `fileHash`, `fileName`, `mimeType`, `fileSize`
- `originalStoragePath`, `redactedStoragePath`
- `extractedTextPath` or `extractedText`
- `ocrProvider`, `embeddingModel`, `language`
- `redactionStatus`: NotNeeded, Pending, Available, Failed, Blocked
- `isCurrent`, `createdAt`, `createdBy`

Rules:

- Original file bytes are not modified.
- A new hash can become a new version by authorized action.

## Email

Email body and metadata from configured mailbox intake.

Fields:

- `id`, `clientId`
- `mailboxConfigId`
- `messageId`, `threadId`
- `subject`, `sender`, `recipients`, `cc`, `receivedAt`
- `bodyText`, `bodyHtmlStoragePath`
- `processingStatus`, `reviewStatus`
- `createdAt`

Rules:

- Email without attachment uses default document/content type such as `Email`.
- Supported attachments become Document records linked to the email.

## Note

Manual text note linked to a client.

Fields:

- `id`, `clientId`
- `noteText`
- `status`: Active, Deleted
- `createdAt`, `createdBy`, `updatedAt`, `updatedBy`

Rules:

- Notes are simple manual text in V1.
- Create, update, and delete/soft-delete actions are audited.

## Document Type

Administrator-configured classification label.

Fields:

- `id`, `name`, `description`
- `active`
- `defaultForEmail`
- `reviewGuidance`
- `createdAt`, `updatedAt`

Rules:

- Document types are configurable for insurance broker use.
- Classification can suggest a document type, but review mode decides whether humans approve it.

## Metadata Field And Metadata Value

Searchable metadata for documents, emails, and clients.

Fixed document metadata examples:

- Policy number
- Claim number
- Carrier
- Product type
- Premium or amount
- Effective date
- Expiry date
- Reference number
- Sender
- Subject
- Source

Metadata Field fields:

- `id`, `scope`: Client, Document, Email
- `key`, `label`, `dataType`
- `fixed`, `active`, `pii`
- `searchable`, `displayOrder`

Metadata Value fields:

- `id`, `ownerType`, `ownerId`, `fieldId`
- typed value columns for text, long text, number, date, datetime, boolean
- `confidence`, `source`: Manual, AIExtracted, Imported

Rules:

- Administrators can label extension fields and mark fields as PII.
- PII metadata is masked or hidden from Processor users.

## Review Queue Item

Human review task for intake or processing exceptions.

Fields:

- `id`
- `itemType`: Document, Email, DocumentVersion
- `itemId`
- `reason`: Unlinked, LowClientConfidence, LowClassificationConfidence, LowExtractionConfidence, DuplicateUncertainty, RedactionFailed, PromptInjectionRisk, ProcessingFailed
- `assignedTo`, `status`: Open, InProgress, Resolved, Rejected
- `createdAt`, `resolvedAt`

Rules:

- Indexers can link items to clients, correct metadata, and release approved items.
- Review routing follows configured review mode.

## User And Role

Local authenticated account and role assignments.

User fields:

- `id`, `username`, `passwordHash`
- `displayName`, `email`
- `status`: Active, Locked, Disabled
- `failedLoginCount`, `lastLoginAt`

Roles:

- Indexer
- Processor
- Supervisor
- Administrator

Rules:

- Failed logins and login success are audited.
- Permissions are enforced before search, retrieval, preview, download, AI context assembly, and answer generation.

## Audit Log

Immutable operational and compliance event.

Fields:

- `id`, `timestamp`, `actorUserId`
- `action`
- `targetType`, `targetId`, `clientId`
- `outcome`: Success, Denied, Failed, Skipped
- `piiAccessed`
- `detailsJson`
- `requestId`, `ipAddress`

Rules:

- Audit covers authentication, intake, review, metadata edits, duplicate detection, document access, PII access, AI activity, configuration changes, and exports.
- Authorized users can filter and export audit logs to CSV.

## AI Interaction

Trace of client-level AI Q&A.

Fields:

- `id`, `clientId`, `userId`
- `question`, `answer`
- `answerStatus`: Answered, NoEvidence, Refused, Failed
- `sourceCitationsJson`
- `piiContextUsed`
- `modelProvider`, `modelName`
- `createdAt`

Rules:

- AI answers must cite sources or state no supporting evidence.
- Decision-making requests are refused and audited.

## Embedding Chunk

Searchable content segment for semantic retrieval.

Fields:

- `id`, `clientId`
- `sourceType`: DocumentVersion, Email, Note
- `sourceId`
- `chunkText`
- `embedding`
- `language`
- `piiDetected`
- `promptInjectionFlag`
- `createdAt`

Rules:

- Chunks inherit access rules from their source.
- Unauthorized or unredacted PII chunks must not enter Processor retrieval context.
