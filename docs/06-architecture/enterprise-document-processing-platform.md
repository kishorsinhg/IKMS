# Enterprise Document Processing Platform

## Scope

Phase 7 formalizes the customer-centric document processing pipeline that turns raw intake artifacts into reviewable knowledge and, after human approval, published customer knowledge.

IKMS remains a knowledge and document intelligence platform:

- Customer is the primary business context.
- Policy Number, Claim Number, Insurer, Broker Reference, Effective Date, Expiry Date, and Renewal Date remain Business Reference Fields.
- These fields are searchable metadata only.
- IKMS does not introduce Policy or Claim entities, lifecycle workflows, or standalone workspaces.

## Processing Stages

1. `INTAKE_RECEIVED`
   - Original file is accepted and preserved.
   - Duplicate detection runs before expensive downstream work where possible.
2. `VIRUS_SCANNED`
   - Provider-agnostic malware scan hook.
   - Failure routes the item to review or marks the job failed, depending on severity.
3. `EXTRACTING` and `OCR_COMPLETE`
   - Text extraction and OCR remain provider-abstracted.
   - Output carries provider identity, language, and OCR confidence.
4. `CLASSIFIED`
   - Document classification returns predicted type, confidence, and provider lineage.
5. `VALIDATED`
   - Metadata extraction, Business Reference extraction, and rule validation produce reviewer-visible fields and findings.
6. `WAITING_REVIEW`
   - Low-confidence, failed, ambiguous, or policy-boundary-sensitive cases pause for human review.
7. `APPROVED`, `PUBLISHED`, `INDEXED`
   - Reviewer-approved values become the authoritative IKMS knowledge projection.
   - Publication refreshes retrieval projections, related knowledge, and customer timeline state asynchronously.

## Processing Job Model

`DocumentProcessingJob` is the operational lifecycle projection for one document-processing run.

It records:

- current status and stage
- retry count
- stage-specific confidence values
- provider lineage
- reviewer comment
- error code/message
- extracted fields
- validation findings
- key timestamps for review, approval, rejection, publishing, and completion

This projection is rebuildable from canonical knowledge artifacts plus review history when needed. It is not the source of truth for customer knowledge itself.

## Reviewer-Centric Outputs

Two reviewer-visible projections are persisted:

- `DocumentProcessingField`
  - extracted value
  - corrected value
  - approved value
  - source page
  - extraction method
  - validation state
  - confidence
- `DocumentProcessingFinding`
  - finding code
  - severity
  - stage
  - evidence text
  - confidence
  - resolution state/comment

These outputs let Review surfaces show why a value needs attention without fabricating AI explanations.

## Retry And Failure Handling

- Retry is allowed only for document-backed review items with an associated processing job.
- Retry increments the job counter, resets the job to an active processing state, and reruns the modular pipeline.
- Hard failures preserve the last error code/message and route the review item to `PROCESSING_FAILED`.
- Soft failures and low-confidence outputs route the item to `WAITING_REVIEW`.

## Approval And Publishing

Human review remains authoritative.

- Reviewer corrections update processing-field traceability.
- Approval marks the job approved, applies approved values, and triggers publication.
- Publication refreshes:
  - document version state
  - retrieval projection
  - customer timeline
  - related knowledge

## Audit And Metrics Expectations

Audit coverage includes:

- intake processing started
- metadata corrected
- retry requested
- review approved
- review rejected
- publish completed

Metrics captured for later operations work include:

- OCR confidence
- classification confidence
- extraction confidence
- validation confidence
- duplicate confidence
- retry count
- review outcomes

Operational dashboards remain deferred to the later hardening phase.
