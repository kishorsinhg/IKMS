# Review UI from Screenshots

## Role

Act as a Principal Enterprise UX Architect.

Review attached screenshots only.

Do not review source code.

Do not implement changes.

## Read First

Read:

- `specs/001-insurance-broker-ikms/contracts/ui.md`
- `docs/06-ui/01-design-principles.md`
- `docs/06-ui/02-ux-rules.md`
- `docs/06-ui/03-ui-design-guidelines.md`
- `docs/06-ui/04-ui-flow-map.md`
- `docs/06-ui/05-workspace-catalog.md`
- `docs/06-ui/06-design-tokens.md`
- Relevant workspace specification

## Reference Priority

1. Functional UI contract
2. Workspace specification
3. IKMS MUI theme specification
4. Approved enterprise reference screenshot

Material UI is the component foundation.

Do not require default Material styling when IKMS theme overrides define the appearance.

## Evaluate

### Product Fit

- Enterprise workspace character
- Operational rather than dashboard appearance
- Search-first
- Grid-first
- Suitable for long daily use

### Layout

- Header
- Toolbar
- Main work region
- Context panel
- Split-view balance
- Alignment
- Panel sizing
- Scroll behaviour

### Visual System

- Typography
- Density
- White space
- Color
- Borders
- Shadows
- Radius
- Status
- Icons
- Logo
- Visible MUI theme consistency

### MUI Usage Visible from Screenshot

- Appropriate use of DataGrid or table
- Consistent buttons and icon buttons
- Consistent tabs
- Consistent inputs
- Correct use of Drawer or side panel
- No misuse of Card
- No obvious mixed component libraries
- No inconsistent local styling

### Interaction Clarity

- Primary and secondary actions
- Selection
- Active navigation
- Filters
- Permission state
- Evidence linkage
- AI recommendation clarity

### Accessibility Indicators

- Contrast
- Labels
- Text size
- Status beyond color
- Discoverability of icon-only actions

## Mandatory Violations

Call out:

- Card-based operational collections
- KPI dashboard layout
- Gradient
- Glassmorphism
- Oversized card
- Excessive whitespace
- Marketing hero
- Floating chatbot
- Emoji icons
- Mixed icon styles
- Missing IKMS branding
- Low-density grids
- Raw default MUI appearance that ignores the IKMS theme

## Produce

### Screenshot Coverage

### Critical Issues

### Medium Issues

### Minor Improvements

### Layout Improvements

### Information-Hierarchy Improvements

### Component Improvements

### MUI Theme Deviations

### Positive Elements

### Overall Score

### Recommended Next Action

Recommend:

- `02-implement-workspace.md` for missing/incomplete workspace
- `03-improve-workspace.md` for existing workspace

Do not generate implementation plan until requested.
