# Administration Workspace

**Version:** 3.1  
**File:** `docs/06-ui/screens/administration-workspace.md`

---

# Purpose

The Administration Workspace manages IKMS configuration.

Use Explorer + Grid + Editor.

Do not use one long settings page.

---

# Layout

```text
Workspace Header
Toolbar
Configuration Explorer | Configuration DataGrid | Editor Drawer
```

---

# Material UI Components

- MUI X `TreeView`
- MUI X `DataGrid`
- `Drawer`
- Standard MUI form controls
- `Dialog` for destructive confirmation
- `Menu` for secondary actions
- `Chip` for status

---

# Configuration Explorer

Modules:

- Users
- Roles
- Permissions
- Document Types
- Metadata Templates
- Classification Rules
- AI Configuration
- Search Configuration
- Integrations
- Notifications
- System Settings
- Feature Flags
- Audit Settings

Use MUI X Tree View.

Preserve selected node.

---

# Configuration DataGrid

Standard columns:

| Column | Width |
|---|---:|
| Name | 260 |
| Category | 180 |
| Description | 320 |
| Status | 120 |
| Modified | 160 |
| Modified By | 180 |

Supports:

- Search
- Sort
- Filter
- Resize
- Selection
- Pagination

---

# Editor Drawer

Use right-anchored MUI `Drawer`.

Sections:

- General
- Properties
- Validation
- Permissions
- History

Controls:

- TextField
- Select
- Autocomplete
- Checkbox
- Switch
- Date control
- JSON editor only where already supported

Rules:

- Fixed header and footer.
- Body scrolls independently.
- Warn before closing unsaved changes.
- Use one column inside narrow drawer.
- Do not use Dialog for large forms.

---

# Users

Columns:

| Column | Width |
|---|---:|
| User | 220 |
| Email | 260 |
| Role | 180 |
| Status | 120 |
| Last Login | 180 |

Actions:

- Create
- Edit
- Disable
- Reset Password

---

# Roles and Permissions

Roles use DataGrid.

Permissions use a matrix or grid with sticky rows and columns.

History remains read-only.

---

# AI Configuration

Sections:

- Models
- Prompts
- Confidence Thresholds
- Extraction Rules
- Review Thresholds

Use existing installed components and permissions.

---

# Search Configuration

Sections:

- Search Fields
- Ranking
- Synonyms
- Stop Words
- Boost Rules

---

# Integrations

Columns:

- Name
- Type
- Endpoint
- Status
- Last Sync

Examples:

- Broker System
- Email
- OCR
- OpenSearch
- LLM

---

# Status

- Active
- Draft
- Disabled
- Deprecated

Use MUI `Chip`.

---

# States

- Populated
- Loading
- Empty
- Error
- Restricted
- Unsaved changes
- Validation error

---

# Keyboard

| Shortcut | Action |
|---|---|
| Ctrl+N | New |
| Ctrl+S | Save |
| Ctrl+D | Duplicate |
| Delete | Delete |
| F2 | Edit |
| Ctrl+F | Search |

---

# Codex Rules

- Use MUI X TreeView and DataGrid.
- Use Drawer for editor.
- Use Dialog only for confirmation.
- Use standard MUI form controls.
- Preserve selected node and grid state.
- Do not build a custom tree, grid or form library.
- Use shared theme and icons.
