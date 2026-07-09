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

