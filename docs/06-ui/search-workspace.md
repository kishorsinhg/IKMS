# Search Workspace Specification

Version: 1.0

Status: Approved

---

# Purpose

The Search Workspace is the home of IKMS.

Users should begin productive work within three seconds.

Search is not navigation.

Search is the primary interaction model.

---

# Primary Users

- Processor
- Supervisor

---

# Primary Goal

Locate customer knowledge quickly.

---

# Success Criteria

The user can:

- Search immediately
- Resume recent work
- Open Customer360
- Understand today's priorities

---

# Layout

------------------------------------------------

Header

------------------------------------------------

Global Search

------------------------------------------------

Continue Recent Work

Today's Work

Recent Activity

Quick Actions

------------------------------------------------

Search Results (when searching)

------------------------------------------------

---

# Header

Contains

- Workspace title
- User profile
- Notifications

Do not place statistics here.

---

# Global Search

Always visible.

Always focused when page opens.

Supports:

- Customer
- Policy Reference
- Claim Reference
- Email
- Document
- Note
- Metadata
- Natural language

Examples

John Smith

Motor policy for John

Unread emails from ABC

Driver licence

Document uploaded yesterday

---

# Continue Recent Work

Shows recently opened customers.

Suggested maximum

5

Each item should show

- Customer Name
- Last Activity
- Last Opened
- Quick Open

---

# Today's Work

Shows

- Documents awaiting review
- OCR exceptions
- AI validation
- High priority items

Do not show management KPIs.

---

# Recent Activity

Displays recent operational activity.

Examples

- Document uploaded
- Email received
- AI completed extraction
- Review completed

---

# Quick Actions

Examples

- Upload Document
- New Note
- Import
- Search Help

Do not expose administrative functions.

---

# Search Results

Group by category

Customers

Documents

Emails

Notes

Policy References

Claim References

Knowledge

Each result should display enough context for users to decide whether to open it.

---

# Selecting a Customer

Selecting a customer opens Customer360.

This is the primary navigation path.

---

# Empty State

Show

Search for a customer, document, email or note.

Also display

Recent Work

Quick Actions

Do not display a blank page.

---

# Loading

Search should progressively return grouped results.

Users should receive feedback immediately.

---

# Permissions

Search only returns authorized content.

Restricted content should never leak through snippets.

---

# Acceptance Criteria

✓ Search available immediately

✓ Cursor focused

✓ Continue work visible

✓ Search grouped

✓ Customer360 launch

✓ No traditional dashboard

✓ Operational layout

✓ Responsive desktop experience