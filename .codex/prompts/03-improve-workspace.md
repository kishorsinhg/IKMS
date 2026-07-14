# Improve Existing Workspace

## Objective

Improve one existing workspace to conform to IKMS UI Design System v3.1.

Workspace:

`<WORKSPACE_NAME>`

Specification:

`docs/06-ui/screens/<WORKSPACE_FILE>.md`

Preserve business logic, APIs, permissions, routing and repository structure.

## Read First

Read:

- `specs/001-insurance-broker-ikms/spec.md`
- `specs/001-insurance-broker-ikms/contracts/ui.md`
- Relevant API contract
- `docs/06-ui/01-design-principles.md`
- `docs/06-ui/02-ux-rules.md`
- `docs/06-ui/03-ui-design-guidelines.md`
- `docs/06-ui/04-ui-flow-map.md`
- `docs/06-ui/05-workspace-catalog.md`
- `docs/06-ui/06-design-tokens.md`
- Relevant workspace specification

## Material UI Standard

- Use Material UI as the component foundation.
- Use installed MUI packages and licence tier.
- Reuse and improve the existing MUI theme.
- Do not create a competing component library.
- Identify custom controls that duplicate MUI and consolidate them safely.
- Custom IKMS components must compose MUI primitives.

## Evaluate

Compare the workspace against the specification.

Review:

- Workflow
- Information hierarchy
- Visual hierarchy
- Navigation
- Operational density
- Grid-first compliance
- Toolbar placement
- Context panel
- AI presentation
- MUI component selection
- Theme compliance
- Hardcoded values
- Repeated `sx`
- Misuse of `Card`
- Mixed icon libraries
- Logo usage
- Accessibility
- Responsiveness
- Keyboard navigation
- Loading
- Empty
- Error
- Restricted state
- State preservation
- Performance
- Component reuse

## Improvement Goals

Improve:

- Productivity
- Density
- MUI consistency
- Theme adoption
- Component reuse
- Accessibility
- Performance perception

Remove:

- Card-based operational collections
- Dashboard layouts
- Oversized whitespace
- Gradients
- Glassmorphism
- Duplicate standard controls
- Mixed icon styles
- Hardcoded theme values
- Floating chatbot patterns

## Implementation Rules

- Preserve APIs, routing, permissions and state management.
- Do not create duplicate routes or pages.
- Use shared MUI-based components.
- Use theme overrides for repeated styling.
- Use `sx` for local layout only.
- Do not upgrade MUI major version.
- Do not modify unrelated workspaces.

## Before Completion

Run:

- Format
- Lint
- Type check
- Tests
- Frontend build

## Completion Report

### Improvements Implemented

### MUI Deviations Fixed

### Theme Changes

### Components Reused

### Components Consolidated

### Existing Behaviour Preserved

### Test Results

### Breaking Changes

### Remaining Issues

Do not stop after analysis.
