# Go-Live Readiness Guide

Date: 2026-07-15

## Platform Checklist

- Configuration values are set for database, storage, CORS, OCR, AI, and embedding providers.
- Secrets are provided through environment management and are not committed to the repository.
- RBAC and ABAC permissions are seeded and reviewed for administrator, processor, supervisor, auditor, and steward roles.
- Governance settings for classification, retention, legal hold, AI governance, and security policy are reviewed.
- OCR, retrieval, and AI provider validation checks succeed in the target environment.
- Knowledge-quality and operations views show no blocking readiness issues.

## Deployment Checklist

- Required environment variables are configured.
- Flyway migrations run successfully.
- Search and retrieval indexes are initialized through the application migration and rebuild flow.
- Embedding initialization or rebuild is completed for seeded and production-imported content.
- Scheduler configuration is reviewed and enabled only after dependencies are healthy.

## Security Checklist

- RBAC permissions are reviewed against the release access matrix.
- ABAC user and document attributes are populated and tested.
- Encryption responsibilities for database, filesystem, and transport are confirmed in the target environment.
- Retention and legal-hold procedures are documented and assigned.
- PII masking and restricted export behaviors are verified.

## Validation Checklist

- Backend compile succeeds.
- Targeted backend tests for observability, operations, and API error handling succeed.
- Frontend lint, targeted frontend tests, and production build succeed.
- Health and diagnostics endpoints return expected results for the target operator role.
- Smoke-test flows cover login, customer search, Customer360, review, document preview, AI ask, audit, governance, quality, and operations.

## Documentation Checklist

- Architecture documentation is current.
- API and data-model references are current.
- Administrator and operations guidance is current.
- Runbooks and troubleshooting guidance exist for core failure modes.
- Worklog and task tracking are updated for the release candidate.

## Exact Environment Variables

- `IKMS_DB_URL`
- `IKMS_DB_USERNAME`
- `IKMS_DB_PASSWORD`
- `IKMS_SERVER_PORT`
- `IKMS_CORS_ALLOWED_ORIGINS`
- Any provider-specific AI, OCR, or storage settings used through administration-managed configuration

## Non-Goals

This guide remains platform-neutral. It does not prescribe Kubernetes, cloud-vendor monitoring, or cloud-specific deployment automation.
