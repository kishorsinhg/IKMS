# IKMS Material UI Theme Specification

**Version:** 3.1  
**File:** `docs/06-ui/06-design-tokens.md`  
**Purpose:** Shared MUI theme values and component defaults for IKMS.

---

# 1. Theme Ownership

Material UI is the component foundation.

This document defines the IKMS theme through:

- `palette`
- `typography`
- `spacing`
- `shape`
- `shadows`
- `zIndex`
- `breakpoints`
- `components`

Use the existing theme implementation in the repository.

Do not create a parallel theme system.

Do not upgrade the installed MUI major version unless explicitly approved.

---

# 2. Theme Baseline

Adapt to the installed MUI version.

```ts
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#2563EB',
      dark: '#1D4ED8',
      contrastText: '#FFFFFF'
    },
    secondary: {
      main: '#20324A'
    },
    background: {
      default: '#F5F6F8',
      paper: '#FFFFFF'
    },
    text: {
      primary: '#1F2937',
      secondary: '#5F6B7A',
      disabled: '#A0A8B3'
    },
    divider: '#D8DEE6',
    success: { main: '#2E7D32' },
    warning: { main: '#B45309' },
    error: { main: '#C62828' },
    info: { main: '#0369A1' }
  },
  typography: {
    fontFamily: '"Inter", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 13
  },
  spacing: 4,
  shape: {
    borderRadius: 4
  }
});
```

This is a specification example, not a mandatory file structure.

---

# 3. Semantic Colors

| Token | Value |
|---|---|
| Navy 900 | `#17263A` |
| Navy 800 | `#20324A` |
| Navy 700 | `#29415F` |
| Blue 700 | `#1D4ED8` |
| Blue 600 | `#2563EB` |
| Blue 100 | `#DBEAFE` |
| Blue 50 | `#EFF6FF` |
| Slate 900 | `#1F2937` |
| Slate 700 | `#374151` |
| Slate 600 | `#4B5563` |
| Slate 500 | `#6B7280` |
| Slate 400 | `#9CA3AF` |
| Slate 300 | `#D1D5DB` |
| Slate 200 | `#E5E7EB` |
| Slate 100 | `#F3F4F6` |
| Slate 50 | `#F8FAFC` |
| Background | `#F5F6F8` |
| Surface | `#FFFFFF` |
| Border | `#D8DEE6` |
| Border Strong | `#C3CBD5` |
| Success | `#2E7D32` |
| Warning | `#B45309` |
| Error | `#C62828` |
| Info | `#0369A1` |

Use theme palette references in components.

---

# 4. Typography

| Usage | Size | Weight |
|---|---:|---:|
| Operational body | 13px | 400 |
| General body | 14px | 400 |
| Small metadata | 12px | 400 |
| Caption | 11px | 400 |
| Section title | 16px | 600 |
| Workspace title | 22px | 600 |
| Dialog title | 18px | 600 |

Rules:

- Font: Inter
- Compact line height: 1.3
- Body line height: 1.4
- No marketing typography
- No page title above 24px
- Map these values to MUI typography variants

---

# 5. Spacing

MUI spacing unit: `4px`.

| Theme Spacing | Value |
|---|---:|
| `theme.spacing(1)` | 4px |
| `theme.spacing(2)` | 8px |
| `theme.spacing(3)` | 12px |
| `theme.spacing(4)` | 16px |
| `theme.spacing(5)` | 20px |
| `theme.spacing(6)` | 24px |
| `theme.spacing(8)` | 32px |

Defaults:

- Component gap: 8px
- Panel padding: 12px
- Workspace padding: 16px
- Form row gap: 12px

---

# 6. Shape and Elevation

| Element | Value |
|---|---|
| Default radius | 4px |
| Dialog radius | 6px |
| Chip radius | 3px |
| Standard Paper elevation | 0 |
| Structural separation | Border |
| Overlay elevation | Restrained MUI shadow |

Rules:

- Prefer borders over shadows.
- Do not use large rounded cards.
- Do not use floating dashboard surfaces.

---

# 7. Shell Dimensions

| Element | Value |
|---|---:|
| Top navigation | 48px |
| Workspace header | 56px |
| Toolbar | 44px |
| Status bar | 28px |
| Left navigation | 232px |
| Collapsed navigation | 52px |
| Right context panel | 360px |
| Context panel minimum | 320px |
| Context panel maximum | 420px |

---

# 8. Logo and Icons

| Item | Value |
|---|---|
| Header logo height | 28px |
| Compact logo | 28 × 28px |
| Login logo height | 36px |
| Standard icon | 16px |
| Navigation icon | 18px |
| Large status icon | 20px |

Rules:

- Use official IKMS SVG.
- Use one outlined icon library.
- Do not use emoji.
- Do not mix icon styles.
- Icon-only actions require tooltip and accessible label.

---

# 9. MUI Component Defaults

## `MuiCssBaseline`

- Apply application background.
- Apply Inter font.
- Use border-box sizing.
- Preserve browser focus accessibility.

## `MuiButton`

- `defaultProps.size`: `small`
- `defaultProps.disableElevation`: `true`
- Minimum height: 32px
- Text transform: none
- Border radius: 4px
- Font size: 13px
- Font weight: 500

## `MuiIconButton`

- Default size: small
- Target size: 28–32px
- Require tooltip and `aria-label`

## `MuiTextField`

- `defaultProps.size`: `small`
- `defaultProps.variant`: `outlined`
- Target control height: 32px

## `MuiOutlinedInput`

- Radius: 4px
- Font size: 13px
- Compact vertical padding
- Focus border uses primary blue

## `MuiSelect`

- Size: small
- Compact item height
- Use searchable `Autocomplete` when options exceed 10

## `MuiAutocomplete`

- Size: small
- Compact option height
- Use for customers, policies, claims and other known entities

## `MuiCheckbox`

- Size: small
- 16px visual target where practical

## `MuiRadio`

- Size: small

## `MuiSwitch`

- Use only for immediate binary settings
- Avoid using switches for delayed save operations

## `MuiToolbar`

- Dense height: 44px
- Horizontal padding: 8px
- White surface
- Bottom border

## `MuiPaper`

- Default elevation: 0
- Structural surfaces use borders
- Do not apply card-like shadows by default

## `MuiCard`

- Use only for:
  - AI Brief
  - Alerts
  - Summary
  - Quick Actions
  - Evidence Summary
- Never use for operational collections

## `MuiTabs`

- Compact height: 38px
- Restrained blue indicator
- Active label weight: 600

## `MuiTab`

- Text transform: none
- Font size: 13px
- Minimum height: 38px
- Compact horizontal padding

## `MuiChip`

- Height: 22px
- Font size: 11px
- Radius: 3px
- Restrained fill
- Use for status and active filters

## `MuiDialog`

- Small: 400px
- Standard: 520px
- Large: 720px
- Use for confirmation, assignment and conflict
- Do not use for long forms

## `MuiDrawer`

- Standard editor width: 420px
- Wide editor width: 560px
- Fixed header and footer
- Independently scrolling body

## `MuiTooltip`

- Use for icon-only actions
- Keep text concise
- Use theme z-index

## `MuiAlert`

- Compact padding
- Inline near affected content
- Critical alerts require acknowledgement

## `MuiSnackbar`

- Width target: 360px
- Top-right placement
- Maximum three visible
- Auto-dismiss after 5 seconds unless critical

## `MuiMenuItem`

- Compact height: 32px
- Font size: 13px

## `MuiTableCell`

- Compact padding
- Header font size: 12px
- Body font size: 13px

---

# 10. MUI X Data Grid Defaults

Use the installed MUI X Data Grid package and licence tier.

| Setting | Value |
|---|---:|
| Header height | 36px |
| Row height | 38px |
| Compact row height | 34px |
| Expanded row height | 44px |
| Cell horizontal padding | 8px |
| Selection column | 40px |
| Icon column | 36px |
| Action column | 44px |
| Header font size | 12px |
| Cell font size | 13px |
| Default page size | 50 |
| Incremental batch | 100 |

Theme rules:

- Header background: slate 100
- Header text: secondary text
- Header weight: 600
- Border color: divider
- Selected row: blue 50
- Hover row: subtle neutral
- No alternating row colors
- Compact density
- Sticky header
- Preserve column state where supported
- Use DataGrid slots and theme overrides

Do not implement Pro or Premium features unless installed and licensed.

---

# 11. MUI X Tree View Defaults

Use the installed MUI X Tree View package.

Defaults:

- Compact item height
- 16–18px icons
- Clear selected state
- Keyboard navigation
- Expand/collapse icons from the shared icon set

Use for Administration explorer.

---

# 12. Search

| Setting | Value |
|---|---:|
| Height | 36px |
| Minimum width | 320px |
| Preferred width | 520px |

Use `TextField` or `Autocomplete`.

Search icon left, clear action right.

---

# 13. Right Context Panel

| Setting | Value |
|---|---:|
| Default width | 360px |
| Minimum | 320px |
| Maximum | 420px |
| Section padding | 12px |
| Section gap | 12px |
| Header height | 40px |

Use bordered `Box` or `Drawer`.

Use compact stacked sections.

---

# 14. Document Viewer

| Setting | Value |
|---|---:|
| Viewer toolbar | 40px |
| Viewer background | `#EEF1F5` |
| Page background | White |
| Thumbnail width | 112px |
| Minimum viewer width | 480px |

Viewer controls use MUI buttons and tooltips.

---

# 15. Status Mapping

| Status Group | Text | Background |
|---|---|---|
| Success | `#256029` | `#E8F5E9` |
| Warning | `#8A4B08` | `#FFF7ED` |
| Error | `#A61B1B` | `#FEF2F2` |
| Info | `#075985` | `#E0F2FE` |
| Neutral | `#4B5563` | `#F3F4F6` |

Use `Chip`.

Do not use color alone.

---

# 16. Breakpoints

| Breakpoint | Width |
|---|---:|
| Minimum supported desktop | 1280px |
| Compact desktop | 1366px |
| Standard desktop | 1440px |
| Large desktop | 1600px |
| Primary target | 1920px |

Rules:

- Collapse context panel first.
- Collapse navigation second.
- Preserve grids.
- Allow horizontal scrolling.
- Do not transform grids into cards.

---

# 17. Z-Index

| Layer | Value |
|---|---:|
| Base | 0 |
| Sticky grid header | 20 |
| Workspace toolbar | 90 |
| Application header | 100 |
| Dropdown | 300 |
| Drawer | 400 |
| Modal | 500 |
| Toast | 600 |
| Tooltip | 700 |

Map to MUI `zIndex`.

Do not create arbitrary component-specific values.

---

# 18. Motion

| Token | Value |
|---|---:|
| Fast | 120ms |
| Normal | 180ms |
| Easing | ease-out |

Use only for:

- Drawer
- Menu
- Panel collapse
- Status transition

Respect reduced motion.

---

# 19. Implementation Rules

- Use one shared MUI theme.
- Use `ThemeProvider` and `CssBaseline`.
- Do not create a competing theme.
- Do not hardcode repeated values.
- Use `sx` for local layout only.
- Use theme component overrides for repeated styling.
- Use standard MUI components wherever possible.
- Build custom components only for IKMS-specific workflows.
- Use installed MUI packages and licence tier only.
- Keep operational collections in DataGrid.
- Keep Administration hierarchy in TreeView.
- Keep AI contextual.
