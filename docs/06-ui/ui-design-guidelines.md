# UI Design Guidelines

## Purpose

Define the visual and interaction baseline for IKMS V1 so frontend work stays
consistent across authentication, client workspace, review, intake, search,
administration, and audit screens.

This document complements the functional UI contract in
`specs/001-insurance-broker-ikms/contracts/ui.md`. The contract defines what
screens must do; this file defines how they should feel and behave.

## Product UI Direction

IKMS should look like a compact enterprise knowledge workspace for insurance
operations, not a marketing site and not a generic admin template.

The intended character is:

- calm and trustworthy
- dense enough for operational users
- highly legible for document-heavy workflows
- explicit about risk, permissions, review state, and evidence status

Avoid:

- flashy SaaS dashboard styling
- oversized whitespace that slows operators down
- consumer-app aesthetics
- decorative motion unrelated to user intent
- visually hiding important operational detail

## Design Principles

### 1. Readability First

- Optimize for long sessions in client records, queues, and detail views.
- Use strong hierarchy so users can scan titles, metadata, evidence, and status quickly.
- Treat tables, forms, and side panels as primary surfaces, not secondary ones.

### 2. Controlled Density

- Desktop is the primary target.
- Show meaningful information without forcing excessive drilling.
- Prefer compact but breathable layouts over sparse layouts.

### 3. Trust Through Restraint

- Use warm neutrals and restrained accent colors.
- Reserve stronger color for alerts, denied access, review-required states, and destructive actions.
- Keep surfaces stable and predictable.

### 4. Permission-Aware UX

- Differences between Indexer, Processor, Supervisor, and Administrator must be obvious.
- Masked, denied, redacted, and unavailable states should appear intentional, not broken.
- Never silently omit important role-driven behavior without an explanatory state.

### 5. Evidence-Centric Interaction

- Search, AI answers, notes, and document metadata should feel connected to source evidence.
- Citations, document states, and audit-relevant actions must stay visible.

## Layout Rules

### Global Shell

- Use a persistent left navigation rail on desktop.
- Keep the main workspace wide enough for two-panel and three-panel operational views.
- Top-level shell should support:
  - navigation
  - current user context
  - sign-out
  - page title and section summary

### Page Structure

Each primary page should generally follow:

1. Title row with key context and primary actions
2. Filters or task controls near the top
3. Main content area with list/detail or workspace sections
4. Secondary metadata in side panels or cards, not mixed into the main flow without structure

### Preferred Screen Patterns

- Client profile: sectioned workspace with evidence-first content ordering
- Review queue: list plus detail/correction panel
- Search: results list plus filters and evidence detail
- Administration: compact forms and grouped settings panels
- Audit: dense filter toolbar and export-ready results grid

## Typography

Use typography with an editorial-operational balance.

- Headings should feel firm and professional, not playful.
- Body text must remain easy to scan in dense panels and tables.
- Labels, metadata, and status text should be visually distinct from narrative text.

Guidelines:

- Use a clear display style for page titles
- Use a smaller but strong section heading style
- Use compact metadata text for labels, timestamps, statuses, and secondary values
- Avoid oversized headings inside operational panels
- Keep line lengths controlled in detail views and AI answer areas

## Color System

Use warm neutral foundations instead of cold default dashboard blue-gray.

Suggested token categories:

- `bg.canvas`: warm off-white workspace background
- `bg.surface`: slightly elevated panel background
- `bg.subtle`: muted section background
- `fg.default`: primary text
- `fg.muted`: secondary text
- `border.default`: low-contrast separators
- `accent.primary`: restrained dark accent for key actions
- `accent.info`: informational state
- `accent.warning`: review/pending/risk state
- `accent.danger`: denied/destructive/error state
- `accent.success`: confirmed/completed state

Usage rules:

- Use color to clarify state, not to decorate empty space.
- Do not rely on color alone to convey permission or risk.
- Keep red reserved for true errors, denied access, destructive actions, or failed states.
- Use warning tones for low confidence, review required, duplicate uncertainty, and blocked processing.

## Spacing And Rhythm

- Keep outer page spacing generous enough to avoid crowding.
- Keep inner panel spacing tighter for data-heavy areas.
- Use consistent spacing increments across cards, filters, fields, tables, and panels.
- Separate sections with clear spacing and borders; avoid random visual jumps.

Operational guidance:

- Major page sections should have visibly larger separation than controls within a section.
- Dense objects such as tables or metadata grids should use tighter row spacing than freeform content.

## Components And Patterns

### Navigation

- Navigation should be simple, direct, and role-aware.
- Do not overload the left rail with deep nesting in V1.
- Active state must be visually obvious.

### Forms

- Labels always visible; do not rely on placeholders as labels.
- Group related fields into clear panels.
- Validation errors should be close to the field and easy to recover from.
- Administrative forms should be compact and practical.

### Tables And Lists

- Use tables for audit, review queue, search results, and document listings when density matters.
- Keep headers sticky when content gets long if practical.
- Show important statuses inline with the row.
- Prefer sortable/filterable structures over large card walls for operational data.

### Detail Panels

- Use side panels or stacked sections to show metadata, audit context, evidence, and related actions.
- Keep primary action controls close to the content they affect.

### Empty States

- Empty states should explain what the screen is for and what action unlocks content.
- Avoid playful or decorative empty-state messaging.

### Loading States

- Use skeletons or structured placeholders for dense layouts.
- Loading states should preserve layout shape where possible to reduce visual jump.

## State Design

The following states must feel explicit and reusable:

- authenticated
- unauthenticated
- loading
- empty
- validation error
- backend failure
- unauthorized
- review required
- duplicate detected
- redacted
- PII masked
- original access allowed
- original access denied
- no evidence found
- AI refusal
- conflict detected

Each state should include:

- a clear label or message
- a stable visual treatment
- the next expected user action where relevant

## Role-Specific Experience Rules

### Indexer

- Prioritize queue throughput, correction controls, and intake status.
- Keep review actions prominent.

### Processor

- Prioritize client search, profile readability, and evidence access.
- PII restrictions should be clear but not disruptive when working with redacted content.

### Supervisor

- Preserve Processor usability while exposing additional original/PII actions clearly.
- Elevated permissions should feel deliberate, not hidden.

### Administrator

- Favor compact settings workflows and clear grouping.
- Avoid assuming Administrator has broad client-content access unless explicitly granted.

## Responsive Rules

IKMS is desktop-first, but must still function on smaller screens.

- On tablet and smaller laptop widths, collapse multi-column detail layouts progressively.
- On mobile, prioritize read-only access and key navigation rather than dense editing workflows.
- Navigation may collapse, but the current page context and primary action must stay obvious.
- Avoid horizontal overflow in forms and metadata grids.

## Accessibility Baseline

- Maintain strong text/background contrast.
- Ensure keyboard access to navigation, filters, forms, and action controls.
- Use visible focus states.
- Do not encode critical state only through color.
- Ensure tables and form fields have clear semantic labeling.
- Keep error and denied-access messages understandable without surrounding context.

## Motion

- Use subtle transitions for route changes, panel loading, and inline state shifts.
- Keep motion fast and purposeful.
- Avoid ornamental animation in operational workflows.

Good uses:

- shell fade-in on page load
- panel skeleton to content transition
- inline reveal for filters or metadata sections

Bad uses:

- bouncing status elements
- animated distractions in tables or detail panels
- slow transitions that delay task flow

## Initial Implementation Priorities

As future frontend tasks are implemented, apply these priorities in order:

1. Layout hierarchy and information density
2. Readable typography and spacing
3. Consistent state styling for review, permission, and evidence flows
4. Role-aware navigation clarity
5. Responsive cleanup
6. Secondary motion and polish

## References

- Functional UI contract: `specs/001-insurance-broker-ikms/contracts/ui.md`
- UI flow index: `docs/06-ui/ui-flow-map.md`
- Security expectations: `docs/08-security/security-baseline.md`
