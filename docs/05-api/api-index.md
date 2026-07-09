# API Index

The authoritative V1 API contract lives at:

- `specs/001-insurance-broker-ikms/contracts/api.md`

This file remains as the stable `docs/` navigation point for API design.

## V1 API Areas

- Authentication and session management
- Client management and CSV import
- Notes
- Document upload, preview, download, and versioning
- Email intake records
- Review queue
- Client-scoped search and AI Q&A
- Administration
- Audit search and CSV export

## Contract Rule

All API implementation must enforce role and PII checks before returning data or
assembling AI/RAG context.
