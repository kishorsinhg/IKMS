# Enterprise Knowledge Quality Platform

## Purpose

Phase 8 adds a customer-centric Knowledge Quality Management layer on top of the existing document-processing, retrieval, timeline, and related-knowledge architecture.

IKMS continues to manage customer knowledge only.

Policy Number, Claim Number, Broker Reference, Insurer, Effective Date, Expiry Date, Renewal Date, and similar insurance values remain structured Business Reference Fields. They are searchable and quality-scored metadata, not Policy or Claim entities.

The broker management system remains the system of record.

## Lifecycle

```text
Customer Knowledge
        │
        ▼
Quality Evaluation
        │
        ▼
Issue Detection
        │
        ▼
Knowledge Score
        │
        ▼
Steward Review
        │
        ▼
Correction
        │
        ▼
Revalidation
        │
        ▼
Knowledge Republished
        │
        ▼
Timeline Updated
        │
        ▼
Retrieval Updated
```

## Current Implementation

The current implementation introduces two rebuildable stewardship projections:

- `knowledge_quality_snapshot`
- `knowledge_quality_issue`

These projections summarize customer-level knowledge quality without becoming canonical sources of record. Canonical knowledge remains:

- documents
- document versions
- emails
- notes
- review records
- extracted fields
- metadata values
- retrieval projections

## Scoring Model

Each customer receives an explainable overall score derived from the following dimensions:

- metadata completeness
- Business Reference quality
- customer linkage
- duplicate confidence
- timeline quality
- version quality
- retrieval readiness
- AI quality

Scores are deterministic and explainable. The implementation does not rely on opaque AI-only scoring.

## Stewardship Flow

The stewardship flow is intentionally corrective rather than operational:

1. Evaluate customer knowledge.
2. Persist snapshot and open issues.
3. Present open issues in the Knowledge Quality workspace.
4. Allow a steward to trigger controlled correction, revalidation, or reindex actions.
5. Recompute the quality snapshot.
6. Republish retrieval-ready artifacts through the existing publishing and indexing pipeline where needed.

## API Surface

Phase 8 adds:

- `GET /api/knowledge-quality/customers`
- `GET /api/knowledge-quality/customer/{clientId}`
- `GET /api/knowledge-quality/issues`
- `POST /api/knowledge-quality/revalidate`
- `POST /api/knowledge-quality/reindex`
- `POST /api/knowledge-quality/bulk-correct`

These APIs remain customer-centric and do not introduce Policy or Claim services or routes.

## Authorization And Audit

- Access is restricted to users with `MANAGE_CONFIGURATION`.
- Bulk correction, revalidation, and reindex actions are audited.
- Steward actions do not bypass review, PII, or restricted-document controls.
- Restricted customer knowledge must never leak through quality explanations or issue summaries.

## Extension Points

Phase 8 establishes extension points for future work without changing the current boundary:

- richer recommendation categorization
- calibrated duplicate-confidence models
- background quality sweeps
- stewardship SLA metrics
- final operational dashboards and alerting
