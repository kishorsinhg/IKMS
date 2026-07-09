# IKMS Artifact Workspace

This folder contains the working artifacts for the Insurance Broker Knowledge Management System V1.

The artifact set is intentionally modular. Each file should stay focused and reasonably small so Codex can load only the relevant context for a task instead of carrying the whole project history.

## Navigation

- `00-governance/` - project principles, scope control, quality gates
- `01-requirements/` - business requirements, SRS, acceptance criteria
- `02-sds/` - software design specification and feature design slices
- `03-architecture/` - architecture decisions, deployment, component model
- `04-data/` - data model, migrations, retention model
- `05-api/` - API contracts and integration notes
- `06-ui/` - UI flows, screens, roles, accessibility notes
- `07-ai-rag/` - OCR, classification, metadata extraction, embeddings, RAG
- `08-security/` - security, audit, PII, guardrails, threat controls
- `09-testing/` - test strategy, test cases, traceability
- `10-operations/` - deployment, backup, monitoring, runbooks
- `11-handoff/` - AI handoff notes for future Codex sessions
- `12-decisions/` - ADRs and decision log
- `13-worklog/` - chronological work notes
- `14-release/` - changelog, release notes, version checklist
- `templates/` - reusable artifact templates

## Context Budget Rules

- Prefer one topic per file.
- Keep each artifact concise and link to related files instead of duplicating large sections.
- Record decisions in `12-decisions/decision-log.md`; do not bury decisions in long prose.
- Put task-specific context for future agents in `11-handoff/ai-handoff.md`.
- Keep test cases traceable to requirement IDs.

