# Review Workspace

**Version:** 3.1  
**File:** `docs/06-ui/screens/review-workspace.md`

---

# Purpose

The Review Workspace validates AI-extracted metadata before knowledge is committed.

It is the primary human-in-the-loop workspace.

---

# Layout

```text
Workspace Header
Queue Toolbar
Review Queue | Document Viewer | Metadata | AI Context
Evidence Panel
Action Bar
```

Panels may be resizable.

---

# Material UI Components

- MUI X `DataGrid` for queue
- IKMS `DocumentViewer`
- MUI form controls for metadata
- MUI X `DataGrid` or structured table for evidence
- `ButtonGroup`, `IconButton`, `Tooltip` for viewer controls
- `Dialog` for reject, reassign and escalation
- Bordered `Box` for AI context
- Shared split-layout component only for panel resizing

Do not recreate standard controls inside custom panels.

---

# Review Queue

Columns:

| Column | Width |
|---|---:|
| Priority | 40 |
| Document | 280 |
| Customer | 220 |
| Type | 120 |
| AI Confidence | 120 |
| Assigned To | 160 |
| Status | 120 |
| Received | 150 |

Actions:

- Open
- Assign
- Reassign
- Skip
- Escalate

Double-click opens the review item.

---

# Document Viewer

Supports:

- PDF
- Image
- Word
- Email

Controls:

- Zoom In
- Zoom Out
- Fit Width
- Fit Page
- Rotate
- Previous Page
- Next Page
- Search
- OCR Layer
- Download

Use MUI buttons, button groups, tooltips and icons.

The viewer is the primary region.

---

# OCR and Evidence Synchronization

- Selecting metadata highlights source evidence.
- Selecting evidence highlights the document region.
- OCR overlay remains optional.
- Low-confidence regions remain visible.
- Evidence is visible before approval.

---

# Metadata Editor

Use:

- `TextField`
- `Select`
- `Autocomplete`
- `Checkbox`
- Date controls already approved in the project
- `FormHelperText`

Groups:

- Customer
- Document
- References
- Classification

Validation:

- Required fields
- Format validation
- Duplicate detection
- Business rules
- Inline errors

AI never commits metadata automatically.

---

# Evidence DataGrid

| Column | Width |
|---|---:|
| Field | 180 |
| Extracted Value | 260 |
| Source Text | 320 |
| Confidence | 120 |

Selecting a row highlights the source region.

---

# AI Context

Sections:

- Document Summary
- Business Context
- Important Facts
- Missing Metadata
- Incorrect Classification
- Duplicate Warning
- Similar Documents
- Confidence
- Warnings

Use MUI `Alert`, `Chip`, `Typography`, `Divider` and icons.

---

# Action Bar

| Action | Component |
|---|---|
| Save | `Button variant="outlined"` |
| Approve | `Button variant="contained"` |
| Reject | `Button color="error"` |
| Reassign | `Button` + `Dialog` |
| Escalate | `Button` + `Dialog` |

Maximum two visually primary actions.

---

# Lifecycle

```text
Queue
↓
Open
↓
Validate Metadata
↓
Review AI
↓
Verify Evidence
↓
Approve / Reject / Escalate
```

---

# States

- New
- Assigned
- In Review
- Approved
- Rejected
- Escalated
- Restricted
- Loading
- Error

---

# Keyboard

| Shortcut | Action |
|---|---|
| Ctrl+S | Save |
| Ctrl+Enter | Approve |
| Ctrl+R | Reject |
| Ctrl+Right | Next |
| Ctrl+Left | Previous |
| Tab | Next field |
| Shift+Tab | Previous field |
| F | Search OCR |

---

# Performance

- Queue loads independently.
- Viewer streams pages.
- Metadata loads asynchronously.
- AI context updates independently.
- Viewer controls remain responsive during AI processing.

---

# Codex Rules

- Use MUI X DataGrid for queue and evidence.
- Use standard MUI form controls.
- DocumentViewer is custom but composed from MUI.
- Keep metadata, viewer and evidence synchronized.
- AI never commits.
- Evidence appears before approval.
- Preserve queue position.
- Use shared theme and icons.
