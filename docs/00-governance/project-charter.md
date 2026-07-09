# Project Charter

## Product

Insurance Broker Knowledge Management System V1.

## Goal

Provide a single-tenant knowledge layer for small insurance brokers so users can store, retrieve, summarize, and ask questions over client-linked documents, emails, and notes.

The broker's existing business system remains the source of truth for policy and claim administration. IKMS is not a policy, claim, underwriting, or decisioning system.

## V1 Principles

1. Client-centric knowledge, not generic entity management.
2. AI assists, humans decide.
3. Evidence before answer.
4. Security trimming before retrieval.
5. Preserve original documents.
6. Keep V1 monolithic and simple.
7. Prefer fixed fields plus controlled extension fields over dynamic schema.
8. Optimize artifacts for focused Codex context usage.

## Out Of Scope For V1

- Multi-tenant SaaS.
- Generic industry configuration.
- Full policy/claim/contact management.
- Global cross-client AI Q&A.
- AI business decisioning.
- Automatic claim approval, underwriting, cancellation, fraud decisions.
- Offline/local LLM fallback.
- Full multilingual UI.

