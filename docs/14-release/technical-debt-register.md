# Technical Debt Register

Date: 2026-07-15

| ID | Description | Impact | Priority | Recommended Solution | Estimated Effort |
| --- | --- | --- | --- | --- | --- |
| TD-001 | `EnterpriseAiOrchestrationService` remains large and handles multiple responsibilities. | Harder change safety and test isolation. | High | Split into retrieval, completion, persistence, and response-assembly collaborators. | 3-5 days |
| TD-002 | `OperationsService` owns queue, scheduler, rebuild, diagnostics, and job execution logic in one class. | Operational change risk and limited reuse. | High | Introduce command handlers per job family and separate read-model assembly. | 3-5 days |
| TD-003 | Observability is now standardized, but trace data is still split across logs, audit entries, metrics, and persisted AI traces. | Operator investigation requires multiple surfaces. | Medium | Add one internal observability facade and optional trace-summary query surface. | 2-4 days |
| TD-004 | Alert definitions are static metadata only; no trigger evaluation exists yet. | Operator awareness still depends on manual inspection. | Medium | Add internal alert evaluation and state tracking without coupling to a notification vendor. | 2-4 days |
| TD-005 | Several workflow status models are intentionally separate but semantically overlapping. | Increases UI mapping and support complexity. | Medium | Introduce a shared workflow-state vocabulary or mapping layer. | 2-3 days |
| TD-006 | Full frontend suite still has a known `PiiVisibility` failure outside the Phase 11 surface. | Release validation remains partially targeted instead of fully clean. | High | Fix the existing Customer360 test path and restore full-suite green status. | 0.5-1 day |
| TD-007 | Retrieval quality and evaluation fixtures remain smaller than production-scale enterprise corpora. | Production readiness confidence is lower for multilingual and noisy OCR extremes. | Medium | Expand gold-label datasets and degraded-mode fixtures. | 3-6 days |
| TD-008 | File storage remains local-filesystem oriented in the repository. | Production deployment requires environment-specific durability controls. | Medium | Keep the abstraction, add hardened storage adapters and documented durability checks. | 2-5 days |
| TD-009 | Package-level architectural constraints are documented but not enforced automatically. | Future erosion risk. | Medium | Add architecture tests for dependency direction and forbidden package access. | 1-2 days |
| TD-010 | Alerting, dashboards, and distributed tracing are deliberately absent. | Operator observability maturity stops at instrumentation. | Low for RC, High post-RC | Add vendor-neutral state evaluation first, then selective platform integration later. | 3-6 days |
