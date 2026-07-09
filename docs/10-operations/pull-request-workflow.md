# Development Workflow

## Purpose

Use direct development on `main` as the default workflow for implementation work.
Keep changes task-scoped and well-documented, but do not require PR creation or
LLM PR review before merging.

## Rules

- Implement directly on `main` unless a temporary branch is useful for risky or interrupted work.
- Keep each commit limited to one logical task slice where practical.
- Update the current handoff and the current daily worklog before or immediately after each significant checkpoint.
- Map completed work to task IDs from `specs/001-insurance-broker-ikms/tasks.md`.
- Push completed slices promptly so the remote stays current.

## Commit Guidance

- Prefer small logical commits.
- Keep commit messages imperative and specific.
- Avoid mixing docs-only cleanup with implementation unless the docs are part of the same slice.

Examples:

- `Add Flyway baseline migration scaffold`
- `Implement shared API error model`
- `Implement auth and protected app foundation`

## Recommended Slice Size

- Target roughly one coherent task group per commit when practical.
- Split work earlier if a change spans unrelated subsystems with different risks.
- A full user story is usually too large for one unchecked commit.

Good slice examples:

- `T009-T012` migration framework, API error model, exception handling, audit interface
- `T013-T019` auth model, security wiring, route shell, API client foundation
- `T020-T022` test setup and foundation review artifacts

## Required Checkpoint Content

Every checkpoint should leave behind:

- Updated task statuses in `specs/001-insurance-broker-ikms/tasks.md`
- Updated `docs/11-handoff/ai-handoff.md`
- Updated current daily worklog in `docs/13-worklog/`
- Verification notes for tests or builds run

## Direct Merge Flow

1. Implement one task slice only.
2. Update `specs/001-insurance-broker-ikms/tasks.md` for completed items.
3. Update `docs/11-handoff/ai-handoff.md`.
4. Update the current file in `docs/13-worklog/`.
5. Run the relevant tests or builds.
6. Commit the slice on `main`.
7. Push `main`.
