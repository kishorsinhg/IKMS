# Search Workspace

**Version:** 3.1  
**File:** `docs/06-ui/screens/search-workspace.md`

---

# Purpose

The Search Workspace is the authenticated home page of IKMS.

It is the primary workspace for locating and opening enterprise knowledge.

---

# Layout

```text
Workspace Header
Search Toolbar
Saved Searches / Active Filters
Search Results DataGrid
Optional Preview
Right Context Panel
```

---

# Material UI Components

- `TextField` or `Autocomplete` for search
- `Toolbar` for actions
- `Chip` for active filters
- MUI X `DataGrid` for results
- `Menu` for row actions
- `Drawer` or bordered `Box` for context
- `Tooltip` for icon-only actions

Use the installed MUI licence tier only.

---

# Header

Use shared `WorkspaceHeader`.

Actions:

| Action | Component |
|---|---|
| Saved Search | `Button` or `Menu` |
| Refresh | `IconButton` |
| Export | `Button` |
| Columns | `IconButton` |

---

# Search

Supports:

- Full text
- Customer
- Policy number
- Claim number
- Email
- Metadata
- Notes

Shortcut: `Ctrl+K`

Search remains visible.

---

# Filters

- Customer
- Document Type
- Policy
- Claim
- Email
- Owner
- Created Date
- Modified Date
- Status
- Tags

Use standard MUI controls.

Persist filters until cleared.

---

# Saved Searches

Users may:

- Save
- Rename
- Delete
- Pin

Use `Menu`, `Dialog` and `Chip` where appropriate.

---

# Results DataGrid

Columns:

| Column | Width |
|---|---:|
| Type Icon | 40 |
| Title | 320 |
| Customer | 220 |
| Policy | 140 |
| Claim | 140 |
| Type | 120 |
| Modified | 160 |
| Owner | 160 |
| Status | 110 |

Required where supported:

- Sorting
- Filtering
- Column resize
- Column visibility
- Row selection
- Pagination or incremental loading
- Keyboard navigation
- Virtualization
- Preserved grid state

Double-click or Enter opens the selected record.

---

# Row Actions

Use `Menu`.

- Open
- Open in New Tab
- Preview
- AI Summary
- Download
- Copy Link
- Properties

---

# Result Types

| Type | Icon |
|---|---|
| Document | Document icon |
| Email | Mail icon |
| Note | Note icon |
| Customer | Person icon |
| Policy | Shield icon |
| Claim | Assignment icon |

Use the shared icon library.

---

# Selection

Single selection:

- AI Brief
- Metadata
- Activity
- Related Records

Multiple selection:

- Export
- Assign
- Download
- AI Summary where supported

Bulk actions appear in the toolbar only when rows are selected.

---

# Right Context Panel

Sections:

- AI Brief
- Search Insights
- Evidence
- Related Records
- Recent Activity
- Quick Actions

Use compact bordered sections.

Do not use oversized cards.

---

# States

Support:

- Populated
- Loading
- Empty
- Error
- Restricted
- Long titles
- Mixed statuses
- Active filters
- Selected row

---

# Keyboard

| Shortcut | Action |
|---|---|
| Ctrl+K | Focus search |
| Arrow keys | Navigate results |
| Enter | Open |
| Space | Select |
| Esc | Clear selection or close menu |
| F5 | Refresh |

---

# Permissions

Actions must render according to existing permission contracts.

Restricted records must appear intentional, not broken.

---

# Codex Rules

- Search is the authenticated home page.
- Use MUI X DataGrid for results.
- Do not use cards for results.
- Preserve query, filters, sorting, selection and scroll position.
- Use shared MUI theme values.
- Use official logo and shared icons.
- Do not recreate standard MUI controls.
