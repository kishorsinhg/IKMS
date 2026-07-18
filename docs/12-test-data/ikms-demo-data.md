# IKMS Demo Data Dataset

## Purpose

This dataset seeds deterministic synthetic records into the local IKMS PostgreSQL development database through the Spring `demo` profile.

- Namespace: `ikms-demo-2026-07`
- Reference date: `2026-07-18`
- Safety boundary: the runner prints the active Spring profile, database host, and database name before any write and refuses to proceed if the profile, host, or database name appears production-like.
- Reset scope: only deterministic namespace-owned records created by this dataset are removed.

## Schema And UI Mapping

| UI workspace | Backend entities / tables | Seed scenarios |
|---|---|---|
| Customer Access | `client`, `note` | 18 customers, active/inactive/archived, business/individual, long names, list pagination, note density |
| Customer360 Documents | `document`, `document_version`, `metadata_value`, `document_processing_job`, `document_processing_field`, `document_processing_finding` | multi-version documents, restricted documents, OCR/review states, metadata gaps, archived knowledge |
| Customer360 Emails | `email_item` | linked and unlinked emails, restricted-review email, claim and renewal correspondence |
| Customer360 Relationships | derived from `embedding_chunk` / related-knowledge services, not a standalone table | current architecture supports derived related knowledge only; no standalone customer relationship seed table exists |
| Customer360 Timeline | `document`, `document_version`, `email_item`, `note`, `audit_log`, `metadata_value` | timeline density, business references, selected-row context, recent and stale events |
| Client Search Panel | `embedding_chunk`, `document`, `document_version`, `email_item`, `note`, `metadata_value` | predictable customer-scoped search hits for `silverridge`, `acord`, `marine`, `TMP-PROP-90814`, `CLM-SEA-240118` |
| Review Queue | `review_queue_item`, `document_processing_job`, `document_processing_field`, `document_processing_finding`, `document`, `email_item` | open/in-progress/resolved/rejected, unlinked, low confidence, duplicate uncertainty, processing failure |
| Knowledge Quality | `knowledge_quality_snapshot`, `knowledge_quality_issue` | open and resolved issues, missing metadata, conflicting references, linkage gaps, summary freshness |
| Administration | `app_user`, `document_type`, `metadata_field`, `shared_folder_config`, `mailbox_config`, `review_setting`, `ai_provider_setting`, `app_setting`, `operations_*`, `retention_record` | users, config modules, governance settings, operations queues/jobs/metrics, legal holds |
| Audit | `audit_log` | 100 synthetic events across login, search, review, quality, admin, restricted-access attempts |
| Governance / Operations panels | `app_setting`, `retention_record`, `operations_job`, `operations_queue_state`, `operations_scheduler`, `operations_scheduler_execution`, `operations_metric` | legal holds, queue pause state, scheduler history, operational failures |

## Current Architecture Limits

- Global `/search` live mode currently queries the customer list, not the persisted knowledge index.
- Customer-scoped knowledge search is available through the Customer360 search panel and uses the seeded retrieval data.
- Intake queue records are not persisted as a standalone table in the current backend.
- Client import results are request-time responses only; there is no persisted import batch table to seed.
- The authorization model supports `INDEXER`, `PROCESSOR`, `SUPERVISOR`, and `ADMINISTRATOR` roles only.
- Knowledge Quality backend statuses are `OPEN`, `ACKNOWLEDGED`, and `RESOLVED`. The frontend still includes a `DISMISSED` filter option, but that state is not produced by the current backend model.

## Seed Inventory

Expected deterministic demo-owned minimums after `seed`:

| Record type | Count |
|---|---:|
| Users | 6 |
| Customers | 18 |
| Notes | 25 |
| Emails | 25 |
| Documents | 80 |
| Document versions | 108 |
| Document processing jobs | 80 |
| Review items | 20 |
| Knowledge quality snapshots | 18 |
| Knowledge quality issues | 15 |
| Retention records | 8 |
| Operations jobs | 12 |
| Operations metrics | 4 |
| Audit events | 100 |

## Flagship Scenarios

| Customer | Seed focus |
|---|---|
| Harborview Marine Logistics LLC | marine renewal pack, claim reference `CLM-SEA-240118`, multiple evidence sources, high-severity quality conflict |
| Silver Ridge Property Management Holdings and Tenant Stewardship Consortium | ACORD-heavy records, unresolved location metadata, mixed review states, long-name layout coverage |
| North Valley Dental Group PC | cleaner records, low-risk profile, resolved quality issue, recent completeness |

## Personas

The local authentication model already supports fixed username/password login. The demo dataset adds extra synthetic users without changing the base auth model.

| Persona | Username | Role | Expected access |
|---|---|---|---|
| Administrator | `demo-admin` | `ADMINISTRATOR` | Administration, Governance, Operations, Audit |
| Supervisor | `demo-supervisor` | `SUPERVISOR` | Customer access, search, audit, restricted content visibility |
| Reviewer / Processor | `demo-processor` | `PROCESSOR` | Customer access, client-scoped search, redacted knowledge |
| Restricted operator | `demo-indexer` | `INDEXER` | Intake/review-adjacent restricted experience |
| Locked user | `demo-locked` | `PROCESSOR` | Account-status verification |
| Disabled user | `demo-disabled` | `PROCESSOR` | Account-status verification |

Default local password remains `ChangeMe123!` through the existing local bootstrap mechanism.

## Search Verification Matrix

These queries are intended for the customer-scoped knowledge search inside Customer360.

| Query | Expected result pattern |
|---|---|
| `silverridge` | Silver Ridge document, email, note, metadata, and review-context hits |
| `acord` | ACORD 125 and related ACORD-form documents with mixed confidence and version states |
| `marine` | Harborview marine renewal and claim-reference evidence across document/email/note sources |
| `TMP-PROP-90814` | exact policy-reference and related renewal evidence |
| `CLM-SEA-240118` | claim-reference support evidence |
| `unlikely-phrase-zz-20418` | zero results |

## Manual Screen Verification Matrix

| Scenario | User | Seed record / query | Expected result |
|---|---|---|---|
| Login active admin | `demo-admin` | username/password login | authenticated admin shell |
| Login locked account | `demo-locked` | username/password login | lock/denial flow |
| Customer list pagination | `demo-supervisor` | Customer Access | page size 10 with multi-page list |
| Long-name truncation | `demo-supervisor` | Silver Ridge customer row | responsive truncation and drawer context |
| Customer360 marine evidence | `demo-supervisor` | Harborview Marine Logistics LLC | dense documents, emails, notes, timeline |
| Customer360 clean record | `demo-processor` | North Valley Dental Group PC | mostly complete record with fewer open issues |
| Customer-scoped search mixed hits | `demo-supervisor` | `marine` | documents, emails, notes, metadata-backed hits |
| Customer-scoped search zero state | `demo-processor` | `unlikely-phrase-zz-20418` | no-results state |
| Review low confidence | `demo-indexer` or `demo-supervisor` | first open review item | metadata correction / evidence review state |
| Review resolved history | `demo-supervisor` | resolved / rejected queue filters | non-open queue states render |
| Knowledge quality conflict | `demo-admin` | Harborview conflicting policy issue | high-severity open issue with remediation action |
| Knowledge quality resolved item | `demo-admin` | North Valley resolved summary freshness issue | resolved issue visible in customer detail |
| Administration config coverage | `demo-admin` | Administration modules | users, document types, metadata, folders, mailboxes, AI, queues, health |
| Audit filtering | `demo-supervisor` or `demo-admin` | actor/date/action filters | filtered audit grid and export affordance |
| Restricted document cue | `demo-processor` | restricted document rows | redaction/restriction cues without permission weakening |

## Commands

Seed and exit:

```sh
./scripts/seed-demo-data.sh
```

Reset only and exit:

```sh
./scripts/reset-demo-data.sh
```

Run backend continuously with demo profile:

```sh
cd backend && mvn -Dspring-boot.run.profiles=demo spring-boot:run
```

## Validation Coverage

Backend compile and seed integration coverage:

- `cd backend && mvn -q -DskipTests compile`
- `cd backend && mvn -q -Dtest=IkmsDemoDataSeederIntegrationTest test`

Recommended repository validation attempts after the local database is available:

- `cd backend && mvn test`
- `cd frontend && npm run lint`
- `cd frontend && npm test`
- `cd frontend && npm run build`
