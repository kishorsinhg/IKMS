# Customer360 Workspace

**Version:** 3.1  
**File:** `docs/06-ui/screens/customer360-workspace.md`

---

# Purpose

Customer360 provides persistent access to all knowledge associated with a customer.

---

# Layout

```text
Workspace Header
Persistent Customer Summary
Tabs
Active Tab DataGrid
Right Context Panel
```

---

# Material UI Components

- `Paper` or bordered `Box` for customer summary
- `Tabs` and `Tab`
- MUI X `DataGrid`
- `Chip` for status
- `Drawer` or bordered `Box` for context
- Standard MUI buttons and menus

---

# Customer Summary

Display:

- Customer Name
- Customer Number
- Customer Type
- Status
- Assigned Broker
- Primary Contact
- Email
- Phone
- Last Activity
- Last Updated

Rules:

- Remains visible across tabs.
- Use one structured summary region.
- Do not split into KPI cards.

---

# Tabs

1. Documents
2. Emails
3. Notes
4. Relationships
5. Policy References
6. Claim References
7. Timeline

Use MUI `Tabs` and `Tab`.

Preserve active tab.

---

# Documents DataGrid

| Column | Width |
|---|---:|
| Icon | 40 |
| Document | 320 |
| Category | 160 |
| Version | 90 |
| Modified | 150 |
| Owner | 140 |
| Status | 100 |

Actions:

- Open
- Preview
- Review
- Download

Double-click opens Review.

---

# Emails DataGrid

| Column | Width |
|---|---:|
| Icon | 40 |
| Subject | 340 |
| Sender | 220 |
| Date | 150 |
| Attachment | 80 |
| Status | 100 |

---

# Notes DataGrid

| Column | Width |
|---|---:|
| Title | 320 |
| Author | 180 |
| Created | 160 |
| Updated | 160 |

---

# Relationships DataGrid

| Column | Width |
|---|---:|
| Related Customer | 280 |
| Relationship | 180 |
| Source | 160 |
| Status | 100 |

---

# Policy References DataGrid

| Column | Width |
|---|---:|
| Policy Number | 180 |
| Product | 220 |
| Insurer | 220 |
| Status | 120 |
| Expiry | 150 |

Read-only reference data.

Navigation opens the source system.

---

# Claim References DataGrid

| Column | Width |
|---|---:|
| Claim Number | 180 |
| Policy | 180 |
| Status | 120 |
| Opened | 150 |
| Updated | 150 |

Read-only reference data.

---

# Timeline

Use IKMS `AuditTimeline` or structured list composed from MUI primitives.

Events include:

- Document Added
- Email Imported
- Note Created
- Review Completed
- Metadata Updated
- AI Summary Generated

Newest first.

---

# Right Context Panel

- AI Customer Brief
- Alerts
- Recent Changes
- Recommendations
- Related Evidence
- Quick Actions

Use compact sections.

---

# Navigation

```text
Search
↓
Customer360
↓
Document
↓
Review
↓
Customer360
```

Preserve:

- Customer
- Active tab
- Grid state
- Selected row
- Scroll position

---

# States

Support:

- Populated
- Loading
- Empty
- Error
- Restricted
- Long names
- Mixed statuses

---

# Keyboard

| Shortcut | Action |
|---|---|
| Ctrl+1 | Documents |
| Ctrl+2 | Emails |
| Ctrl+3 | Notes |
| Ctrl+4 | Relationships |
| Ctrl+5 | Policies |
| Ctrl+6 | Claims |
| Ctrl+7 | Timeline |

---

# Codex Rules

- Use MUI Tabs and DataGrid.
- Customer context remains visible.
- Every operational tab uses a grid.
- Policy and claim references remain read-only.
- Do not use dashboard cards.
- Use shared theme and icons.
- Do not recreate standard MUI controls.
