# Search And AI Q&A Code Review

## Review Metadata

- Slice: User Story 3 search and AI Q&A
- Reviewer: Codex
- Date: 2026-07-10
- Related tasks: T080-T093
- Related user story: US3 - Search And Ask Questions About One Client
- Related requirements: FR-021, FR-022, FR-023, FR-026, FR-027
- Related tests: `backend/src/test/java/com/ikms/search/ClientSearchTest.java`, `backend/src/test/java/com/ikms/ai/ClientQuestionAnsweringTest.java`, `frontend/src/features/search/ClientSearchAsk.test.tsx`
- Outcome: Approved with follow-up tasks

## Scope Reviewed

- Files changed: search/AI entities, contracts, retrieval services, answer service, feedback endpoint, frontend panel
- APIs changed: `GET /api/clients/{clientId}/search`, `POST /api/clients/{clientId}/ask`, `POST /api/ai-interactions/{interactionId}/feedback`
- Data model or migration changes: added `embedding_chunk` and `ai_interaction`
- UI changes: client-scoped search panel and AI Q&A interaction inside client profile; search landing page
- Configuration changes: none

## Constitution Gates

- Client-centric V1 scope respected: Yes
- Monolithic single-tenant simplicity preserved: Yes
- Evidence-before-answer preserved: Yes
- Security/PII before retrieval enforced: Yes through trim helpers
- Original files preserved: Yes
- Audit coverage present: Yes
- Tests trace to requirements: Yes

## Functional Review

- Acceptance scenarios covered: search within a client, answer with citations, no-evidence state, refusal for prohibited decisions, feedback capture
- Edge cases covered: blank/unsupported question validation, prohibited claim decision prompts
- Error handling reviewed: validation and illegal-argument paths route through shared API error handling
- Role behavior reviewed: processor answers/search stay trimmed through existing PII trim logic

## Security, PII, Audit, And AI Review

- Authentication and authorization: existing permissions reused for search and ask
- PII masking/redaction: search excerpts and AI context are trimmed for non-PII roles
- Original file access: unchanged
- Security trimming before retrieval: enforced through `SecurityTrimService`
- Audit events: client AI question events are audited
- AI guardrails and citations: no-evidence and refusal states are first-class, citations attached on answered state
- Prompt injection handling: not specialized yet
- Retention/deletion/anonymization: unchanged

## Test Evidence

- Unit tests: question-answering guardrails and status outcomes
- Integration tests: Spring context and Docker-backed smoke path still pass
- Contract tests: search and ask API shapes
- UI tests: client-scoped search and Q&A panel
- Quickstart or manual validation: `mvn test`, `npm test -- --run`, `npm run build`

## Findings

| ID | Severity | Area | Finding | Required Action |
| --- | --- | --- | --- | --- |
| CR-001 | Low | Retrieval quality | Retrieval is keyword-driven with placeholder chunk indexing rather than real vector similarity. | Replace placeholder embedding/index pipeline with pgvector-backed retrieval when deeper relevance is required. |
| CR-002 | Low | Answer synthesis | Current answer generation is rule-based evidence summarization, not provider-backed LLM reasoning. | Integrate configured AI provider once administration settings are available. |

## Decision

Review decision:
Approved with follow-up tasks.

Follow-up tasks:
- Upgrade placeholder chunk indexing to real embeddings and vector retrieval.
- Replace rule-based answer synthesis with provider-backed, cited generation.

Reviewer notes:
The slice is usable for V1 workflow validation now: search is client-scoped, answers are evidence-backed, prohibited decisions are refused, and feedback is captured for later model improvement.
