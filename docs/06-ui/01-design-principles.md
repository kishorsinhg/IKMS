# IKMS UI Design Principles

**Version:** 3.1  
**Purpose:** Mandatory UI principles for all IKMS workspaces.  
**Audience:** Product, UX, Frontend Developers, AI Coding Agents.

---

# 1. Product Identity

IKMS is an **AI-powered Enterprise Knowledge Workspace** for insurance brokers.

It complements the broker's existing systems of record.

IKMS manages operational knowledge including:

- Documents
- Emails
- Notes
- AI summaries
- Metadata
- Relationships
- Policy references
- Claim references
- Evidence

IKMS is not:

- A policy administration system
- A claims system
- A CRM
- A BI dashboard
- A marketing application

---

# 2. Primary Goal

Enable operational users to locate, review, validate and manage knowledge with minimum effort.

Every UI decision must improve:

- Findability
- Readability
- Consistency
- Operational speed
- Decision support

---

# 3. Target Users

Primary users:

- Processor
- Indexer
- Supervisor
- Compliance
- Administrator

Usage characteristics:

- Desktop-first
- Keyboard-heavy
- High-volume processing
- 6–8 hours of daily usage

---

# 4. Material UI Foundation

Material UI is the standard frontend component foundation for IKMS.

Use Material UI for standard controls, including:

- Buttons
- Icon buttons
- Inputs
- Selects
- Autocomplete
- Checkboxes
- Radio buttons
- Switches
- Menus
- Dialogs
- Drawers
- Tabs
- Tooltips
- Alerts
- Snackbars
- Skeletons
- Progress indicators
- Pagination
- Data grids
- Tree views

IKMS defines:

- Material theme configuration
- Enterprise density
- Application shell
- Workspace layouts
- Business-specific components
- AI and evidence patterns

Do not recreate a standard Material UI component unless:

- Material UI cannot meet a documented requirement
- Accessibility would be reduced
- A business-specific interaction requires custom behaviour

Custom IKMS components should compose Material UI primitives.

---

# 5. Design Philosophy

IKMS is a modern Enterprise Workspace.

Target characteristics:

- Compact
- Structured
- Professional
- Predictable
- Neutral
- Information-dense
- Minimal decoration

Reference products:

- OpenText Content Suite
- ServiceNow Workspace
- Salesforce Service Console
- Guidewire
- Outlook
- Azure Portal
- Jira
- VS Code

---

# 6. Search First

Search is the authenticated landing page.

Users must be able to immediately:

- Search
- Filter
- Continue work
- Open recent activity
- Navigate to customer or document context

Search must remain accessible from every workspace.

---

# 7. Grid First

Operational collections must use:

- MUI X Data Grid
- Table
- Tree view
- Structured list

Collections include:

- Customers
- Documents
- Emails
- Notes
- Search results
- Review queues
- Intake queues
- Activities
- Relationships
- Policy references
- Claim references
- Audit events
- Administration records

Do not use cards for operational collections.

---

# 8. Card Usage

Cards are allowed only for contextual content.

Allowed:

- AI Brief
- Alerts
- Summary
- Quick Actions
- Evidence Summary
- Warnings

Not allowed:

- Customers
- Documents
- Emails
- Notes
- Search Results
- Queues
- Audit Events
- Policy References
- Claim References

Use MUI `Card` sparingly. Prefer bordered `Box` or `Paper` for structural regions.

---

# 9. Standard Workspace Pattern

Every workspace follows:

```text
Workspace Header
↓
Toolbar / Search
↓
Main Operational Region
↓
Optional Right Context Panel
```

The layout remains consistent across:

- Search
- Customer360
- Review
- Administration
- Audit

---

# 10. Context First

Primary work occupies most of the screen.

Secondary context belongs in the right context panel.

Typical context:

- AI Brief
- Activity
- Alerts
- Evidence
- Review History
- Related Records
- Quick Actions

The context panel must not replace the primary workspace.

---

# 11. AI Assisted

AI behaves as an operational colleague.

Allowed:

- Summaries
- Recommendations
- Warnings
- Evidence
- Missing metadata
- Relationship insights
- What changed
- What needs attention

Not allowed:

- Floating chatbot as the primary interface
- Autonomous operational decisions
- Unsupported conclusions
- Hidden evidence

---

# 12. Human Controlled

Users approve:

- Metadata
- Classification
- Review outcomes
- Final actions

AI suggestions require human review.

---

# 13. Evidence Before Decision

Present information in this order:

```text
Evidence
↓
AI Recommendation
↓
User Action
```

Every important AI recommendation should expose:

- Source
- Reason
- Confidence
- Missing information

---

# 14. Information Density

Prefer:

- Compact spacing
- Small controls
- Multiple visible rows
- Sticky headers
- Persistent toolbars
- Minimal scrolling
- Split views

Avoid:

- Oversized controls
- Excessive whitespace
- Large rounded containers
- Decorative empty sections

---

# 15. Visual Language

Use:

- Inter typography
- Navy, slate, neutral and blue palette
- Thin borders
- Minimal shadows
- Small radius
- Restrained status colors
- Consistent outlined icons

Avoid:

- Gradients
- Glassmorphism
- Marketing layouts
- Decorative motion
- Bright accent colors
- Emoji as interface icons

---

# 16. Consistency

Equivalent actions remain in equivalent locations.

| Element | Standard Location |
|---|---|
| Search | Toolbar |
| Filters | Toolbar |
| Bulk actions | Above grid |
| Workspace actions | Header |
| AI Brief | Right context panel |
| Status | Grid or metadata section |
| Secondary edit | Drawer |
| Confirmation | Dialog |

Consistency overrides visual experimentation.

---

# 17. Context Preservation

Preserve where possible:

- Search text
- Filters
- Sorting
- Selected row
- Scroll position
- Active tab
- Resized panels
- Workspace state

Users should not lose operational context during navigation.

---

# 18. Status Visibility

Operational states must be visible.

Examples:

- Indexed
- Pending
- In Review
- Approved
- Rejected
- Archived
- Restricted
- Failed
- Needs Attention

Do not rely on color alone.

---

# 19. Desktop First

Primary target:

- 1920 × 1080

Supported:

- 1600 × 900
- 1440 × 900
- 1366 × 768
- Minimum 1280px desktop width

Below 1280px:

- Collapse the right context panel first
- Preserve grid usability
- Allow horizontal scrolling
- Do not convert grids into cards

---

# 20. Accessibility

Support:

- Keyboard navigation
- Visible focus
- WCAG AA contrast
- Accessible labels
- Screen readers
- Semantic headings
- Status communication beyond color
- Tooltips for icon-only actions

Use Material UI accessibility behaviour rather than replacing it with custom controls.

---

# 21. Performance

Preferred:

- Incremental loading
- Virtualized grids
- Lazy loading
- Skeletons
- Independent panel loading
- Preserved previous content during refresh

Avoid blocking the entire workspace when one region is loading.

---

# 22. Implementation Rules

Every workspace shall:

- Use Material UI as the component foundation.
- Use the shared IKMS Material theme.
- Use MUI X Data Grid for operational collections where available.
- Use the installed MUI licence tier only.
- Use the standard workspace layout.
- Use cards only for contextual content.
- Keep AI inside the contextual workspace region.
- Preserve navigation state.
- Maintain compact enterprise spacing.
- Use one outlined icon library consistently.
- Use the official IKMS logo asset.
- Avoid custom duplicates of standard MUI controls.
- Optimize operational efficiency over decoration.

These principles are mandatory for every IKMS screen.
