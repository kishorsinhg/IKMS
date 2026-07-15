# Customer Knowledge Timeline And Related Knowledge

## Boundary

- Customer remains the primary business context.
- Timeline and related-knowledge responses are derived from customer knowledge artifacts already stored in IKMS.
- Policy Number, Claim Number, Insurer, Effective Date, Expiry Date, Renewal Date, Broker Reference, and similar insurance values remain structured Business Reference Fields.
- Business Reference Fields can filter, group, and explain customer knowledge, but they do not create Policy or Claim entities, services, repositories, or workspaces.
- The broker management system remains the system of record for policy and claim lifecycle.

## Timeline Model

The Customer Knowledge Timeline is assembled dynamically from canonical customer knowledge artifacts:

- Documents
- Document versions
- Emails
- Notes
- Review queue activity
- Business Reference Field extraction or correction metadata
- AI conversations

Timeline responses expose:

- `CustomerKnowledgeTimelineEventResponse`
- stable `eventId`
- `eventType`, `sourceType`, `sourceId`, optional `sourceVersionId`
- `occurredAt` and `recordedAt`
- human-readable `title` and `summary`
- optional `actor`
- structured `businessReferenceFields`
- evidence references and available navigation actions

Ordering is deterministic:

1. `occurredAt` descending
2. `recordedAt` descending
3. stable `eventId` descending

Pagination uses a cursor derived from the last returned event identifier so page boundaries remain stable while the query sort stays deterministic.

## Event Taxonomy

Implemented event types are knowledge-oriented only:

- `DOCUMENT_CREATED`
- `DOCUMENT_RECEIVED`
- `DOCUMENT_VERSION_ADDED`
- `EMAIL_RECEIVED`
- `NOTE_CREATED`
- `NOTE_UPDATED`
- `REVIEW_CREATED`
- `REVIEW_APPROVED`
- `REVIEW_REJECTED`
- `BUSINESS_REFERENCE_EXTRACTED`
- `BUSINESS_REFERENCE_CORRECTED`
- `AI_CONVERSATION_CREATED`

No policy or claim lifecycle event taxonomy is introduced.

## Related Knowledge Model

Related Knowledge links customer knowledge sources through deterministic or clearly labelled inferred relationships.

Supported source types:

- `DOCUMENT`
- `DOCUMENT_VERSION`
- `EMAIL`
- `NOTE`
- `CUSTOMER` as a query scope only

Supported relationship types currently include:

- `EMAIL_ATTACHMENT`
- `VERSION_LINEAGE`
- `SAME_POLICY_REFERENCE`
- `SAME_CLAIM_REFERENCE`
- `SAME_INSURER`
- `SAME_EXTERNAL_REFERENCE`
- `SAME_BROKER_REFERENCE`
- `SAME_EMAIL_THREAD`
- `EXACT_DUPLICATE`
- `CONTENT_SIMILARITY`

`CONTENT_SIMILARITY` is explicitly marked inferred and reuses the existing PostgreSQL plus `pgvector` retrieval implementation. Deterministic links are preferred and rank above inferred links.

## Persistence Strategy

- Canonical source of truth remains the existing customer knowledge tables.
- Timeline events are derived dynamically at request time from canonical artifacts.
- Related-knowledge links are derived dynamically from canonical artifacts, business reference metadata, document lineage, content hashes, email linkage, and indexed embeddings.
- No new Policy or Claim persistence model is introduced.
- No new retrieval architecture is introduced.

## API Surface

Implemented endpoints:

- `GET /api/clients/{clientId}/knowledge/timeline`
- `GET /api/clients/{clientId}/knowledge/related`
- `GET /api/knowledge/sources/{sourceType}/{sourceId}/related`
- `GET /api/documents/{documentId}/versions`

These APIs preserve the existing customer-centric route structure and provide typed responses for Customer360, the document viewer, and assistant context surfaces.
