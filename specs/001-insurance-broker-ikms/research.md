# Research: Insurance Broker IKMS V1

## Decision: Use A Spring Boot Monolith With API And Worker Profiles

**Rationale**: The broker deployment is small, single-tenant, and needs simple operations. One Spring Boot codebase can expose user APIs and run background intake/processing jobs using separate runtime profiles when needed.

**Alternatives considered**: Microservices were rejected as unnecessary operational complexity. A pure desktop app was rejected because browser-based access is more practical for 20-25 users.

## Decision: Use PostgreSQL With pgvector

**Rationale**: PostgreSQL satisfies transactional needs for clients, documents, metadata, roles, audit, and review queues. pgvector keeps semantic search in the same operational database for V1.

**Alternatives considered**: Separate vector databases were rejected for V1 because they add deployment and backup complexity. Plain keyword search alone was rejected because RAG search is required.

## Decision: Store Files Outside The Database

**Rationale**: Original and redacted files should be immutable and easy to back up. Database records store storage pointers, hashes, version state, extraction state, and audit references.

**Alternatives considered**: Storing files as database blobs was rejected because large document storage makes database backup, restore, and migration heavier for small deployments.

## Decision: Use Fixed Client Model Plus Controlled Extension Fields

**Rationale**: V1 is insurance-broker specific and should avoid generic entity builders. Fixed fields handle common broker needs; labeled extension fields provide limited flexibility without dynamic schema complexity.

**Alternatives considered**: Fully configurable master/related entities were rejected for V1 because they increase design, security, UI, testing, and migration complexity.

## Decision: Treat Policy And Claim Details As Metadata

**Rationale**: The broker's existing system remains source of truth. IKMS captures searchable policy number, claim number, carrier, product, dates, amount, and reference values as evidence metadata.

**Alternatives considered**: Managed Policy and Claim tables were rejected for V1 because they would turn IKMS into an operational policy/claim system.

## Decision: Preserve Originals And Model New Files As Versions

**Rationale**: Compliance and audit need original document preservation. Exact duplicate hashes are skipped or blocked; different hashes can become explicit new versions.

**Alternatives considered**: Replacing original files was rejected because it weakens auditability and evidence traceability.

## Decision: Processor Access Uses Redacted Variants

**Rationale**: Processor users need to work with knowledge but cannot see PII. Redacted previews/downloads plus masked metadata preserve usefulness while meeting access rules.

**Alternatives considered**: Hiding whole documents from Processor was rejected because it would reduce operational value. Giving Processor original files was rejected due to PII risk.

## Decision: Client-Level RAG Only

**Rationale**: Client-scoped retrieval reduces accidental cross-client disclosure and matches the central user workflow. Security filters run before retrieval and LLM context assembly.

**Alternatives considered**: Global cross-client Q&A was rejected for V1 because it increases privacy, authorization, and answer-boundary risk.

## Decision: Configurable AI Provider With Mistral First

**Rationale**: The user wants Mistral Cloud testing and configurable LLM/OCR providers. Provider abstraction keeps future model changes contained.

**Alternatives considered**: Hard-coding Mistral was rejected because the end state requires provider configurability. Offline fallback was rejected for V1.

## Decision: English UI With English/German Content Processing

**Rationale**: Keeping the UI English reduces V1 scope. Search, OCR, embeddings, and answer generation must still handle English and German broker documents.

**Alternatives considered**: Full multilingual UI was rejected for V1 because it adds translation, QA, and support overhead.
