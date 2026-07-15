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
- `POST /api/documents/process`: rerun processing for an existing document using the preserved source artifact.
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
- `POST /api/review-queue/{itemId}/retry`: retry the processing pipeline for a document-backed review item.

Rules:

- Indexer can resolve unlinked and low-confidence items.
- Review actions are audited.
- Review item responses may include a `processingJob` payload with reviewer-visible status, stage, confidence, extracted fields, findings, reviewer comments, and processing timestamps.
- Processing fields must expose extracted, corrected, and approved value lineage without creating Policy or Claim entities.
- Retry is an operational document-processing action only. It is not a policy or claim workflow operation.

## Search And AI Q&A

- `GET /api/clients/{clientId}/search?query=&filters=`: keyword, metadata, and semantic search within one client.
- `POST /api/clients/{clientId}/ask`: client-level RAG question.
- `POST /api/clients/{clientId}/summarize`: customer-scoped summary from authorized customer knowledge.
- `POST /api/clients/{clientId}/explain`: customer-scoped explanation from authorized evidence.
- `POST /api/clients/{clientId}/compare`: compare customer-linked documents or versions.
- `POST /api/clients/{clientId}/extract`: extract structured knowledge fields from customer-linked evidence.
- `POST /api/clients/{clientId}/validate`: validate extracted fields against customer-linked evidence.
- `POST /api/ask`: global ask across authorized customer knowledge with customer-aware scoping and no policy/claim entity lookup.
- `GET /api/search/knowledge?query=&customerId=&documentType=&from=&to=&policyNumber=&claimNumber=&insurer=`: enterprise knowledge search across authorized customer knowledge.
- `GET /api/ai/conversations/{conversationId}`: retrieve conversation history and grounded responses.
- `POST /api/ai/conversations/{conversationId}/continue`: continue a customer-centric conversation with prior evidence history.
- `GET /api/ai/interactions/{interactionId}/evidence`: expand evidence, citations, and supporting attributes for a grounded answer.
- `POST /api/ai/stream`: streaming ask/search response contract for enterprise AI workflows.
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

### Enterprise Retrieval Boundary

- Customer remains the primary business context.
- Policy Number, Claim Number, Insurer, Effective Date, Expiry Date, Renewal Date, Broker Reference, External System Reference, and similar insurance values are structured Business Reference Fields.
- Business Reference Fields are searchable, filterable, and indexable.
- IKMS must not expose Policy CRUD, Claim CRUD, Policy360, Claim360, or any lifecycle-management API for policy or claim records.
- Policy or claim lookups must be resolved through customer-linked knowledge and business-reference fields, not through IKMS-owned policy or claim entities.

## Customer Knowledge Timeline And Related Knowledge

- `GET /api/clients/{clientId}/knowledge/timeline`: retrieve customer-centric timeline events with cursor pagination and optional filters for date, source type, event type, review status, actor, and Business Reference Fields.
- `GET /api/clients/{clientId}/knowledge/related`: retrieve related customer knowledge links ranked by deterministic lineage, shared Business Reference Fields, duplication, and similarity.
- `GET /api/knowledge/sources/{sourceType}/{sourceId}/related`: retrieve related knowledge for one source without introducing standalone policy or claim routes.
- `GET /api/documents/{documentId}/versions`: retrieve document version history for viewer and related-knowledge use cases.

Rules:

- Timeline events must be derived from supported customer knowledge artifacts and review activity.
- Policy Number and Claim Number remain Business Reference Field filters only.
- Related Knowledge may group sources by shared references, lineage, duplication, or inferred similarity, but it must not create Policy or Claim entities.
- Similarity-based relationships must be clearly marked inferred and must reuse the current PostgreSQL plus pgvector retrieval platform.

## Knowledge Quality And Stewardship

- `GET /api/knowledge-quality/customers`: list customer knowledge quality summaries.
- `GET /api/knowledge-quality/customer/{clientId}`: retrieve one customer quality snapshot and open issues.
- `GET /api/knowledge-quality/issues?clientId=`: retrieve an issue queue for stewardship review.
- `POST /api/knowledge-quality/revalidate`: recompute quality projections for one or more customers.
- `POST /api/knowledge-quality/reindex`: rerun publish/index refresh for one or more customers through the existing retrieval pipeline.
- `POST /api/knowledge-quality/bulk-correct`: submit steward-driven bulk correction actions for metadata, Business Reference Fields, customer reassignment, or controlled republish.

Rules:

- Quality scores must remain explainable and customer-centric.
- Policy Number, Claim Number, Insurer, Broker Reference, Effective Date, Expiry Date, and Renewal Date remain structured Business Reference Fields only.
- Quality APIs must not introduce Policy services, Claim services, Policy360, Claim360, or lifecycle-management routes.
- Bulk correction and reindex actions are auditable stewardship operations, not policy or claim workflow operations.

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

## Operations

- `GET /api/operations/jobs`
- `GET /api/operations/jobs/{jobId}`
- `POST /api/operations/jobs/{jobId}/retry`
- `POST /api/operations/jobs/{jobId}/cancel`
- `GET /api/operations/queues`
- `GET /api/operations/queues/{queueKey}/items`
- `POST /api/operations/queues/{queueKey}/pause`
- `POST /api/operations/queues/{queueKey}/resume`
- `POST /api/operations/queues/{queueKey}/items/{itemId}/retry`
- `POST /api/operations/queues/{queueKey}/items/{itemId}/cancel`
- `POST /api/operations/queues/{queueKey}/items/{itemId}/prioritize`
- `GET /api/operations/schedulers`
- `POST /api/operations/schedulers/{schedulerKey}/enable`
- `POST /api/operations/schedulers/{schedulerKey}/disable`
- `POST /api/operations/schedulers/{schedulerKey}/run`
- `POST /api/operations/reindex`
- `POST /api/operations/embeddings/rebuild`
- `POST /api/operations/ocr/retry/{documentId}`
- `POST /api/operations/ai/retry/{interactionId}`
- `GET /api/operations/cache`
- `POST /api/operations/cache/{cacheKey}/clear`
- `POST /api/operations/cache/{cacheKey}/invalidate`
- `POST /api/operations/cache/{cacheKey}/refresh`
- `GET /api/operations/health`
- `GET /api/operations/diagnostics`

Rules:

- Operational actions are audited.
- Operational permissions remain additive to current RBAC and must not weaken existing security.
- Reindex, embedding, OCR, and AI operations remain platform operations over customer-linked knowledge, not Policy or Claim workflows.

## Governance

- `GET /api/governance/classification`
- `POST /api/governance/classification`
- `GET /api/governance/retention`
- `POST /api/governance/retention`
- `GET /api/governance/legal-holds`
- `POST /api/governance/legal-holds`
- `POST /api/governance/reclassify/{documentId}`
- `GET /api/governance/ai`
- `POST /api/governance/ai`
- `GET /api/governance/security`
- `POST /api/governance/security`
- `GET /api/governance/reports/compliance`

Rules:

- Configuration changes are audited.
- Administrators can mark metadata fields as PII.
- Administrator business-data access remains permission-based.
- Governance changes remain additive to existing authorization and do not introduce Policy or Claim entities.
