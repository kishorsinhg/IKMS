# Enterprise Operations Platform

## Purpose

Phase 9 turns IKMS into an enterprise-operable platform without changing the customer-centric product boundary.

Customer remains the primary business context.

Policy Number, Claim Number, Broker Reference, Insurer, Effective Date, Expiry Date, and similar insurance values remain Business Reference Fields only.

No Policy entity or Claim entity is introduced by the operations layer.

## Operational Building Blocks

- `OperationsJob`: reusable background-job record for reindex, embedding rebuild, OCR retry, AI retry, metadata rebuild, and scheduler-dispatched maintenance work.
- `OperationsQueueState`: persisted pause or resume state for operational queues.
- `OperationsQueueItemOverride`: operator priority and cancellation override state for queue items where workflow-safe control is practical.
- `OperationsScheduler`: persisted scheduler definition with enablement, interval, next execution, and last execution outcome.
- `OperationsSchedulerExecution`: execution-history record for manual and scheduled runs.
- `OperationsMetric`: durable metrics store for queue size, processing duration, OCR success, AI success, embedding success, retry counts, and reindex duration.
- `OperationsCacheService`: administration-facing cache control surface for retrieval, AI, metadata, and configuration caches.

## Responsibilities

### Background Jobs

- Accept asynchronous requests.
- Persist `queued`, `running`, `completed`, `failed`, `retrying`, and `cancelled` status.
- Track `jobId`, `jobType`, `submittedBy`, `submittedAt`, `startedAt`, `completedAt`, `duration`, `status`, `progress`, `errorSummary`, and `retryCount`.
- Expose operator actions through the Administration workspace and `/api/operations/jobs`.

### Queues

- Surface queue depth and queue state for document processing, review, OCR, AI, reindex, embedding, and publishing.
- Preserve current business workflow ownership.
- Add operational pause or resume, retry, cancel, and prioritize controls where current workflow code can honor them safely.

### Schedulers

- Use one persisted scheduler registry instead of hard-coded isolated timers.
- Allow enable, disable, run-now, next execution, last execution, and history visibility.
- Dispatch scheduler work into the same `OperationsJob` framework so operational actions stay auditable and observable.

### Workers And Retry Services

- `OperationsAsyncConfig` provides the dedicated operator background executor.
- `OperationsService` performs runtime dispatch and queue-aware execution.
- Existing `DocumentIntakeProcessingService`, `EmbeddingIndexService`, and `ReviewQueueService` remain the source of truth for domain-specific work.

### Cache

- Cache management remains abstraction-first.
- Administration exposes cache controls without leaking implementation-specific internals into the UI contract.

### Diagnostics And Recovery

- Health checks explain `HEALTHY`, `WARNING`, and `CRITICAL` states per subsystem.
- Diagnostics report active workers, queue depth, failed jobs, bottlenecks, configuration validation, and dependency validation.
- Recovery paths favor retry, targeted rebuild, or full rebuild from canonical customer knowledge artifacts rather than direct data mutation.

## Extension Points

- Add more scheduler types by inserting `operations_scheduler` rows and extending scheduler dispatch mapping.
- Add more background job types by extending `OperationsService.runJob`.
- Add deeper cache implementations behind `OperationsCacheService` without changing the Administration UI contract.
- Add metrics exporters later without replacing the local metrics persistence introduced in this phase.

## Deliberate Boundaries

- PostgreSQL and `pgvector` remain the active persistence and retrieval platform.
- No OpenSearch is introduced.
- No Kubernetes-specific operational code is introduced.
- No telemetry dashboards, alerting, distributed tracing, or load-testing infrastructure is added in this phase.
