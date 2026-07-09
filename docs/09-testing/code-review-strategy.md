# Code Review Strategy

## Purpose

Code review is a required SDS gate for each completed implementation slice.
Review confirms that the slice is correct, tested, secure, and aligned with the
IKMS Constitution before the next dependent slice starts.

## Review Levels

### Foundation Review

Run after setup and foundational scaffolding are complete.

Focus:

- Project structure matches `specs/001-insurance-broker-ikms/plan.md`.
- Build, test, migration, configuration, and storage foundations are coherent.
- Authentication, authorization, audit, and file-storage extension points exist.
- No implementation path bypasses future security trimming.

### Hardening Review

Run after pre-implementation hardening is complete.

Focus:

- CSV import validation and duplicate handling are covered.
- Account status, failed-login handling, lockout, session timeout, and login audit are covered.
- Shared security trimming exists before preview, download, search, and AI context assembly.
- Retention, legal hold, deletion, and anonymization behavior is explicitly designed and tested.
- SLA validation exists for the V1 success criteria.

### Story Slice Review

Run after each user story phase.

Focus:

- Acceptance scenarios for the story pass.
- Tests map to the story and relevant FR/SC IDs.
- Access control and audit events are enforced.
- UI behavior matches the UI contract.
- Data model changes match the feature data model.

### Release Readiness Review

Run before demo or release.

Focus:

- Quickstart validation results are recorded.
- Security, PII, audit, AI guardrails, retention, and source preservation are reviewed.
- Known gaps are documented in handoff and changelog artifacts.

## Review Artifact

Use `docs/templates/code-review-template.md` for each slice review.

Store completed reviews under:

```text
docs/09-testing/reviews/
```

Use filenames such as:

```text
foundation-review.md
hardening-review.md
us1-client-profile-review.md
release-readiness-review.md
```

## Minimum Review Outcome

Each review must end with one of:

- Approved
- Approved with follow-up tasks
- Changes required

Changes required blocks the next dependent implementation slice.
