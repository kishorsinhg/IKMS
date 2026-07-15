# Operational Runbooks

Date: 2026-07-15

## Failed OCR

Symptoms: OCR retry jobs fail, scanned PDFs remain unsearchable, review queue grows with low-confidence extraction.

Probable causes: OCR provider outage, invalid OCR model configuration, storage read failure, malformed source file.

Diagnostics: Check `/api/operations/jobs`, `/api/operations/queues`, OCR retry history, document processing job details, and trace IDs in audit records.

Recovery steps: Validate OCR provider settings, retry the affected document, confirm source-file access, then reprocess and verify page-aware extraction output.

Escalation guidance: Escalate to platform operations if provider recovery is needed; escalate to engineering if failures persist after configuration and storage checks.

## Failed AI Request

Symptoms: AI interactions return fallback, insufficient evidence, or repeated failure warnings.

Probable causes: unapproved model configuration, provider timeout, AI provider outage, prompt blocked by governance or guardrails.

Diagnostics: Inspect AI interaction audit records, request and correlation IDs, provider validation results, and operations AI retry history.

Recovery steps: Validate provider settings, confirm approved model registry alignment, retry through operations if appropriate, and verify grounded fallback behavior.

Escalation guidance: Escalate to engineering when failures persist beyond provider validation or approved-model correction.

## Failed Retrieval

Symptoms: retrieval warnings increase, vector fallback frequency rises, answers degrade to keyword-only context.

Probable causes: stale embeddings, pgvector issues, incomplete publishing, degraded similarity retrieval.

Diagnostics: Review retrieval warnings, recent reindex or embedding jobs, knowledge-quality readiness, and related audit correlations.

Recovery steps: run targeted reindex or embedding rebuild, verify current document versions are indexed, then re-run representative search and ask workflows.

Escalation guidance: Escalate to platform operations first; escalate to engineering if repeated rebuilds do not restore hybrid retrieval quality.

## Queue Backlog

Symptoms: queue depth remains elevated, operators see delayed retries or approvals, scheduler output lags.

Probable causes: paused queue, repeated failures, worker saturation, dependency outage.

Diagnostics: Check queue state, recent job failures, paused flags, scheduler history, and worker counts in diagnostics.

Recovery steps: Resume paused queues where safe, prioritize urgent items, clear repeated invalid retries, and stabilize the failing dependency before bulk replay.

Escalation guidance: Escalate to platform operations when backlog exceeds agreed operational thresholds.

## Scheduler Failure

Symptoms: scheduled rebuilds or maintenance jobs stop running or miss expected windows.

Probable causes: scheduler disabled, repeated job failure, application restart gap, downstream dependency failure.

Diagnostics: Review scheduler status, execution history, recent failures, and queue pause state.

Recovery steps: Re-enable the scheduler if intentionally disabled, run the schedule manually, confirm downstream queues are healthy, and inspect failed job details.

Escalation guidance: Escalate to platform operations; involve engineering for repeated scheduler logic failures.

## Reindex

Symptoms: search or AI answers miss recent approved knowledge, retrieval warnings reference stale projections.

Probable causes: publish hook missed, rebuild not run, queue paused, prior job failed.

Diagnostics: Inspect operations jobs, queue state, document version currency, and knowledge-quality retrieval readiness.

Recovery steps: Run targeted customer or document reindex first, use full rebuild only when scope is unclear, then validate search and ask behavior.

Escalation guidance: Escalate to engineering if reindex completes successfully but stale behavior persists.

## Embedding Rebuild

Symptoms: vector retrieval degrades while lexical results still appear, embedding jobs fail, similarity links weaken.

Probable causes: embedding provider issue, model mismatch, stale vector projections, rebuild interruption.

Diagnostics: Check embedding rebuild jobs, provider validation, and document-version indexing coverage.

Recovery steps: Validate provider and model, rerun targeted rebuilds, then confirm hybrid retrieval and related-knowledge behavior.

Escalation guidance: Escalate to platform operations first; involve engineering for repeatable provider/model defects.

## Knowledge Projection Rebuild

Symptoms: timeline, related knowledge, or search result freshness diverges from approved review output.

Probable causes: publish step failure, rebuild queue pause, stale derived projections.

Diagnostics: Review recent approvals, reindex jobs, and affected customer knowledge summaries.

Recovery steps: Run the appropriate rebuild, verify current versions, and confirm the customer timeline and related knowledge repopulate correctly.

Escalation guidance: Escalate to engineering if projections remain inconsistent after successful rebuild completion.

## Cache Invalidation

Symptoms: administration data, policy configuration, or support views appear stale after changes.

Probable causes: cached reads retained old state, cache refresh not run, operator action targeted the wrong cache.

Diagnostics: Check cache administration history and compare persisted settings with rendered UI/API output.

Recovery steps: Invalidate or refresh the specific cache first; clear only when scoped actions are insufficient.

Escalation guidance: Escalate to platform operations for repeated stale-cache incidents.

## Governance Issue

Symptoms: incorrect classification, retention eligibility mismatch, or legal-hold processing concern.

Probable causes: misconfigured governance settings, stale metadata, improper document classification, workflow interruption.

Diagnostics: Inspect governance configuration, document metadata, audit trails, and retention or hold records.

Recovery steps: Correct the configuration or metadata, re-run the relevant governance workflow, and verify export or access behavior.

Escalation guidance: Escalate to the data-governance owner for classification, retention, or hold disputes.

## Security Incident

Symptoms: unauthorized access attempt, export restriction violation, unexpected PII exposure, or suspicious audit activity.

Probable causes: permission misconfiguration, session misuse, ABAC mismatch, privileged misuse.

Diagnostics: Review audit logs using request and correlation IDs, confirm actor permissions and attributes, and inspect recent governance changes.

Recovery steps: Contain access, disable the affected session or account, confirm classification and ABAC policy state, and preserve audit evidence.

Escalation guidance: Escalate immediately to the security officer and follow organizational incident procedures.

## Permission Misconfiguration

Symptoms: legitimate users lose access or unauthorized users gain access to documents, AI, or operations.

Probable causes: wrong role assignment, stale permission seed, ABAC attribute mismatch, admin misconfiguration.

Diagnostics: Review effective permissions, actor attributes, recent admin changes, and governance decisions.

Recovery steps: Correct the role or attribute configuration, re-test the affected action, and confirm audit traces show the corrected behavior.

Escalation guidance: Escalate to platform operations, then engineering if the permission engine behaves unexpectedly after correction.

## Database Connectivity

Symptoms: startup failure, API errors across multiple modules, job persistence failures, migration failures.

Probable causes: database unavailable, invalid credentials, exhausted connections, network interruption.

Diagnostics: Review application startup logs, health status, datasource configuration, and recent migration status.

Recovery steps: restore connectivity, validate credentials, restart the application if needed, and verify migrations before resuming queued work.

Escalation guidance: Escalate to infrastructure or database operations immediately.

## Storage Failure

Symptoms: preview/download errors, OCR retry failures, document processing failures reading originals.

Probable causes: local storage path unavailable, file permission issue, deleted or moved artifact, disk pressure.

Diagnostics: Inspect storage path configuration, file presence, application logs, and document version metadata.

Recovery steps: restore storage availability, recover the missing file if possible, then retry the affected processing or access action.

Escalation guidance: Escalate to platform operations; involve infrastructure if the underlying storage host is degraded.

## Dependency Failure

Symptoms: provider validation fails, external OCR or AI calls time out, shared-folder or IMAP ingestion degrades.

Probable causes: third-party outage, invalid endpoint, secret drift, TLS or network issue.

Diagnostics: Use provider validation, diagnostics output, recent failure jobs, and correlation-linked audit events.

Recovery steps: validate endpoint and credentials, switch to approved fallback behavior where available, and pause bulk workflows until the dependency is stable.

Escalation guidance: Escalate to platform operations first, then engineering for adapter or retry-policy defects.
