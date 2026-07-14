# Audit Workspace

**Version:** 3.1  
**File:** `docs/06-ui/screens/audit-workspace.md`

---

# Purpose

The Audit Workspace provides searchable, immutable operational history.

---

# Layout

```text
Workspace Header
Audit Search / Filters
Audit Events DataGrid | Event Details
Timeline
Evidence
```

---

# Material UI Components

- Standard MUI filters
- MUI X `DataGrid`
- `Drawer` or persistent right panel
- IKMS `AuditTimeline`
- `Menu` for export formats
- `Chip` for event result
- `Alert` for investigation warnings

---

# Search and Filters

Search by:

- User
- Customer
- Document
- Policy
- Claim
- Event Type
- Object Type
- Date Range
- IP Address
- Correlation ID

Use standard MUI controls.

Common filters:

- Today
- Yesterday
- Last 7 Days
- Last 30 Days
- Custom Range
- Success
- Failed
- Warning
- AI Events
- Security Events
- Configuration Changes

---

# Audit Events DataGrid

Columns:

| Column | Width |
|---|---:|
| Timestamp | 170 |
| Event | 220 |
| Object | 220 |
| User | 180 |
| Customer | 220 |
| Result | 100 |
| Source | 140 |
| IP Address | 140 |

Supports:

- Sort
- Filter
- Resize
- Selection
- Pagination
- Export
- Keyboard navigation

Audit records are immutable.

---

# Event Details

Display in right context panel or Drawer.

Sections:

- Event Summary
- Object Details
- User Details
- Related Objects
- Request Information
- Correlation ID
- Before and After Values

Read-only.

---

# Timeline

Use IKMS `AuditTimeline` composed from MUI primitives.

Example:

```text
Document Uploaded
↓
OCR Completed
↓
Metadata Extracted
↓
AI Summary Generated
↓
Human Review
↓
Approved
↓
Indexed
```

Newest first unless process order requires chronological display.

---

# AI Context

Allowed:

- Event Summary
- Business Impact
- Related Events
- Unusual Activity Warning
- Suggested Investigation

AI never modifies audit records.

---

# Evidence

Display:

- Previous Value
- New Value
- Source System
- Correlation ID
- Processing Time
- Structured payload viewer where supported

Read-only.

---

# Export

Use `Button` + `Menu`.

Formats:

- CSV
- Excel
- PDF

Respect permissions.

---

# States

- Populated
- Loading
- Empty
- Error
- Restricted
- Long event details
- Mixed result states

---

# Keyboard

| Shortcut | Action |
|---|---|
| Ctrl+K | Search |
| Enter | Open event |
| Ctrl+E | Export |
| F5 | Refresh |
| Esc | Clear selection |

---

# Codex Rules

- Use MUI X DataGrid.
- Use standard MUI filters.
- Use right panel or Drawer for details.
- Timeline is custom but composed from MUI.
- Records are immutable.
- AI summarizes only.
- Export respects permissions.
- Preserve filters and selected event.
- Use shared theme and icons.
