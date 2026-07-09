# Data Model Index

The authoritative V1 feature data model lives at:

- `specs/001-insurance-broker-ikms/data-model.md`

This file remains as the stable `docs/` navigation point for data design.

## V1 Data Direction

- Client is the only master profile.
- Policy and claim values are searchable metadata, not authoritative managed
  records.
- Original document files are preserved outside the database and represented by
  immutable document versions.
- PostgreSQL with pgvector stores business records, audit records, metadata, and
  embeddings.

## Related Artifacts

- `specs/001-insurance-broker-ikms/contracts/api.md`
- `docs/10-operations/file-storage.md`
- `docs/08-security/security-baseline.md`
