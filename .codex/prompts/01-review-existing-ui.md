# Review Existing UI

## Objective

Review the existing frontend implementation before making changes.

Do not modify code.

## Read First

Read:

- `specs/001-insurance-broker-ikms/spec.md`
- `specs/001-insurance-broker-ikms/plan.md`
- `specs/001-insurance-broker-ikms/contracts/ui.md`
- `docs/06-ui/01-design-principles.md`
- `docs/06-ui/02-ux-rules.md`
- `docs/06-ui/03-ui-design-guidelines.md`
- `docs/06-ui/04-ui-flow-map.md`
- `docs/06-ui/05-workspace-catalog.md`
- `docs/06-ui/06-design-tokens.md`
- Relevant workspace specification

## Material UI Standard

- Material UI is the standard component library.
- Use the installed MUI packages and version as the baseline.
- Use the installed MUI X licence tier only.
- IKMS defines theme, layout, workspace patterns and business-specific components.
- Do not recommend a parallel component library.

## Review

Identify:

- Frontend architecture
- Routing
- Authenticated landing route
- Application shell
- Layout
- Component hierarchy
- MUI package versions
- MUI X packages and licence tier
- `ThemeProvider`
- `CssBaseline`
- Theme structure
- Component overrides
- Shared MUI-based components
- Custom controls duplicating MUI
- Mixed component libraries
- Icon library and logo usage
- Hardcoded colors, spacing, dimensions and typography
- Repeated `sx` values that belong in the theme
- DataGrid usage
- TreeView usage
- State management
- APIs
- Permissions
- Loading, empty, error and restricted states
- Keyboard navigation
- Accessibility
- Desktop responsiveness
- Test coverage

## Compare Against Design System

Check:

- Search as authenticated home
- Grid-first operational collections
- Compact density
- Thin borders
- Minimal shadows
- Small radius
- Inter typography
- Shared shell and toolbar
- Right context panel
- Official logo
- Consistent outlined icons
- AI as contextual assistance
- No dashboard cards
- No gradients or glassmorphism
- No custom duplicates of standard MUI controls

## Produce Report

### Current Architecture

### MUI Foundation

Include:

- Version
- Packages
- Licence tier
- Theme location
- Theme gaps
- Component overrides
- Duplicate controls

### Strengths

### Critical Issues

### Medium Issues

### Minor Issues

### Design-Token Gaps

### Reusable Components

For each:

- Location
- Current foundation
- Reuse potential
- Required changes

### Workspace Readiness

- Login
- Search
- Customer360
- Review
- Administration
- Audit

### Technical Debt

### Recommended Implementation Order

## Constraints

- Do not implement.
- Do not restructure the repository.
- Do not introduce new scope.
- Do not recommend another UI library unless MUI cannot meet a documented requirement.
- Wait for approval.
