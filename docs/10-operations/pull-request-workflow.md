# Pull Request Workflow

## Purpose

Use small pull requests as the default review boundary for implementation work.
This project should prefer task-slice PRs over large feature dumps to keep human
and LLM review reliable.

## Rules

- Do not implement directly on `main` once feature development starts.
- Create one branch per logical task slice.
- Keep each PR narrow enough that a reviewer can understand it from the diff alone.
- Update the current handoff and the current daily worklog before opening the PR.
- Map the PR to task IDs from `specs/001-insurance-broker-ikms/tasks.md`.
- Request review before merge. A second LLM review is encouraged, not a substitute for scope discipline.

## Branch Naming

Use one of these prefixes:

- `feature/001-<short-slice-name>`
- `chore/001-<short-slice-name>`
- `docs/001-<short-slice-name>`

Examples:

- `feature/001-foundation-errors-audit`
- `feature/001-foundation-auth-shell`
- `docs/001-pr-workflow`

## Commit Guidance

- Prefer small logical commits inside the branch.
- Keep commit messages imperative and specific.
- Avoid mixing docs-only cleanup with implementation unless the docs are part of the same slice.

Examples:

- `Add Flyway baseline migration scaffold`
- `Implement shared API error model`
- `Add foundation PR workflow documentation`

## Recommended Slice Size

- Target roughly 200 to 600 changed lines when practical.
- Split work earlier if a PR spans unrelated subsystems.
- A full user story is usually too large for one PR.

Good slice examples:

- `T009-T012` migration framework, API error model, exception handling, audit interface
- `T013-T019` auth model, security wiring, route shell, API client foundation
- `T020-T022` test setup and foundation review artifacts

## Required PR Content

Every PR should include:

- Summary of the slice
- Task IDs covered
- What changed
- Tests/checks run
- Known risks or follow-up tasks
- Explicit reviewer focus areas

Use `.github/PULL_REQUEST_TEMPLATE.md`.

## Review Flow

1. Create the branch from current `main`.
2. Implement one task slice only.
3. Update `specs/001-insurance-broker-ikms/tasks.md` for completed items.
4. Update `docs/11-handoff/ai-handoff.md`.
5. Update the current file in `docs/13-worklog/`.
6. Commit the slice.
7. Push the branch and open a PR.
8. Run review using the PR diff.
9. Merge only after findings are addressed or converted into explicit follow-up tasks.

## Reviewer Guidance

- Prioritize correctness, regressions, security trimming, PII exposure, auditability, and missing tests.
- Treat large diffs as a process failure and ask for the slice to be split.
- For LLM review, provide the PR diff and the relevant spec/task references, not the entire repo by default.
