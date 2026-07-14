# Implement Workspace

## Objective

Implement one selected workspace according to the approved specifications.

Workspace:

`<WORKSPACE_NAME>`

Specification:

`docs/06-ui/screens/<WORKSPACE_FILE>.md`

## Read First

Read:

- `specs/001-insurance-broker-ikms/spec.md`
- `specs/001-insurance-broker-ikms/plan.md`
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

- Material UI is the standard component foundation.
- Use installed MUI packages and version.
- Use installed MUI X licence tier only.
- Reuse and improve the shared MUI theme.
- Do not create a parallel theme or component library.
- Do not recreate standard MUI controls.
- Custom IKMS components must compose MUI primitives.

Do not create custom replacements for:

- Button
- IconButton
- Input
- Select
- Autocomplete
- Tabs
- Menu
- Dialog
- Drawer
- Tooltip
- Alert
- Snackbar
- DataGrid
- TreeView

## Before Implementation

Identify:

- Route
- APIs
- Permissions
- State management
- Existing MUI components
- Existing theme usage
- Shared components
- Current tests
- Behaviour to preserve

Run the current frontend and record the baseline.

## Implementation Rules

- Implement only the selected workspace and required shared components.
- Preserve APIs, routes, permissions and business logic.
- Refactor rather than duplicate.
- Use the shared MUI theme.
- Move repeated visual values into theme overrides.
- Use `sx` for local layout only.
- Use DataGrid for operational collections.
- Use TreeView for configuration hierarchy.
- Use Drawer for secondary editors.
- Use Dialog for confirmation.
- Use official IKMS logo.
- Use one outlined icon library.
- Do not use emoji.
- Do not use cards for operational collections.
- Do not add dependencies unless technically necessary.
- Do not upgrade MUI major version unless approved.

## Required States

- Populated
- Loading
- Empty
- Error
- Restricted
- Long content
- Mixed status
- Selected row
- Active filters

## Verification

Run:

- Format
- Lint
- Type check
- Tests
- Frontend build

Verify at:

- 1920 × 1080
- 1440 × 900
- 1366 × 768

## Completion Report

### Summary

### Files Changed

### MUI Components Reused

### IKMS Components Added or Updated

### Theme Overrides Added

### Existing Behaviour Preserved

### Verification

### Test Results

### Assumptions

### Remaining Issues

Do not stop after planning unless blocked.
