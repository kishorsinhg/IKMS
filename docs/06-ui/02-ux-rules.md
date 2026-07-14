# IKMS UX Rules

**Version:** 1.1  
**Status:** Approved  
**Purpose:** Operational UX rules for IKMS.

---

# 1. Purpose

This document defines operational UX behaviour for IKMS.

It complements:

- `01-design-principles.md`
- `03-ui-design-guidelines.md`
- `04-ui-flow-map.md`
- `05-workspace-catalog.md`
- `06-design-tokens.md`

It does not redefine:

- Colors
- Typography
- Component styling
- Material UI theme values
- Workspace-specific layouts

---

# 2. Product Philosophy

IKMS is an AI-powered Enterprise Knowledge Workspace.

It is not:

- Broker Management System
- Policy Administration System
- Claims Processing System
- CRM
- Dashboard

Those remain the customer's existing systems of record.

IKMS connects:

- Documents
- Emails
- Notes
- Knowledge
- AI
- Search
- Evidence
- Metadata
- Relationships
- Policy References
- Claim References

---

# 3. Primary User Goal

Users come to IKMS to:

- Understand a customer
- Find information
- Connect knowledge
- Review evidence
- Validate metadata
- Make informed decisions

Users do not come to IKMS to administer policies or claims.

Every workflow must support these goals.

---

# 4. Workspace Philosophy

IKMS is built around workspaces.

Do not design around CRUD pages.

Primary workspaces:

- Search
- Customer360
- Review
- Administration
- Audit

Each workspace must solve one operational problem.

---

# 5. Material UI

Material UI is the standard UI component foundation.

UX rules define:

- User behaviour
- Navigation
- Interaction
- Context
- Workflow expectations

Component implementation is defined in:

- `03-ui-design-guidelines.md`
- `06-design-tokens.md`

Use standard Material UI behaviour wherever possible.

Do not redefine standard Material UI interactions in this document.

Do not create custom replacements for standard controls unless required by documented business behaviour.

---

# 6. Home Experience

The authenticated landing page is the Search Workspace.

Do not create a traditional dashboard.

The home experience prioritizes:

1. Global Search
2. Continue Recent Work
3. Work Requiring Attention
4. Recent Activity
5. Quick Actions

Continue Recent Work and Recent Activity must use structured lists or grids, not cards.

---

# 7. Search Rules

Search is the primary interaction model.

Users should not need to know where information is stored.

Search returns authorized results from:

- Customers
- Documents
- Emails
- Notes
- Metadata
- Knowledge
- Policy References
- Claim References

Search must support:

- Natural-language queries
- Structured filters
- Saved searches
- Recent searches
- Keyboard focus
- Result-type identification
- State preservation

Examples:

- John Smith
- Motor policy for John
- Emails from ABC Manufacturing
- Documents awaiting review

Search results must use MUI X DataGrid, a structured table, or a structured list.

Do not render search results as cards.

Restricted results must appear intentionally restricted without exposing content.

---

# 8. Customer360 Rules

Customer360 is the primary customer workspace.

Customer context remains visible.

Customer360 provides:

- AI Brief
- Customer Summary
- Documents
- Emails
- Notes
- Relationships
- Policy References
- Claim References
- Timeline

The user should rarely leave Customer360 unnecessarily.

Preserve:

- Active customer
- Active tab
- Grid filters
- Sorting
- Selected row
- Scroll position

Policy and claim references remain links to the existing system of record.

---

# 9. Review Rules

Review is the primary human-in-the-loop workspace.

The workflow is:

```text
Queue
↓
Document Viewer
↓
Metadata
↓
Evidence
↓
AI Recommendation
↓
User Decision
```

Users must be able to:

- Review the source document
- Validate extracted metadata
- See evidence
- See AI confidence
- Correct values
- Approve
- Reject
- Reassign
- Escalate

AI must not automatically commit review decisions.

Evidence must be available before approval.

Queue position should be preserved.

---

# 10. AI Rules

AI behaves as an experienced insurance operations colleague.

AI should:

- Brief users
- Explain changes
- Surface related knowledge
- Highlight inconsistencies
- Recommend next actions
- Identify missing information
- Cite evidence
- Expose confidence

AI should never:

- Approve claims
- Approve policies
- Replace human decisions
- Invent facts
- Hide uncertainty
- Commit metadata without review

Every important AI answer must link to supporting evidence.

AI appears in the right context panel or relevant workspace region.

Do not use a floating chatbot as the primary interaction.

---

# 11. Knowledge Rules

IKMS manages knowledge, not transactions.

Knowledge sources include:

- Documents
- Emails
- Notes
- OCR
- Metadata
- Relationships
- AI
- Policy References
- Claim References

The purpose is to transform scattered knowledge into contextual understanding.

---

# 12. Information Density

IKMS is an operational application.

Prefer:

- Data grids
- Structured tables
- Compact layouts
- Persistent filters
- Split panels
- Keyboard shortcuts
- Sticky headers
- Resizable regions
- Inline actions

Avoid:

- Large empty cards
- Decorative dashboards
- Oversized whitespace
- Wizard-driven workflows
- Marketing layouts
- Large rounded containers
- Excessive vertical scrolling

---

# 13. Forms

Forms must not dominate the workspace.

Preferred pattern:

```text
Create or Edit Action
↓
Drawer or Secondary Panel
↓
Save or Cancel
↓
Return to Workspace
```

Use standard Material UI controls.

Use:

- `TextField`
- `Select`
- `Autocomplete`
- `Checkbox`
- `Radio`
- `Switch`
- Approved date controls

Use Dialog only for:

- Confirmation
- Assignment
- Conflict
- Destructive action

Do not place large operational forms inside Dialog.

---

# 14. Operational Collections

Operational collections should use:

- MUI X DataGrid
- Structured tables
- Tree views
- Structured lists

Examples:

- Customers
- Documents
- Emails
- Notes
- Relationships
- Search Results
- Review Queue
- Audit Events
- Configuration Records

Cards are allowed only for:

- Summary
- AI Brief
- Alerts
- Evidence Summary
- Quick Actions
- Warnings

Do not use cards for collections.

---

# 15. Navigation

Desktop-first primary navigation:

- Search
- Customer360
- Review
- Administration
- Audit

Do not create unnecessary top-level modules.

Customer360 contains:

- Documents
- Emails
- Notes
- Relationships
- Policy References
- Claim References
- Timeline

Use Material UI navigation primitives and the shared application shell.

---

# 16. Context Preservation

Never lose customer or search context unnecessarily.

Preserve:

- Active customer
- Search query
- Filters
- Sorting
- Selected row
- Active tab
- Scroll position
- Resized panels
- Context-panel state

Example:

```text
Search
↓
Customer
↓
Document
↓
Review
↓
Return to Customer
```

The selected customer remains active.

---

# 17. Action Placement

Use predictable locations.

| Action Type | Location |
|---|---|
| Workspace action | Workspace header |
| Search and filter | Toolbar |
| Bulk action | Grid toolbar |
| Row action | Row menu |
| AI context | Right context panel |
| Secondary edit | Drawer |
| Confirmation | Dialog |

Equivalent actions must remain in equivalent locations.

---

# 18. Empty States

Never show only:

```text
No data.
```

Every empty state explains:

- Why the workspace is empty
- What the user can do next
- The available recovery action

Do not use decorative illustrations.

---

# 19. Loading Rules

Every asynchronous operation requires a visible loading state.

Use:

- DataGrid loading overlay
- Skeleton
- Circular progress
- Linear progress
- Inline action spinner

Load workspace regions independently.

Do not block the entire workspace when one panel is loading.

Uploads require progress indicators.

---

# 20. Error Handling

Every asynchronous operation requires:

- Error message
- Recovery action
- Retry where appropriate
- Support reference where available

Do not expose stack traces.

Keep unaffected workspace regions available.

---

# 21. Success Feedback

Use:

- Snackbar
- Inline confirmation
- Updated status
- Progress completion

Do not show duplicate notifications for the same action.

Critical outcomes must remain visible until acknowledged.

---

# 22. Security and Permission UX

Never expose unauthorized data.

If restricted information exists, display:

```text
Restricted Information Available
```

Do not reveal:

- Content
- Metadata
- Source text
- AI-derived facts from restricted content

Actions must render according to permissions.

Disabled and hidden actions must follow the functional UI contract.

Restricted states must appear intentional, not broken.

---

# 23. Accessibility

Every workspace must support:

- Keyboard navigation
- Visible focus
- Accessible labels
- Screen-reader semantics
- WCAG AA contrast
- Status communication beyond color
- Tooltips for icon-only actions
- Predictable tab order

Use Material UI accessibility behaviour instead of replacing it with custom controls.

---

# 24. Keyboard UX

Frequent operations should support keyboard interaction.

Recommended:

| Shortcut | Action |
|---|---|
| Ctrl+K | Search |
| Ctrl+S | Save |
| Enter | Open or confirm |
| Space | Select |
| Esc | Close or clear |
| Arrow keys | Navigate |
| Tab | Next control |
| Shift+Tab | Previous control |

Workspace-specific shortcuts are defined in workspace specifications.

---

# 25. Performance

Users should begin working within three seconds where technically feasible.

Search receives implementation priority.

Prefer:

- Virtualized grids
- Incremental loading
- Lazy loading
- Preserved previous content
- Independent panel loading
- Cached workspace state

Do not block navigation while AI processing continues.

---

# 26. Responsive Desktop Behaviour

Primary target:

- 1920 × 1080

Supported:

- 1600 × 900
- 1440 × 900
- 1366 × 768
- Minimum 1280px width

When space is constrained:

1. Collapse the right context panel
2. Collapse navigation
3. Preserve grids
4. Allow horizontal scrolling

Do not convert operational grids into cards.

---

# 27. Acceptance Checklist

Every workspace must satisfy:

- Primary task is obvious
- Search is available where required
- Customer context is preserved
- AI is contextual
- Evidence is available
- Operational density is maintained
- Navigation is minimal
- Authorization is respected
- Collections use grids or structured lists
- Standard MUI controls are reused
- Loading, empty, error and restricted states exist
- Keyboard navigation works
- No unnecessary dashboard
- No card-based operational collection
- No floating chatbot
- No lost workspace state

---

# 28. Guiding Principle

IKMS exists to transform scattered knowledge into meaningful customer understanding so insurance professionals can work faster and make better decisions.
