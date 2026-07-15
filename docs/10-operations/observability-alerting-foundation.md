# Observability And Alerting Foundation

Date: 2026-07-15

## Scope

This phase adds instrumentation and reusable definitions only.

It does not add:

- Prometheus
- Grafana
- Azure Monitor
- CloudWatch
- OpenTelemetry vendor wiring
- email, Slack, Teams, PagerDuty, or similar notification delivery

## Request Context Model

The platform now standardizes the following identifiers:

- `requestId`
- `correlationId`
- `backgroundJobId`
- `aiInteractionId`
- `processingJobId`
- `reviewId`
- `retrievalId`
- `timelineRequestId`
- `searchRequestId`

## Propagation Design

- HTTP requests enter through `RequestContextFilter`.
- `X-Request-Id` and `X-Correlation-Id` are accepted from callers or generated server-side.
- The request context is copied into SLF4J MDC for log correlation.
- Async operations inherit the same context through `RequestContextTaskDecorator`.
- Audit entries automatically inherit current trace identifiers.
- API error payloads now include `requestId` and `correlationId`.

## Workflow Coverage

- Search endpoints create `searchRequestId`.
- Timeline and related-knowledge endpoints create `timelineRequestId`.
- Enterprise AI orchestration creates `retrievalId`.
- Persisted AI interactions scope `aiInteractionId`.
- Document processing scopes `processingJobId`.
- Operations jobs scope `backgroundJobId`.
- Review actions scope `reviewId`.

## Trace Relationship Model

- A browser or API caller starts with `requestId` and `correlationId`.
- Search and timeline requests fan out from that request context with request-scoped IDs.
- Retrieval and AI orchestration create `retrievalId`, then bind persisted `aiInteractionId`.
- OCR or processing flows bind `processingJobId`.
- Operations retries and rebuilds bind `backgroundJobId`.
- Human review actions bind `reviewId`.

## Alert Definition Framework

Alert definitions are modeled with:

- `alertId`
- `severity`
- `category`
- `threshold`
- `suppressionWindow`
- `escalationLevel`
- `resolutionGuidance`

Supported categories:

- OCR
- AI
- Retrieval
- Processing
- Queue
- Scheduler
- Operations
- Governance
- Security
- Quality

## Implementation Notes

- The alert catalog is exposed through `AlertDefinitionService`.
- The initial catalog intentionally defines operator guidance only.
- Trigger evaluation and notification delivery remain future operational work.
