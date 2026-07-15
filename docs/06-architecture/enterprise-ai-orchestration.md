# Enterprise AI Orchestration

## Purpose

This document defines the backend architecture for the enterprise knowledge retrieval and AI orchestration layer that will power:

- Search
- AI Assistant
- Evidence Workspace
- Customer360
- Review Workspace

The design is backend-first and builds on the current Spring Boot monolith, PostgreSQL plus pgvector storage, existing security trimming, current provider-backed extraction and embeddings, and the client-scoped RAG foundation already implemented in `com.ikms.search` and `com.ikms.ai`.

This document should now be read together with `docs/06-architecture/customer-centric-enterprise-retrieval.md`, which narrows the retrieval model so Customer remains the primary knowledge context and Policy/Claim are treated only as structured Business Reference Fields rather than IKMS-managed entities.

## Current Baseline

The repository already provides the following AI and search primitives:

- `ClientSearchService` performs client-scoped hybrid retrieval over documents, emails, notes, metadata, and persisted chunks.
- `RagContextService` and `ClientQuestionAnsweringService` assemble client-level context and call the configured provider for answer synthesis.
- `EmbeddingIndexService` persists semantic chunks with provenance such as page number, section, token count, and retrieval summary.
- `AiProviderClient` supports embeddings, OCR, classification, answer synthesis, and provider validation against the configured external provider.
- `SecurityTrimService`, `ContentSensitivityService`, and `PiiMaskingService` already enforce permission-aware trimming and masking.
- `PromptInjectionDetectionService` already detects tainted content during retrieval and answer preparation.
- `AiInteraction` persists client AI interactions, feedback, and cited-source summaries.

These components are sufficient for client-level Q&A, but they are not yet organized as an enterprise orchestration pipeline. Current limitations:

- Retrieval is centered on client-scoped search rather than a generalized orchestration request.
- Intent handling is implicit and tied to specific endpoints.
- Context construction is specialized for `ask`, not reusable across summarize, explain, compare, extract, or validate operations.
- Provider execution, retries, timeouts, and fallback behavior are handled at a low level but not exposed as orchestration policy.
- Citations are present, but future jump-target metadata is not yet first-class.
- Evaluation, retrieval traces, and latency/grounding metrics are not persisted as orchestration records.

## Design Goals

- Reuse current security, storage, ingestion, and provider infrastructure where possible.
- Keep orchestration inside the monolith for V1 and future on-prem deployment.
- Separate orchestration policy from provider execution.
- Support current client-scoped behavior immediately while allowing future policy-level, claim-level, and cross-document reasoning.
- Preserve clean backend contracts so the existing frontend architecture can integrate without redesign.
- Keep all AI behavior permission-aware, auditable, and grounded in retrieved evidence.

## End-To-End Pipeline

Every enterprise AI request should flow through the same high-level pipeline:

1. Authorization
2. Intent Detection
3. Query Planning
4. Hybrid Retrieval
5. Evidence Ranking
6. Context Building
7. LLM Execution
8. Citation Building
9. Guardrail Validation
10. Response Mapping
11. Audit and Evaluation Recording

### Sequence

```text
Client Request
  -> API Controller
  -> Authorization Gate
  -> Intent Detector
  -> Query Planner
  -> Retrieval Coordinator
      -> lexical retrieval
      -> vector retrieval
      -> metadata retrieval
      -> entity/relationship retrieval
  -> Evidence Ranker
  -> Context Builder
  -> LLM Orchestrator
      -> primary provider/model
      -> retry/timeout policy
      -> fallback provider/model
  -> Citation Builder
  -> Guardrail Validator
  -> Response Mapper
  -> Audit Log + Metrics + Feedback Hooks
```

## Target Package Structure

The new backend work should organize around the following packages:

- `com.ikms.ai.orchestration`
  - orchestration request/response contracts
  - intent detector
  - orchestration service
  - planner
  - orchestration policies
- `com.ikms.ai.context`
  - context builder
  - token budget manager
  - prompt template assembler
  - conversation history assembler
- `com.ikms.ai.provider`
  - provider-agnostic LLM execution contracts
  - retry/timeout/fallback policy
  - streaming hooks
  - local-model adapter contract
- `com.ikms.search`
  - retrieval coordinator
  - lexical/vector/metadata/entity/relationship retrievers
  - evidence ranker
  - retrieval trace mappers
- `com.ikms.ai`
  - citation engine
  - grounding validator
  - hallucination guard
  - prompt injection enforcement
  - feedback capture

Existing classes such as `AiProviderClient`, `ClientSearchService`, `RagContextService`, and `ClientQuestionAnsweringService` should be refactored behind these new contracts rather than duplicated.

## Core Backend Components

### 1. Authorization Gate

Responsibility:

- verify the caller has the operation-level permission
- resolve actor identity and role
- enforce scope constraints such as client access
- determine whether original, redacted, or masked evidence can be used

Reuse:

- `PermissionService`
- `SecurityTrimService`
- `ContentSensitivityService`

New contract:

- `AuthorizationContext`
  - actor user id
  - permissions
  - accessible entity scope
  - pii access mode
  - allowed operations

### 2. Intent Detector

Responsibility:

- classify the request into one orchestration intent
- normalize user wording into execution semantics

Initial supported intents:

- `SEARCH`
- `ASK`
- `SUMMARIZE`
- `EXPLAIN`
- `COMPARE`
- `EXTRACT`
- `VALIDATE`

Design choice:

- V1 should use deterministic rules first.
- Provider-based intent classification can be added later behind the same interface.

Suggested interface:

- `IntentDetectionService.detect(AuthorizationContext, EnterpriseAiRequest)`

### 3. Query Planner

Responsibility:

- convert intent plus scope into a structured retrieval plan
- choose retrieval modes
- choose result depth
- decide whether multiple documents, versions, policy references, or claim references are required

Planner outputs:

- target scope
  - client
  - policy
  - claim
  - review item
  - document/version
- retrieval modes
  - lexical
  - vector
  - metadata
  - entity
  - relationship
- limits
  - max evidence items
  - max chunks per source
  - max tokens for context
- reasoning mode
  - browse
  - answer
  - compare
  - extraction validation

### 4. Hybrid Retrieval Coordinator

Responsibility:

- orchestrate multiple retrievers in one request
- merge document, email, note, policy, claim, metadata, and version signals
- respect permission and PII constraints before evidence becomes model input

Required retrieval strategies:

- lexical retrieval over titles, extracted text, emails, notes
- vector retrieval over persisted chunks
- metadata retrieval over configured field values
- entity retrieval for customer, policy, claim, document, version
- relationship retrieval for linked artifacts such as customer-to-document, policy-to-document, claim-to-email

Required retrieval outputs:

- raw evidence candidates
- retrieval path metadata
- relevance score
- source object identifiers
- provenance details such as page, section, chunk index, occurred-at timestamp
- permission trim status

### 5. Evidence Ranker

Responsibility:

- combine retrieval-mode scores
- deduplicate overlapping evidence
- boost current version when appropriate
- preserve cross-document evidence diversity
- penalize low-confidence or tainted evidence

Ranking factors:

- lexical overlap
- vector similarity
- metadata field relevance
- policy/claim/customer relationship distance
- chunk confidence
- OCR confidence
- citation quality
- document recency
- current-version preference
- prompt-injection taint

### 6. Context Builder

Responsibility:

- construct the final model input in a provider-agnostic way
- allocate token budget across system prompt, user context, evidence, metadata, and conversation history

Context sections:

- system prompt
- authorization and scope context
- user request
- structured intent and planner notes
- retrieved evidence snippets
- business metadata
- prior conversation history
- safety instructions

Design rules:

- evidence snippets must remain source-traceable
- masked evidence must stay masked in context
- tainted evidence must be excluded or clearly labeled according to guardrail policy
- conversation history should be summarized when token limits are exceeded

### 7. LLM Orchestrator

Responsibility:

- execute provider calls through a provider-agnostic interface
- apply retries, timeouts, and fallback policy
- support future streaming responses
- support future local/on-prem models without changing orchestration callers

Execution policy should support:

- primary provider/model
- fallback provider/model
- timeout budget
- retry count
- circuit-breaker style degraded mode
- non-streaming V1 response path
- future streaming event path

Important separation:

- `AiProviderClient` remains the low-level provider adapter.
- `LlmOrchestrator` becomes the policy layer that decides how and when to call providers.

### 8. Citation Engine

Responsibility:

- convert ranked evidence into response citations
- preserve future UI jump-target metadata even if the frontend cannot use it yet

Citation payload should include:

- source type
- source id
- document or email title
- page number
- chunk id or chunk index
- section
- confidence
- excerpt
- retrieval path
- future jump target id

Jump target IDs should be stable application-level identifiers, for example:

- `document:{id}:page:{n}`
- `document-version:{id}:chunk:{index}`
- `email:{id}:section:body`
- `metadata:{ownerType}:{ownerId}:{fieldKey}`

### 9. Guardrail Validator

Responsibility:

- apply post-retrieval and post-generation checks
- reject or degrade unsafe outputs
- log violations

Required guardrails:

- permission enforcement
- PII masking
- prompt injection blocking
- grounding validation
- hallucination mitigation
- token limit enforcement
- refusal for prohibited decision-making

Validation stages:

- before retrieval
- after retrieval before context build
- after LLM output before response

### 10. Response Mapper

Responsibility:

- produce a stable API response contract for each operation
- attach warnings, retrieval mode, citations, confidence, metrics summary, and conversation identifiers

## API Design

Existing contracts should be reused where possible, especially for current client search and ask flows. The enterprise layer should unify operation handling behind a shared internal service even if external endpoints remain separate.

### Existing Endpoints To Preserve

- `GET /api/clients/{clientId}/search`
- `POST /api/clients/{clientId}/ask`
- `POST /api/ai-interactions/{interactionId}/feedback`

### New Endpoint Families

Suggested additions:

- `POST /api/clients/{clientId}/summarize`
- `POST /api/clients/{clientId}/explain`
- `POST /api/clients/{clientId}/compare`
- `POST /api/clients/{clientId}/extract`
- `POST /api/clients/{clientId}/validate`

Optional shared orchestration endpoint for future internal use:

- `POST /api/ai/orchestrate`

This shared endpoint can stay internal or admin-only at first while controllers continue exposing operation-specific routes for frontend compatibility.

### Common Request Shape

All operation handlers should normalize into a shared internal request:

- actor context
- target scope
- operation
- natural language question or instruction
- structured parameters
- conversation id
- selected source ids
- current workspace hints

### Common Response Shape

All operation handlers should normalize from a shared internal response:

- operation result
- response text or structured payload
- citations
- evidence references
- source references
- retrieval mode
- confidence summary
- warnings
- audit interaction id
- metrics summary

## Persistence Changes

Phase 15 will need additional persistence beyond `AiInteraction` and `EmbeddingChunk`.

### New Tables

1. `ai_conversation`
   - conversation scope
   - actor
   - target entity
   - started/updated timestamps

2. `ai_conversation_message`
   - conversation id
   - role
   - content
   - provider metadata
   - token counts

3. `ai_retrieval_trace`
   - request id
   - retrieval mode
   - source identifiers
   - ranking scores
   - permission-trim flags
   - prompt-injection flags

4. `ai_citation_record`
   - request id
   - source metadata
   - page/chunk/section
   - confidence
   - jump target id

5. `ai_orchestration_metric`
   - request id
   - latency
   - retrieval precision proxy
   - grounding score
   - citation coverage
   - provider/fallback info

6. `ai_evaluation_result`
   - request id
   - evaluation type
   - score
   - notes

### Existing Tables To Extend

- `ai_interaction`
  - add operation type
  - add conversation id
  - add provider/model identifiers
  - add latency and token fields
  - add grounding/citation summary fields

## Retrieval Scope Expansion

Current retrieval is primarily client-scoped. Enterprise orchestration must support these scopes, while still respecting the product rule that the client remains the master record:

- customer-level context
- policy-linked context
- claim-linked context
- review-item-linked context
- document-version comparisons
- cross-document reasoning within the same customer

Policy and claim data remain searchable metadata, not authoritative managed records. Retrieval should therefore resolve them through linked documents, emails, metadata values, and demo/reference tables rather than introducing a separate policy/claim transactional domain.

Policy and Claim are external references or metadata within IKMS. The broker management system remains the system of record.

## Evaluation And Metrics

Every orchestration request should capture operational and quality signals.

Required metrics:

- total latency
- provider latency
- retrieval latency
- context-build latency
- token counts
- evidence count
- citation coverage
- grounding score
- fallback occurrence
- warning count

Initial quality proxies:

- retrieval precision at top N
- citation coverage ratio
- grounding validation pass/fail
- answer refusal correctness
- user feedback helpful/not-helpful

## Audit And Security

All orchestration operations must emit audit events with:

- actor
- operation
- scope
- outcome
- provider used
- fallback used
- blocked evidence count
- prompt-injection detection summary
- refusal or validation reason when applicable

Audit should reuse `AuditService` and avoid storing raw secret-bearing provider payloads.

## Deployment And On-Prem Considerations

- Provider execution must remain abstracted so external cloud models can later be replaced by local models.
- Streaming support should be modeled now at the service boundary even if the first implementation returns buffered responses.
- Token budgeting, retries, and timeouts must be configurable through admin AI settings and app settings.
- Retrieval logic must remain database-centric and file-storage-local to preserve on-prem viability.

## Implementation Sequence

The Phase 15 task sequence should follow this order:

1. `T155` finalize this architecture and package design.
2. `T156` create orchestration integration tests and API contract tests first.
3. `T157` add persistence and migration changes.
4. `T158` implement shared orchestration contracts and service skeletons.
5. `T159` refactor retrieval into coordinated retrievers and evidence ranking.
6. `T160` implement reusable context assembly and token budgeting.
7. `T161` add LLM orchestration policy around the current provider client.
8. `T162` finalize citation, guardrail, and audit extensions.
9. `T163` expose enterprise APIs while preserving existing client search/ask behavior.
10. `T164` add metrics, benchmarks, and documentation.

## Risks And Design Constraints

- Do not bypass `SecurityTrimService` during retrieval or context construction.
- Do not allow provider prompts to directly infer permissions; permission enforcement stays server-side.
- Do not create a second independent search stack beside `ClientSearchService`; refactor it into reusable retrieval modules.
- Do not redesign frontend contracts unless integration requires a backend-compatible extension.
- Do not assume cloud-only provider availability; fallback and degraded modes are required.

## Definition Of Done For Phase 15

Phase 15 is complete when:

- all enterprise AI operations flow through a shared orchestration service
- retrieval supports multi-document and scope-aware evidence assembly
- context building is token-budgeted and provider-agnostic
- citations carry page/chunk/section/confidence and jump-target metadata
- guardrails enforce permissions, masking, injection blocking, grounding, and refusals
- orchestration metrics and audit trails are persisted
- existing frontend workspaces can consume the new backend responses without redesign
