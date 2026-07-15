# IKMS Workspace Catalog

**Version:** 3.1  
**Purpose:** Standard workspace composition for IKMS.  
**Audience:** Product, UX, Frontend Developers, AI Coding Agents.

---

# 1. Standard Workspace

Every workspace follows:

```text
+-------------------------------------------------------------+
| Workspace Header                                            |
+-------------------------------------------------------------+
| Toolbar / Search                                            |
+------------------------------+------------------------------+
| Main Operational Region      | Right Context Panel          |
+------------------------------+------------------------------+
```

Use Material UI primitives and shared IKMS workspace components.

---

# 2. Material UI Composition

| Workspace Element | Standard Component |
|---|---|
| Application layout | `Box`, `Stack`, CSS Grid |
| Workspace header | `Box`, `Stack`, `Breadcrumbs`, `Typography` |
| Toolbar | `Toolbar`, `Button`, `IconButton`, `Tooltip` |
| Operational collection | MUI X `DataGrid` |
| Tabs | `Tabs`, `Tab` |
| Context panel | Bordered `Box` or `Drawer` |
| Configuration explorer | MUI X `TreeView` |
| Forms | `TextField`, `Select`, `Autocomplete`, `Checkbox` |
| Confirmation | `Dialog` |
| Secondary editor | `Drawer` |
| Status | `Chip` |
| Loading | `Skeleton`, `CircularProgress`, `LinearProgress` |
| Alert | `Alert` |
| Notification | `Snackbar` |
| Row actions | `Menu`, `MenuItem` |

Do not use `Card` for operational collections.

---

# 3. Login

## Purpose

Authenticate users.

## Structure

```text
IKMS Logo
Username / Email
Password
Remember Me
Sign In
Forgot Password
```

## MUI Composition

- `Paper` or bordered `Box`
- `TextField`
- `Checkbox`
- `Button`
- `Link`
- `Alert`
- `CircularProgress`

## Rules

- Professional and minimal
- Same theme as application
- No marketing content
- No illustration
- No gradient
- No feature cards

---

# 4. Search Workspace

## Purpose

Authenticated landing workspace.

## Structure

```text
Workspace Header
Search Toolbar
Saved Searches / Active Filters
Search Results DataGrid
Optional Preview
Right Context Panel
```

## MUI Composition

- `TextField` or `Autocomplete`
- `Toolbar`
- `Chip`
- MUI X `DataGrid`
- `Menu`
- `Drawer` or bordered context `Box`

## Context

- AI Brief
- Search Insights
- Evidence
- Related Records
- Recent Activity
- Quick Actions

## Rules

- Search dominates the page.
- Results never use cards.
- Preserve search and grid state.
- Double-click or Enter opens the selected record.

---

# 5. Customer360 Workspace

## Purpose

Persistent customer knowledge workspace.

## Structure

```text
Workspace Header
Persistent Customer Summary
Tabs
Active Tab DataGrid
Right Context Panel
```

## Tabs

- Documents
- Emails
- Notes
- Relationships
- Policy References
- Claim References
- Timeline

## MUI Composition

- `Paper` or bordered `Box` for customer summary
- `Tabs`, `Tab`
- MUI X `DataGrid`
- `Chip`
- Context `Box` or `Drawer`

## Rules

- Customer context remains visible.
- Every operational tab uses a grid.
- Policy and claim references remain read-only links to the system of record.
- Policy and Claim are external references or metadata within IKMS. The broker management system remains the system of record.
- Do not convert customer summary into dashboard cards.

---

# 6. Review Workspace

## Purpose

Human-in-the-loop validation.

## Structure

```text
Review Queue
Document Viewer
Metadata Editor
Evidence
AI Recommendations
Action Bar
```

## MUI Composition

- MUI X `DataGrid` for queue
- IKMS `DocumentViewer`
- MUI form controls
- MUI X `DataGrid` or structured table for evidence
- `ButtonGroup`, `IconButton`, `Tooltip`
- Bordered context `Box`
- `Dialog` for reject or reassign confirmation

## Rules

- Viewer is primary.
- Metadata and evidence remain synchronized.
- AI suggests but never commits.
- Evidence appears before approval.
- Preserve queue position.

---

# 7. Knowledge Quality Workspace

## Purpose

Guide data stewards through customer-centric knowledge quality issues.

## Structure

---

# 8. Administration Workspace

## Purpose

Manage configuration, governance, and enterprise operations without redesigning the shared workspace model.

## Structure

```text
Configuration Explorer
Workspace Toolbar
Administration Grid
Right Context Panel
Editor / Detail Drawer
```

## Operational Modules

- Background Jobs
- Queues
- Scheduler
- Embeddings
- OCR
- AI Operations
- Cache
- Health
- Diagnostics

## Rules

- Reuse `EntityGrid`, `WorkspaceToolbar`, `RightContextPanel`, `StatusBadge`, and responsive drawer behavior.
- Operational controls are administrative actions, not policy or claim workflows.
- Customer remains the primary business context even when running platform operations.

```text
Workspace Header
Quality Toolbar
Customer Quality Grid
Selected Steward Queue
Quality Score Breakdown
Right Context Panel
```

## Rules

- Customer remains the primary business context.
- Policy Number and Claim Number remain Business Reference Fields and may explain quality issues, but they do not create Policy or Claim workspaces.
- Steward actions are corrective and auditable.
- Revalidation and reindex actions must remain explicit.

---

# 8. Administration Workspace

## Purpose

Manage configuration.

## Structure

```text
Configuration Explorer
Configuration DataGrid
Editor Drawer
```

## MUI Composition

- MUI X `TreeView`
- MUI X `DataGrid`
- `Drawer`
- Standard MUI form controls
- `Dialog` for destructive confirmation

## Rules

- Use Explorer + Grid + Editor.
- Avoid one long settings form.
- Preserve selected node and grid state.
- History is read-only.

---

# 8. Audit Workspace

## Purpose

Search and inspect immutable audit history.

## Structure

```text
Audit Search / Filters
Audit Events DataGrid
Event Details
Timeline
Evidence
Export
```

## MUI Composition

- Standard MUI filters
- MUI X `DataGrid`
- Right `Drawer` or persistent bordered panel
- IKMS `AuditTimeline`
- `Menu` for export formats
- `Chip` for result state

## Rules

- Audit records are immutable.
- Timeline and evidence are read-only.
- Export respects permissions.
- AI may summarize but never modify records.

---

# 9. Right Context Panel

Standard sections:

- AI Brief
- Recommendations
- Warnings
- Missing Information
- Evidence
- Related Records
- Activity
- Quick Actions

Use compact bordered sections.

Do not use a collection of oversized cards.

---

# 10. Standard Actions

| Action | MUI Component |
|---|---|
| Open | `Button` or `MenuItem` |
| Edit | `Button`, `IconButton` |
| Assign | `Button`, `Dialog` |
| Review | `Button` |
| Download | `Button`, `IconButton` |
| Export | `Button`, `Menu` |
| Refresh | `IconButton` |
| Delete | `Button color="error"` + `Dialog` |
| More | `IconButton` + `Menu` |

---

# 11. Standard States

Every workspace supports:

- Populated
- Loading
- Empty
- Error
- Restricted
- Long-content
- Selected-row
- Active-filter

Use shared state components composed from MUI.

---

# 12. Navigation

Standard flows:

```text
Search
↓
Customer360
↓
Document
↓
Review
↓
Return to Customer
```

```text
Search
↓
Document
↓
Review
↓
Audit
```

Preserve:

- Search
- Filters
- Sorting
- Selection
- Active tab
- Scroll position
- Panel state

---

# 13. Component Usage Matrix

| Component | Usage |
|---|---|
| MUI X DataGrid | Customers, Documents, Emails, Notes, References, Queues, Audit |
| MUI form controls | Metadata, filters, configuration |
| Tabs | Customer360 and peer views |
| TreeView | Administration explorer |
| Drawer | Secondary editor and detail |
| Dialog | Confirmation, assignment, conflict |
| Bordered Box/Paper | Structural surface |
| Card | Context only |
| Chip | Status |
| Snackbar | Notification |
| Alert | Inline issue |
| Skeleton | Loading |

---

# 14. Codex Rules

- Use Material UI as the standard component foundation.
- Use only the installed MUI packages and licence tier.
- Reuse shared IKMS MUI-based components.
- Do not recreate standard MUI controls.
- Do not use cards for operational collections.
- Keep AI in the contextual workspace region.
- Preserve workspace state.
- Follow the shared theme and design tokens.
- Maintain compact enterprise density.
