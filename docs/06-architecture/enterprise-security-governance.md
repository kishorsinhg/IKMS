# Enterprise Security, Governance, And Compliance

## Scope

This phase extends the existing RBAC-based IKMS platform with additive governance controls for regulated knowledge handling. Customer remains the primary business context. Policy Number, Claim Number, Broker Reference, Insurer, Effective Date, Expiry Date, Renewal Date, and similar values remain Business Reference Fields only.

## Authorization Model

- RBAC remains the baseline permission model.
- ABAC augments RBAC using additive checks on `businessUnit`, `department`, `region`, `country`, `brokerOffice`, and `securityClearance`.
- Document access is denied when user clearance is below the document classification or when scoped document attributes conflict with the current user context.
- Existing permissions remain intact. New governance-sensitive export behavior relies on `EXPORT_SENSITIVE_CONTENT` and administrative governance actions rely on `MANAGE_GOVERNANCE`.

## Information Governance

- Document classification levels:
  - `PUBLIC`
  - `INTERNAL`
  - `CONFIDENTIAL`
  - `RESTRICTED`
  - `HIGHLY_RESTRICTED`
- Governance-only lifecycle states:
  - `ACTIVE`
  - `ARCHIVED`
  - `RETENTION_HOLD`
  - `LEGAL_HOLD`
  - `PENDING_DISPOSAL`
  - `DISPOSED`
- Retention records now carry `holdType`, `retentionPolicyKey`, `reviewAt`, `archivalEligibleAt`, and `disposalEligibleAt`.

## Enforcement Points

- Search excludes governance-restricted documents before result shaping.
- Client AI and enterprise orchestration inherit the same search governance filtering.
- Document preview and download paths now apply ABAC and classification checks before PII/redaction routing.
- AI provider execution falls back to deterministic answers when the configured model is not in the approved model registry.

## Configuration

- Governance policy configuration is persisted through the existing application settings store for classification policy, retention policy, AI governance policy, and security policy.
- Legal hold state remains persisted in `retention_record`.

## Security Architecture

- Encryption at rest, encryption in transit, key-management abstraction, and secret-management posture are captured in the security policy contract and surfaced in Administration.
- The implementation remains vendor-neutral and does not assume any cloud-specific KMS or secret store.
