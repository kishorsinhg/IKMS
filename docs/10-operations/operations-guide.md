# Operations Guide

## Administration Workspace Modules

The Administration workspace now includes operational modules for:

- Background Jobs
- Queues
- Scheduler
- Embeddings
- OCR
- AI Operations
- Cache
- Health
- Diagnostics

These modules reuse the existing grid, toolbar, drawer, context panel, and status-badge patterns.

## Standard Operator Procedures

### Reindex

- Use `Background Jobs` for job history and retry.
- Use `Queues` to pause or resume the reindex queue when controlled maintenance is required.
- Use `Scheduler` for nightly or manually triggered rebuilds.

### Embedding Recovery

- Use `Embeddings` or `Background Jobs` to inspect vector rebuild and embedding-regeneration jobs.
- Prefer targeted customer or document rebuilds before a full projection rebuild.

### OCR Recovery

- Use `OCR` or `Background Jobs` to inspect OCR retry jobs and failures.
- Retry OCR against the preserved source artifact instead of manually altering extracted text.

### AI Provider Diagnostics

- Use `AI Operations` and `Diagnostics` to inspect failed AI retry jobs, provider degradation, and dependency validation notes.

### Queue Control

- Queue pause or resume is persisted.
- Queue item retry and prioritization are exposed where the current workflow can safely honor them.
- Queue cancellation is practical for operational jobs and is stored as an operator override for workflow-backed queues.

### Cache Control

- `Clear`: remove cached entries.
- `Invalidate`: mark cache content stale.
- `Refresh`: request immediate cache refresh semantics.

## Health Interpretation

- `HEALTHY`: dependency or subsystem is available for normal operations.
- `WARNING`: degraded or incomplete state exists, but the platform remains operable.
- `CRITICAL`: manual operator intervention is required before relying on the subsystem.

## Product Boundary

Operational administration does not create policy or claim lifecycle ownership.

Policy Number, Claim Number, Broker Reference, Insurer, Effective Date, and Expiry Date remain Business Reference Fields only.

## Phase 11 References

- Observability and alerting foundation: `docs/10-operations/observability-alerting-foundation.md`
- Operational runbooks: `docs/10-operations/runbooks.md`
- Go-live readiness guide: `docs/10-operations/go-live-readiness-guide.md`
