# IKMS UI Flow Map

**Version:** 3.1  
**Purpose:** Authoritative navigation and state-transition map for IKMS workspaces.

---

# 1. Source of Truth

Functional behaviour and permissions:

- `specs/001-insurance-broker-ikms/contracts/ui.md`

Design and interaction:

- `docs/06-ui/01-design-principles.md`
- `docs/06-ui/02-ux-rules.md`
- `docs/06-ui/03-ui-design-guidelines.md`
- `docs/06-ui/05-workspace-catalog.md`
- `docs/06-ui/06-design-tokens.md`
- Files under `docs/06-ui/screens/`

When guidance conflicts:

1. Functional behaviour and permissions come from `specs/`.
2. Workspace-specific rules override generic UI rules.
3. `06-design-tokens.md` controls Material UI theme values.
4. Do not infer new scope.

---

# 2. Primary Application Flow

```text
Login
  │
  ▼
Search Workspace
  │
  ├──────────────► Customer360
  │                    │
  │                    ├──► Documents
  │                    ├──► Emails
  │                    ├──► Notes
  │                    ├──► Relationships
  │                    ├──► Policy References
  │                    ├──► Claim References
  │                    └──► Timeline
  │
  ├──────────────► Review Workspace
  │                    │
  │                    ├──► Queue
  │                    ├──► Document Viewer
  │                    ├──► Metadata
  │                    ├──► Evidence
  │                    ├──► AI Context
  │                    └──► Review Actions
  │
  ├──────────────► Administration
  │                    │
  │                    ├──► Configuration Explorer
  │                    ├──► Configuration Grid
  │                    └──► Editor Drawer
  │
  └──────────────► Audit
                       │
                       ├──► Search and Filters
                       ├──► Audit Grid
                       ├──► Event Details
                       ├──► Timeline
                       ├──► Evidence
                       └──► Export
```

---

# 3. Authentication Flow

```text
Unauthenticated Route
↓
Login
↓
Authentication Success
↓
Search Workspace
```

Failure:

```text
Login
↓
Invalid Credentials or System Error
↓
Inline Error
↓
Retry
```

Rules:

- Search is the authenticated landing page.
- Preserve requested protected route only when supported by the authentication contract.
- Login uses the same Material UI theme.
- No marketing page is inserted between login and Search.

---

# 4. Search Flow

```text
Search Workspace
↓
Enter Query
↓
Apply Filters
↓
View Results Grid
↓
Select Result
↓
Preview or Open
```

Result navigation:

```text
Customer Result
↓
Customer360
```

```text
Document Result
↓
Document Preview or Review Workspace
```

```text
Email or Note Result
↓
Customer360 Relevant Tab or Detail View
```

```text
Policy or Claim Reference
↓
Customer360 Reference Tab
↓
External System of Record when opened
```

Preserve:

- Query
- Filters
- Sorting
- Selected row
- Scroll position
- Saved-search context

Back navigation returns to the same Search state.

---

# 5. Continue Working Flow

```text
Search Home
↓
Continue Recent Work Grid/List
↓
Open Previous Customer, Document or Review
```

Rules:

- Continue Working uses structured rows, not cards.
- Recent state must be permission-trimmed.
- Returning to Search restores the previous home state.

---

# 6. Customer360 Flow

```text
Search
↓
Customer Result
↓
Customer360
↓
Select Tab
↓
Select Record
↓
Open Detail or Review
```

Tabs:

```text
Documents
Emails
Notes
Relationships
Policy References
Claim References
Timeline
```

Preserve:

- Active customer
- Active tab
- Tab-specific grid state
- Selected row
- Scroll position
- Right context-panel state

---

# 7. Customer Document Flow

```text
Customer360
↓
Documents Tab
↓
Select Document
↓
Preview
↓
Open Review Workspace
```

Return:

```text
Review Workspace
↓
Complete, Cancel or Back
↓
Customer360 Documents Tab
```

Restore:

- Customer
- Documents tab
- Grid filters
- Selected document
- Scroll position

---

# 8. Email and Note Flow

```text
Customer360
↓
Emails or Notes Tab
↓
Select Record
↓
Open Detail
```

Editing:

```text
Notes Tab
↓
Create or Edit
↓
Drawer
↓
Save or Cancel
↓
Return to Notes Grid
```

Rules:

- Use Drawer for secondary editing.
- Preserve tab and grid state.
- Do not navigate to an unrelated full-page CRUD form.

---

# 9. Relationship Flow

```text
Customer360
↓
Relationships Tab
↓
Select Related Customer
↓
Open Related Customer360
```

Back:

```text
Related Customer360
↓
Back
↓
Original Customer360 Relationships Tab
```

Preserve the originating customer context in navigation history.

---

# 10. Policy and Claim Reference Flow

```text
Customer360
↓
Policy or Claim References Tab
↓
Select Reference
↓
View Reference Details
↓
Open External System of Record
```

Rules:

- IKMS reference data is read-only.
- External navigation is explicit.
- Returning to IKMS restores the original customer and tab.

---

# 11. Review Queue Flow

```text
Review Workspace
↓
Queue Scope
  ├── My Queue
  ├── Team Queue
  ├── Unassigned
  └── High Priority
↓
Select Item
↓
Load Viewer, Metadata, Evidence and AI Context
```

Preserve:

- Queue scope
- Filters
- Sorting
- Selected item
- Queue scroll position
- Panel sizes

---

# 12. Review Decision Flow

```text
Open Review Item
↓
Inspect Document
↓
Validate Metadata
↓
Inspect Evidence
↓
Review AI Suggestions
↓
Choose Action
```

Approve:

```text
Approve
↓
Validate Required Fields
↓
Commit Review
↓
Success Feedback
↓
Next Item or Queue
```

Reject:

```text
Reject
↓
Reason Dialog
↓
Confirm
↓
Update Status
↓
Next Item or Queue
```

Reassign:

```text
Reassign
↓
Assignment Dialog
↓
Select User or Team
↓
Confirm
↓
Return to Queue
```

Escalate:

```text
Escalate
↓
Escalation Dialog
↓
Reason and Destination
↓
Confirm
↓
Return to Queue
```

Rules:

- Evidence is available before approval.
- AI never commits decisions.
- Auto-load next item only when user preference or workflow supports it.
- Queue position remains recoverable.

---

# 13. Review Evidence Interaction

```text
Select Metadata Field
↓
Highlight Evidence Row
↓
Highlight Document Region
```

```text
Select Evidence Row
↓
Highlight Document Region
↓
Focus Related Metadata Field
```

Rules:

- Viewer, metadata and evidence remain synchronized.
- Missing evidence is shown explicitly.
- Low-confidence evidence remains visible.
- Restricted evidence is not revealed.

---

# 14. Administration Flow

```text
Administration
↓
Select Explorer Node
↓
Load Configuration Grid
↓
Select Configuration
↓
Open Editor Drawer
```

Create or edit:

```text
Configuration Grid
↓
Create or Edit
↓
Editor Drawer
↓
Validate
↓
Save
↓
Close Drawer
↓
Return to Same Grid State
```

Delete or disable:

```text
Configuration Grid
↓
Delete or Disable
↓
Confirmation Dialog
↓
Confirm
↓
Refresh Affected Grid
```

Preserve:

- Explorer node
- Grid filters
- Sorting
- Selected record
- Scroll position

History remains read-only.

---

# 15. Audit Flow

```text
Audit Workspace
↓
Enter Search Criteria
↓
Apply Filters
↓
View Audit Grid
↓
Select Event
↓
View Event Details, Timeline and Evidence
```

Export:

```text
Audit Grid
↓
Export
↓
Choose Format
↓
Permission Check
↓
Generate Export
↓
Download or Background Completion
```

Preserve:

- Search
- Date range
- Filters
- Sorting
- Selected event
- Grid position

Audit records are immutable.

---

# 16. Global Search Access

```text
Any Authenticated Workspace
↓
Global Search Action
↓
Search Workspace
```

Rules:

- Preserve the originating workspace in navigation history.
- Search opens with the entered query when launched from global search.
- Returning restores the originating workspace state.

---

# 17. Right Context Panel Flow

```text
Select Primary Record
↓
Load Context Panel
  ├── AI Brief
  ├── Evidence
  ├── Activity
  ├── Related Records
  └── Quick Actions
```

Collapse:

```text
Context Panel
↓
Collapse
↓
Main Region Expands
```

Rules:

- Preserve collapse state where appropriate.
- Context loads independently.
- Context loading must not block the primary region.
- Primary collections never move into the context panel.

---

# 18. Drawer Flow

Use Drawer for:

- Note editing
- Configuration editing
- Secondary details
- Non-primary metadata editing

```text
Trigger Action
↓
Open Right Drawer
↓
Edit
↓
Save or Cancel
↓
Return Focus to Trigger
```

Unsaved changes:

```text
Close Drawer
↓
Unsaved Changes Detected
↓
Confirmation Dialog
↓
Discard or Continue Editing
```

---

# 19. Dialog Flow

Use Dialog for:

- Confirmation
- Assignment
- Conflict
- Rejection reason
- Escalation
- Destructive action

```text
Trigger
↓
Dialog
↓
Confirm or Cancel
↓
Return Focus to Trigger
```

Do not use Dialog for large operational forms.

---

# 20. Restricted Information Flow

```text
User Opens Record
↓
Permission Check
↓
Authorized?
```

Authorized:

```text
Display Allowed Content
```

Restricted:

```text
Display Restricted State
↓
Do Not Reveal Content
↓
Do Not Pass Restricted Content to AI
```

Rules:

- Security trimming occurs before search ranking and AI context.
- Restricted states appear intentional.
- Do not expose metadata derived from restricted content.

---

# 21. Loading Flow

Each region loads independently.

```text
Workspace Shell
↓
Primary Grid
↓
Selected Record Detail
↓
Context Panel
```

Rules:

- Keep available regions usable.
- Preserve previous data during refresh when safe.
- Show progress for uploads and long-running exports.
- AI loading does not block the workspace.

---

# 22. Error Recovery Flow

```text
Operation
↓
Error
↓
User-Readable Message
↓
Retry or Recovery Action
```

Rules:

- Keep unaffected regions available.
- Preserve user input where possible.
- Do not expose stack traces.
- Include support reference when available.

---

# 23. Back Navigation Rules

Back navigation must restore the previous operational state.

Restore where possible:

- Workspace
- Customer
- Search query
- Filters
- Sorting
- Selected row
- Active tab
- Scroll position
- Panel sizes
- Context-panel state

Do not return users to an empty default workspace when previous state is available.

---

# 24. Browser Refresh Rules

On refresh:

- Restore authenticated route
- Restore route parameters
- Reload authorized data
- Restore recoverable workspace state
- Do not restore sensitive transient content from insecure storage
- Do not duplicate actions or submissions

---

# 25. Deep Link Rules

Deep links may target:

- Customer360 customer
- Document review item
- Audit event
- Administration configuration where authorized

Deep links must:

- Require authentication
- Apply permission checks
- Load the correct workspace
- Show restricted state when unauthorized
- Provide safe navigation back to Search

---

# 26. Role-Based Primary Flows

## Processor

```text
Search
↓
Customer360
↓
Documents / Emails / Notes / AI Brief
```

## Indexer

```text
Review Queue
↓
Document
↓
Metadata
↓
Evidence
↓
Approve / Reject
```

## Supervisor

```text
Search or Review
↓
Customer360 / Queue
↓
PII or Original Document Access
↓
Escalation and Oversight
```

## Compliance

```text
Audit
↓
Search
↓
Event
↓
Timeline
↓
Evidence
↓
Export
```

## Administrator

```text
Administration
↓
Explorer
↓
Grid
↓
Editor Drawer
```

---

# 27. Navigation Acceptance Rules

The implemented UI must ensure:

- Search is the authenticated home.
- Customer context is preserved.
- Search state is restored.
- Review queue state is preserved.
- Drawers return users to the originating workspace.
- Dialogs return focus to the triggering action.
- Operational collections use grids or structured lists.
- Policy and claim references remain external-system references.
- Audit is immutable.
- AI remains contextual.
- Restricted data never enters search results or AI context.
- Back navigation restores operational state.
- No unnecessary dashboard or CRUD navigation is introduced.
