# IKMS UI Design Guidelines

**Version:** 3.1  
**Purpose:** Material UI implementation specification for IKMS.  
**Audience:** Frontend Developers, UX Engineers, AI Coding Agents.

---

# 1. Foundation

IKMS uses Material UI as the standard component library.

The rules in this document define:

- MUI theme values
- Component defaults
- Density overrides
- Enterprise workspace composition
- IKMS-specific components

Do not rebuild standard Material UI controls.

Use the installed MUI version and APIs already present in the repository.

Do not upgrade the MUI major version as part of a UI redesign unless explicitly approved.

---

# 2. Component Selection

| Need | Use |
|---|---|
| Layout | `Box`, `Stack`, CSS Grid |
| Structural surface | `Paper` or bordered `Box` |
| Typography | `Typography` |
| Breadcrumb | `Breadcrumbs` |
| Toolbar | `Toolbar`, `Stack` |
| Primary action | `Button variant="contained"` |
| Secondary action | `Button variant="outlined"` |
| Tertiary action | `Button variant="text"` |
| Icon action | `IconButton` + `Tooltip` |
| Input | `TextField` |
| Searchable selection | `Autocomplete` |
| Selection | `Select` |
| Boolean | `Checkbox`, `Radio`, `Switch` |
| Tabs | `Tabs`, `Tab` |
| Menu | `Menu`, `MenuItem` |
| Confirmation | `Dialog` |
| Secondary editor | `Drawer` |
| Alert | `Alert` |
| Notification | `Snackbar` |
| Loading | `Skeleton`, `CircularProgress`, `LinearProgress` |
| Status | `Chip` |
| Operational collection | MUI X `DataGrid` |
| Configuration explorer | MUI X `TreeView` |
| Pagination | `Pagination` or DataGrid pagination |
| Tooltip | `Tooltip` |

Do not use MUI `Card` for operational collections.

---

# 3. Visual Direction

Required:

- Compact enterprise workspace
- Desktop-first
- High information density
- Thin borders
- Minimal shadows
- Small radius
- Neutral surfaces
- Restrained blue accent
- Consistent outlined icons

Avoid:

- SaaS dashboard styling
- KPI card collections
- Marketing pages
- Gradients
- Glassmorphism
- Oversized whitespace
- Large rounded cards
- Decorative animation
- Floating AI chatbot

---

# 4. Typography

Use Material UI typography through the shared theme.

| Usage | Size |
|---|---:|
| Operational body | 13px |
| General body | 14px |
| Small metadata | 12px |
| Caption | 11px |
| Section title | 16px |
| Workspace title | 22px |
| Dialog title | 18px |

Rules:

- Font: Inter
- Use weight before increasing size.
- Do not exceed 24px for workspace titles.
- Keep line height compact.
- Use `Typography` variants mapped by the theme.
- Avoid page-specific font-size overrides.

---

# 5. Color

Use the shared MUI palette.

Primary values:

| Role | Value |
|---|---|
| Primary navy | `#20324A` |
| Primary blue | `#2563EB` |
| Background | `#F5F6F8` |
| Surface | `#FFFFFF` |
| Border | `#D8DEE6` |
| Text primary | `#1F2937` |
| Text secondary | `#5F6B7A` |
| Success | `#2E7D32` |
| Warning | `#B45309` |
| Error | `#C62828` |
| Info | `#0369A1` |

Rules:

- Use semantic palette references.
- Do not hardcode colors inside workspace components.
- Reserve strong color for status, action and focus.
- Do not use bright fills for large surfaces.

---

# 6. Spacing

The theme uses a 4px spacing unit.

| Token | Value |
|---|---:|
| 1 | 4px |
| 2 | 8px |
| 3 | 12px |
| 4 | 16px |
| 5 | 20px |
| 6 | 24px |
| 8 | 32px |

Defaults:

- Component gap: 8px
- Panel padding: 12px
- Workspace padding: 16px
- Form row gap: 12px
- Major section gap: 20–24px

Avoid spacing above 32px inside operational workspaces.

---

# 7. Borders, Radius and Elevation

| Rule | Value |
|---|---|
| Standard border | 1px solid theme divider |
| Strong border | 1px solid strong divider |
| Component radius | 4px |
| Dialog radius | 6px |
| Badge radius | 3px |
| Standard Paper elevation | 0 |
| Dialog elevation | MUI dialog default or restrained override |

Prefer borders over shadows.

Do not apply default card-like shadows to workspace sections.

---

# 8. Application Shell

The shared shell contains:

- IKMS logo
- Primary navigation
- Global search access
- Notifications
- Help
- User menu
- Workspace header
- Toolbar
- Main work region
- Optional right context panel

Dimensions:

| Element | Size |
|---|---:|
| Top navigation | 48px |
| Workspace header | 56px |
| Toolbar | 44px |
| Status bar | 28px |
| Left navigation | 232px |
| Collapsed navigation | 52px |
| Right context panel | 360px |
| Context panel min/max | 320–420px |

Use `Box`, `Stack`, `Toolbar`, `Divider` and CSS Grid.

Do not wrap the entire application in `Card`.

---

# 9. Logo and Icons

Use the official IKMS SVG logo.

Logo sizes:

- Header: 28px height
- Collapsed shell: 28 × 28px
- Login: 36px height

Use one outlined SVG icon library.

Rules:

- Do not mix icon libraries.
- Do not use emoji.
- Icon-only actions require tooltip and `aria-label`.
- Use the same icon for the same action.

Standard mapping:

| Action/Object | Icon |
|---|---|
| Search | `Search` |
| Filter | `FilterList` or equivalent |
| Refresh | `Refresh` |
| Export | `Download` |
| Import | `Upload` |
| Save | `Save` |
| Edit | `Edit` |
| Delete | `Delete` |
| Add | `Add` |
| More | `MoreHoriz` |
| Customer | `Person` |
| Organization | `Business` |
| Document | `Description` |
| Email | `Mail` |
| Note | `Note` |
| Relationship | `Link` |
| Policy | `Shield` |
| Claim | `Assignment` |
| Review | `FactCheck` |
| Approve | `CheckCircle` |
| Reject | `Cancel` |
| Assign | `PersonAdd` |
| AI | `AutoAwesome` |
| Warning | `WarningAmber` |
| Evidence | `ContentCopy` |
| History | `History` |
| Audit | `ReceiptLong` |
| Administration | `Settings` |

Use icons from the installed icon package where possible.

---

# 10. Buttons

Use:

- `Button`
- `IconButton`
- `ButtonGroup`

| Usage | Variant |
|---|---|
| Primary | `contained` |
| Secondary | `outlined` |
| Tertiary | `text` |
| Destructive | `contained`, `color="error"` |
| Toolbar icon | `IconButton` |

Defaults:

- `size="small"`
- Disable elevation
- Target height: 32px
- Radius: 4px
- Text transform: none
- Maximum two visually primary actions per workspace

Do not create custom button implementations.

---

# 11. Form Controls

Use:

- `TextField`
- `Select`
- `Autocomplete`
- `Checkbox`
- `Radio`
- `Switch`
- `FormControl`
- `FormLabel`
- `FormHelperText`

Defaults:

- `size="small"`
- `variant="outlined"`
- Target height: 32px
- Labels remain visible
- Required fields use `*`
- Validation uses MUI `error` and helper text
- Use `Autocomplete` for searchable entity selection
- Use standard MUI focus and keyboard behaviour

Do not create custom input wrappers that duplicate MUI behaviour.

---

# 12. Search

Use `TextField` or `Autocomplete` depending on suggestion requirements.

Defaults:

- Height: 36px
- Minimum width: 320px
- Preferred width: 520px
- Search icon on left
- Clear action on right
- Enter submits
- Escape closes suggestions before clearing query

Search results always use a grid or structured list.

---

# 13. Data Grid

Use the existing MUI X Data Grid package.

Use only the installed licence tier.

Do not implement Pro or Premium features unless the package and licence already exist.

Defaults:

| Setting | Value |
|---|---:|
| Header height | 36px |
| Row height | 38px |
| Compact row height | 34px |
| Cell padding | 8px |
| Selection column | 40px |
| Icon column | 36px |
| Action column | 44px |
| Page size | 50 |

Required where supported:

- Sorting
- Filtering
- Column visibility
- Column resizing
- Row selection
- Pagination or incremental loading
- Keyboard navigation
- Virtualization
- Sticky header
- Preserved grid state

Rules:

- Use `density="compact"` or theme equivalent.
- Do not use alternating row colors by default.
- Keep identifier columns visible.
- Truncated values require tooltip.
- Use DataGrid slots and theme overrides instead of page-specific duplicate CSS.
- Do not replace DataGrid with card lists.

---

# 14. Toolbar

Use `Toolbar`, `Stack`, `Button`, `IconButton`, `Tooltip` and filter controls.

Structure:

```text
Left: Search / View / Scope
Center: Filters / Active filters
Right: Bulk actions / Export / Refresh / Columns
```

Rules:

- Frequent actions remain visible.
- Secondary actions use `Menu`.
- Bulk actions appear only when rows are selected.
- Keep refresh and column settings on the right.
- Toolbar may be sticky below the workspace header.

---

# 15. Tabs

Use `Tabs` and `Tab`.

Defaults:

- Compact height
- Restrained blue indicator
- Active font weight 600
- Preserve active state
- Use count badges only when operationally useful
- Allow horizontal scrolling when needed

Do not implement tabs as cards or custom buttons.

---

# 16. Right Context Panel

Use a persistent bordered `Box` or `Drawer`, depending on workspace behaviour.

Dimensions:

- Default: 360px
- Minimum: 320px
- Maximum: 420px

Allowed content:

- AI Brief
- Alerts
- Evidence
- Activity
- Review History
- Related Records
- Quick Actions

Rules:

- Scroll independently.
- Use a thin left border.
- Support collapse.
- Preserve collapse state where appropriate.
- Use compact stacked sections.
- Do not place primary collections inside the panel.

---

# 17. AI Components

Use IKMS-specific components composed from MUI primitives.

Examples:

- `AIBrief`
- `AIWarning`
- `AIRecommendation`
- `EvidencePanel`
- `ConfidenceIndicator`

Use `Paper`, `Box`, `Typography`, `Chip`, `Alert`, `Divider` and icons.

Rules:

- AI is contextual, not a floating chatbot.
- AI does not commit operational changes.
- Recommendations show evidence when available.
- Confidence uses text and status, not color alone.
- AI loading does not block the workspace.

---

# 18. Status

Use MUI `Chip` with IKMS theme overrides.

Default:

- Compact height
- 11px label
- Small radius
- Restrained fill

Status groups:

- Success: Approved, Indexed
- Warning: Pending
- Error: Rejected, Failed
- Info: In Review
- Neutral: Archived
- Restricted: Locked or access-limited

Do not rely on color alone.

---

# 19. Forms

Use MUI layout primitives and standard controls.

Defaults:

- Two columns for standard forms
- Three columns for dense metadata
- One column in narrow drawers
- Section headings use compact typography
- Save and Cancel remain visible in long editors
- Inline validation
- No large forms inside dialogs

---

# 20. Dialogs and Drawers

Use:

- `Dialog` for confirmation, assignment and conflicts
- `Drawer` for secondary editing and details

Rules:

- Dialogs must not contain long operational forms.
- Drawers anchor right unless specified otherwise.
- Fixed header and footer.
- Body scrolls independently.
- Warn before closing unsaved edits.
- Return focus to triggering control.

---

# 21. Explorer and Tree Navigation

Use the installed MUI X Tree View package.

Use for Administration configuration hierarchy.

Do not implement a custom recursive tree unless required behaviour is unavailable.

---

# 22. Document Viewer

The viewer is IKMS-specific.

Compose its controls using:

- `ButtonGroup`
- `IconButton`
- `Tooltip`
- `Toolbar`
- `Typography`
- `Divider`
- `Box`

Supports:

- Zoom
- Fit width
- Fit page
- Rotate
- Page navigation
- OCR layer
- Text search
- Download
- Evidence highlighting

The viewer remains the primary region in Review.

---

# 23. Empty, Loading and Error States

Use MUI components.

| State | Use |
|---|---|
| Empty | `Box`, icon, `Typography`, `Button` |
| Grid loading | DataGrid loading overlay or `Skeleton` |
| Panel loading | `Skeleton` |
| Inline action | `CircularProgress` |
| Background process | `LinearProgress` |
| Error | `Alert` or local error panel |
| Notification | `Snackbar` |

Rules:

- Load regions independently.
- Do not block the whole workspace unnecessarily.
- Do not use illustrations.
- Do not expose stack traces.

---

# 24. Accessibility

Use Material UI semantics and keyboard behaviour.

Requirements:

- WCAG AA
- Visible focus
- Accessible labels
- Semantic headings
- Status beyond color
- Screen-reader announcements
- `aria-selected`, `aria-expanded`, `aria-sort` where applicable
- Tooltip and `aria-label` for icon-only buttons
- Focus trapping in dialogs
- Focus restoration after overlays close

---

# 25. Responsive Behaviour

Supported widths:

- 1920
- 1600
- 1440
- 1366
- Minimum 1280

Rules:

- Preserve grids.
- Collapse right context first.
- Collapse navigation second.
- Allow horizontal scrolling.
- Do not transform grids into cards.
- Mobile-first behaviour is outside the primary target.

---

# 26. Custom IKMS Components

Custom components are allowed only for IKMS-specific behaviour.

Expected examples:

- `IkmsAppShell`
- `WorkspaceHeader`
- `WorkspaceToolbar`
- `WorkspaceSplitLayout`
- `RightContextPanel`
- `CustomerSummary`
- `AIBrief`
- `EvidencePanel`
- `DocumentViewer`
- `ReviewMetadataEditor`
- `ReviewActionBar`
- `AuditTimeline`
- `PermissionGuard`
- `RestrictedContentState`

These components should compose Material UI primitives.

Do not create custom replacements for:

- Button
- Input
- Select
- Tabs
- Dialog
- Drawer
- Menu
- Tooltip
- Alert
- Snackbar
- DataGrid
- TreeView

---

# 27. Implementation Rules

- Use Material UI as the component foundation.
- Use the shared IKMS MUI theme.
- Apply visual rules through theme overrides.
- Use `sx` only for local layout, not repeated theme values.
- Do not create a parallel component library.
- Do not mix icon libraries.
- Do not hardcode repeated visual values.
- Use DataGrid for operational collections.
- Use TreeView for Administration hierarchy.
- Use Drawer for secondary editors.
- Use Dialog for confirmations.
- Keep AI contextual.
- Match the compact enterprise reference design.
