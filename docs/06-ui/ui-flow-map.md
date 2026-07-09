# UI Flow Index

The authoritative V1 UI contract lives at:

- `specs/001-insurance-broker-ikms/contracts/ui.md`
- `docs/06-ui/ui-design-guidelines.md`

This file remains as the stable `docs/` navigation point for UI flow design.

## Primary V1 Areas

- Login and protected app shell
- Client search and Client profile
- Documents, Emails, Notes, AI Q&A, and Audit/Activity sections
- Manual upload and intake status
- Unlinked review queue and correction workflow
- Administration for users, document types, metadata/PII flags, intake sources,
  review mode, AI settings, and retention settings
- Audit search and CSV export

## Role Notes

- Indexer primarily uses review queue and correction screens.
- Processor uses client search, client profile, redacted document access, and
  client-level AI Q&A.
- Supervisor uses Processor screens plus PII/original document access.
- Administrator uses configuration and user management screens.
