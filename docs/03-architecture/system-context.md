# System Context

## Architecture Choice

V1 is a Spring Boot monolith with React frontend, PostgreSQL database, and pgvector for embeddings.

The system uses one codebase with two runtime profiles:

- Web profile: APIs, UI, authentication, client workspace, review screens, search, AI Q&A.
- Worker profile: shared folder polling, IMAP polling, OCR, classification, extraction, indexing, redaction, background jobs.

Both profiles share the same PostgreSQL database and controlled file storage.

## Deployment Model

Single-tenant deployment:

- On-premise broker server, or
- Private cloud deployment for one broker.

No multi-tenant SaaS in V1.

## External Services

- Configurable AI provider.
- Mistral Cloud is the first tested AI/OCR provider.
- IMAP mailboxes for email intake.

