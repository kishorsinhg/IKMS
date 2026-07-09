# Decision Log

| ID | Date | Decision | Rationale | Status |
| --- | --- | --- | --- | --- |
| DEC-001 | 2026-07-07 | V1 targets insurance brokers only, not generic industry configuration. | Reduces complexity and speeds first build. | Accepted |
| DEC-002 | 2026-07-07 | Client is the only master record. | Keeps 360 view simple. | Accepted |
| DEC-003 | 2026-07-07 | Policy/claim data is searchable metadata, not authoritative records. | Broker system remains source of truth. | Accepted |
| DEC-004 | 2026-07-07 | Spring Boot backend and React frontend. | User preference and enterprise-friendly stack. | Accepted |
| DEC-005 | 2026-07-07 | PostgreSQL with pgvector. | Supports relational, full-text, and vector search in one DB. | Accepted |
| DEC-006 | 2026-07-07 | Single-tenant on-premise/private cloud deployment. | Avoids SaaS complexity. | Accepted |
| DEC-007 | 2026-07-07 | Local username/password for V1. | Keeps deployment simple. | Accepted |
| DEC-008 | 2026-07-07 | Mistral Cloud first, AI provider configurable. | Enables testing now, extension later. | Accepted |
| DEC-009 | 2026-07-07 | Client-level RAG only. | Simpler privacy and retrieval boundary. | Accepted |
| DEC-010 | 2026-07-07 | Processor sees redacted content; Supervisor sees PII/original. | Matches role separation. | Accepted |
| DEC-011 | 2026-07-07 | Original files are preserved and versioned. | Audit and legal evidence. | Accepted |
| DEC-012 | 2026-07-07 | Exact duplicate hashes are blocked/skipped. | Prevents clutter while preserving versions. | Accepted |

