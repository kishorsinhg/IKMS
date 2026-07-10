# Security, PII, And Guardrail Baseline

This baseline adapts the security and AI governance controls from `Deliverable_1_SRS.docx` to the simplified V1.

## Roles

- Indexer: review queue, manual linking, correction, indexing.
- Processor: client search, document/email/note view, client-level AI Q&A, no PII access.
- Supervisor: Processor permissions plus PII access.
- Administrator: configuration, users, roles, intake sources, document types, metadata labels, PII flags, review modes.

Administrator does not automatically require unrestricted business-data access unless explicitly granted.

## Security Trimming

Security filtering must occur before retrieval and before LLM context assembly.

Unauthorized documents, chunks, metadata, emails, and notes must never reach the LLM.

## PII

Fixed PII categories include:

- Email address
- Phone number
- Address
- Date of birth
- National ID/passport/driver license
- Tax ID
- Bank account/IBAN/payment details
- Medical/health information
- Credentials/secrets

Administrator can mark metadata fields as PII.

## Redaction

Processor can view and download redacted previews/copies only.

Supervisor can view and download originals.

If redaction fails or confidence is unacceptable, Processor access is blocked.

## AI Guardrails

- AI assists, humans decide.
- No evidence means no answer.
- Every AI answer must cite sources.
- Prompt injection must be detected during ingestion and retrieval.
- Decision-making requests must be refused.
- Conflicting evidence must be surfaced.

## 2026-07-10 Coverage Review

- Security trimming status: implemented as a shared pre-retrieval contract and applied in client search plus AI context assembly.
- PII masking status: implemented for client profile fields, email summaries, and processor-visible knowledge paths.
- Redaction status: processor preview/download is routed to redacted artifacts; supervisor can access originals; current redaction output remains placeholder text content.
- Audit status: authentication, intake, review, document access, PII/original access, configuration, and AI interactions now emit persisted audit records searchable through `/api/audit`.
- AI guardrail status: no-evidence, citation, conflict surfacing, and decision-refusal behaviors are implemented; answer synthesis remains rule-based rather than provider-backed.
- Retention status: policy workflow and tests exist, but retained/deleted file lifecycle integration is still not fully wired to persisted records.

## Remaining Gaps

- Prompt injection detection is documented as a requirement, but current ingestion/retrieval implementation does not yet include an explicit detector.
- Retrieval remains keyword-driven and not vector-ranked despite pgvector-ready schema support.
- Administrator user lifecycle and secret-bearing provider credentials remain intentionally incomplete.
