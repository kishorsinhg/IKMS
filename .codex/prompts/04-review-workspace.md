# Product Owner Workspace Review

## Role

Act as:

- Product Owner
- Enterprise UX Architect
- Insurance Operations Reviewer

Review user experience only.

Do not review code quality.

Do not implement changes.

## Input

Workspace:

`<WORKSPACE_NAME>`

## Read

Read:

- `specs/001-insurance-broker-ikms/contracts/ui.md`
- `docs/06-ui/01-design-principles.md`
- `docs/06-ui/02-ux-rules.md`
- `docs/06-ui/03-ui-design-guidelines.md`
- `docs/06-ui/04-ui-flow-map.md`
- `docs/06-ui/05-workspace-catalog.md`
- `docs/06-ui/06-design-tokens.md`
- Relevant workspace specification

## Review Standard

IKMS must feel like a compact enterprise knowledge workspace.

Material UI is the component foundation.

Assess whether MUI is used consistently with the IKMS theme.

Do not expect raw default MUI styling where theme overrides are required.

## Score

Score applicable categories from 1–10.

### Workflow

### Navigation

### Information Hierarchy

### Visual Hierarchy

### Operational Efficiency

### Information Density

### Grid and Table Usability

### Search Experience

### Customer Context

### AI Integration

### Evidence Visibility

### Permission Awareness

### Accessibility

### Keyboard Usability

### Desktop Responsiveness

### Material UI and IKMS Theme Consistency

Evaluate:

- Correct MUI component choice
- Consistent variants
- Shared theme
- Compact density
- No misuse of Card
- No inconsistent local overrides
- No mixed icon libraries
- Official logo usage
- Status consistency
- Toolbar consistency
- Context-panel consistency

Mark non-applicable categories as `N/A`.

## Mandatory Checks

Call out:

- Card-based operational collections
- Dashboard layout
- Excessive whitespace
- Oversized controls
- Gradients
- Glassmorphism
- Mixed icons
- Missing logo
- Weak status visibility
- Missing states
- Lost navigation state
- AI without evidence
- Poor grid density
- Default MUI styling that ignores IKMS theme

## Produce Report

### Executive Assessment

### Scorecard

| Category | Score | Evidence |
|---|---:|---|

### Excellent

### Critical Issues

### Important Improvements

### Minor Improvements

### Missing Required Behaviour

### Quick Wins

### High-Impact Improvements

### Recommended Priority

### Overall Score

Do not implement.

Wait for approval.
