# API Index

The authoritative V1 API contract lives at:

- `specs/001-insurance-broker-ikms/contracts/api.md`
- `docs/06-architecture/phase-5a-enterprise-rag-blueprint.md` for the architecture-only API direction behind Phase 5A

This file remains as the stable `docs/` navigation point for API design.

## V1 API Areas

- Authentication and session management
- Client management and CSV import
- Notes
- Document upload, preview, download, and versioning
- Email intake records
- Review queue
- Client-scoped search and AI Q&A
- Enterprise knowledge search, evidence expansion, and streaming AI contracts
- Knowledge quality and stewardship
- Administration
- Audit search and CSV export

## Contract Rule

All API implementation must enforce role and PII checks before returning data or
assembling AI/RAG context.
