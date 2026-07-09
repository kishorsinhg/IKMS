<!--
Sync Impact Report
Version change: 1.0.0 -> 1.1.0
Modified principles: VI. Testable SDS Delivery expanded with code review gate
Added sections: Code review gates and review artifact requirements
Removed sections: None
Templates requiring updates:
- .specify/templates/plan-template.md: updated with IKMS constitution gate guidance
- .specify/templates/spec-template.md: updated with IKMS scope and governance guidance
- .specify/templates/tasks-template.md: updated with IKMS foundational task guidance
Follow-up TODOs: None
-->
# IKMS Constitution

## Core Principles

### I. Client-Centric V1 Scope
IKMS V1 MUST be built as an insurance broker knowledge system centered on one fixed master profile: Client. The system MUST NOT introduce generic master entity builders, multi-master entity configuration, or full policy/claim/contact management in V1. Policy and claim values MAY be captured as searchable metadata, but the broker's existing system remains the source of truth.

Rationale: A fixed Client-centered model keeps V1 simple enough for small brokers and avoids turning the product into a generic platform before the first usable release.

### II. Monolithic Single-Tenant Simplicity
IKMS V1 MUST use a simple monolithic architecture: Spring Boot backend, React frontend, PostgreSQL with pgvector, and controlled file storage. Background work MAY run under a separate worker profile, but it MUST remain in the same backend codebase. V1 MUST target single-tenant on-premise or private-cloud deployment and MUST NOT implement multi-tenant SaaS boundaries.

Rationale: Small brokers need a system that is practical to deploy, operate, back up, and troubleshoot.

### III. Evidence Before Answer
AI features MUST be evidence-bound. Client-level AI Q&A MUST retrieve only authorized information for the selected Client, cite supporting sources, surface conflicting evidence when detected, and return a no-evidence response when supporting evidence is unavailable. AI MUST refuse claim approval, underwriting, cancellation, fraud determination, and other binding business decision requests.

Rationale: IKMS is a retrieval and summarization aid. Humans decide; AI assists.

### IV. Security And PII Before Retrieval
Authorization, role checks, and PII filtering MUST run before search retrieval, document preview, document download, embedding context assembly, and LLM calls. Processor users MUST receive masked metadata and redacted previews/downloads only. Supervisor users MAY access PII and originals when explicitly permitted. If redaction fails or confidence is unacceptable, Processor access to original content MUST be blocked.

Rationale: Preventing exposure before retrieval is safer than trying to clean up leaked context later.

### V. Preserve Originals And Audit Sensitive Activity
Original uploaded, scanned, and attached files MUST be preserved unchanged. New file content MUST be modeled as versions rather than destructive replacement. The system MUST audit login, failed login, intake, review, metadata edits, duplicate detection, document access, PII access, AI activity, configuration changes, retention actions, and exports.

Rationale: Insurance knowledge work needs evidence integrity, traceability, and defensible governance.

### VI. Testable SDS Delivery
Every implementation slice MUST be traceable to a requirement, user story, or governance rule. Tests MUST cover security/PII behavior, audit behavior, AI guardrails, document intake/versioning, and the independent acceptance path for each user story. Each completed slice MUST pass code review before the next dependent slice starts. Tasks and artifacts SHOULD remain concise and linked instead of duplicating large context.

Rationale: The project is intended for SDS-based Codex development, where focused artifacts reduce cost and improve implementation quality.

## V1 Scope Boundaries

V1 includes:

- Client profile, documents, emails, notes, client-level search, client-level AI Q&A, audit, and administration.
- Manual upload, shared folder polling, and IMAP intake for supported content.
- PDF, DOCX, email body, and supported email attachments.
- English UI and English/German content processing.
- Local username/password authentication.

V1 excludes:

- Multi-tenant SaaS.
- Generic industry/entity configuration.
- Operational policy, claim, underwriting, fraud, or decisioning workflows.
- Global cross-client AI Q&A.
- Offline/local LLM fallback unless separately approved in a future constitution amendment.

## Security And AI Governance

- Security trimming MUST happen before retrieval and before LLM context assembly.
- PII metadata flags MUST be administrator configurable.
- Original file access MUST be permissioned and audited.
- Prompt injection risk MUST be detected during ingestion or retrieval and must not influence AI instructions.
- No-evidence, refusal, conflict, and citation behavior MUST be first-class answer states.
- Retention, deletion, and anonymization workflows MUST account for legal hold and audit traceability.

## Development Workflow

- Spec Kit feature artifacts in `specs/` are authoritative for feature implementation.
- `docs/` artifacts provide governance, summaries, indexes, and handoff context.
- Implementation starts only after spec, plan, tasks, and constitution checks are aligned.
- Each task MUST include an explicit file path and clear outcome.
- Code review MUST be performed after Foundation, after Pre-Implementation Hardening, after each user story slice, and before release/demo readiness.
- Code review findings MUST be recorded using `docs/templates/code-review-template.md` or a completed review file under `docs/09-testing/reviews/`.
- Each completed slice SHOULD update handoff, worklog, and changelog artifacts.

## Governance

This constitution supersedes conflicting project notes, brainstorming files, or placeholder documents. If a conflict exists, update the lower-authority artifact or request an explicit constitution amendment.

Amendment process:

1. State the proposed change and rationale.
2. Identify affected specs, plans, tasks, templates, and docs.
3. Update this constitution and include a Sync Impact Report.
4. Re-run pre-implementation analysis before implementation resumes.

Versioning policy:

- MAJOR: Principle removals, scope boundary changes, or governance changes that affect existing implementation direction.
- MINOR: New principles, new mandated sections, or material expansion of governance rules.
- PATCH: Clarifications, wording, or typo fixes that do not change meaning.

Compliance review is required before starting a feature implementation and before accepting a completed feature slice. Code review is required before a completed slice can be treated as accepted.

**Version**: 1.1.0 | **Ratified**: 2026-07-09 | **Last Amended**: 2026-07-09
