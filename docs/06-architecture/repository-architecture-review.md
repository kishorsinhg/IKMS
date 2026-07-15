# Repository Architecture Review

Date: 2026-07-15  
Reviewer: Codex  
Scope: Phase 11 enterprise platform readiness review

## Executive Summary

The repository remains structurally sound for a v1.0 release candidate. The backend is still a modular Spring Boot monolith with clear package-level boundaries, and the frontend remains organized by shared app shell plus workspace-level feature folders. The highest-value low-risk improvements in this phase were the addition of a dedicated observability package, standardized request-context propagation, and a centralized operations alert-definition catalog.

## Current Structure Assessment

### Backend

- `com.ikms.client`, `document`, `review`, `search`, `ai`, `operations`, `governance`, `security`, and `quality` remain coherent domain-oriented packages.
- `com.ikms.common.api` remains an appropriate home for shared API error behavior.
- New cross-cutting observability concerns now live in `com.ikms.observability` instead of being embedded ad hoc in controllers or services.
- Dependency direction is acceptable: controllers depend on services, services depend on repositories and support services, and cross-cutting helpers remain framework-light.

### Frontend

- `src/app` continues to own the shell, theme, shared components, and global behavior.
- `src/features` remains the correct home for workspace-specific screens.
- `src/api` is the right location for transport-level concerns, including the new correlation/request header generation.

## Low-Risk Improvements Applied

- Added `com.ikms.observability` for request-context management, filter-based header handling, MDC propagation, and async task propagation.
- Standardized operations alert definitions under `com.ikms.operations` rather than scattering alert metadata into docs only.
- Fixed stale SDS references from `ui-flow-map.md` to `04-ui-flow-map.md`.
- Exposed request and correlation headers consistently through CORS and API client behavior.

## Architectural Strengths

- Customer remains the primary aggregate and cross-workspace context.
- Business workflows still converge through shared services rather than route-local duplication.
- Operations, governance, and quality remain modular extensions rather than hard-coded per workspace.
- Search, retrieval, AI orchestration, and document processing are separated well enough for a monolith at this size.

## Architectural Smells Observed

- Some services are large and multi-responsibility, especially `EnterpriseAiOrchestrationService`, `ClientKnowledgeService`, and `OperationsService`.
- Several controller packages still own request-scope concerns directly instead of a shared decorator or aspect model.
- Review, processing, and operations flows expose similar lifecycle concepts but do not yet share a common workflow-state abstraction.
- Operational metrics, audit metadata, and persisted traces remain related but are modeled separately.

## Deferred Refactoring Opportunities

1. Split large orchestration and operations services into smaller command-oriented collaborators.
2. Introduce a shared workflow-lifecycle abstraction for processing jobs, operations jobs, and review work items.
3. Consolidate trace, metric, and audit enrichment into one internal observability facade.
4. Introduce package-level architecture tests to enforce dependency direction and prevent future erosion.

## Conclusion

The repository is acceptable for v1.0 RC without a redesign. The remaining issues are mostly scale and maintainability concerns, not release blockers.
