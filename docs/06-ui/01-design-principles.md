# IKMS UI Design Principles (Human Guide)

**Version:** 2.0\
**Audience:** Product Owners, UX Designers, Frontend Developers,
Architects\
**Purpose:** Establish the design philosophy and UX standards for IKMS.

> **Note:** This document defines **design intent**. It is **not** the
> implementation guide for AI agents. Codex/Claude Code implementation
> rules belong in a separate `10-codex-ui-rules.md`.

------------------------------------------------------------------------

# 1. Product Vision

IKMS is an **Enterprise Insurance Knowledge Management Platform**
designed for operational users who spend **8+ hours per day** processing
customers, documents, policies, claims and AI-assisted workflows.

The UI should optimize for:

-   Productivity over decoration
-   Information density over whitespace
-   Consistency over creativity
-   Trust over novelty
-   Operational awareness over dashboards
-   Accessibility by default

------------------------------------------------------------------------

# 2. Design Character

The application should feel:

-   Calm
-   Professional
-   Trustworthy
-   Evidence-driven
-   Permission-aware
-   Predictable
-   Efficient

Avoid:

-   Consumer/mobile-app aesthetics
-   Marketing-style landing pages
-   Excessive animations
-   Large unused whitespace
-   Oversized cards
-   Decorative charts without business value

------------------------------------------------------------------------

# 3. Core Design Principles

## Readability First

Users continuously read:

-   Client profiles
-   Policies
-   Claims
-   Documents
-   AI answers
-   Audit history

Typography and hierarchy must support rapid scanning.

------------------------------------------------------------------------

## Controlled Information Density

Desktop is the primary platform.

Show meaningful information without overwhelming users.

Prefer compact enterprise layouts rather than sparse consumer layouts.

------------------------------------------------------------------------

## Trust Through Restraint

Visual design should communicate reliability.

Use restrained colors.

Highlight only meaningful operational states.

------------------------------------------------------------------------

## Evidence First

Documents, AI responses, metadata and citations should always remain
connected.

Never separate conclusions from supporting evidence.

------------------------------------------------------------------------

## Permission-Aware UX

Role differences must be explicit.

Masked, redacted and restricted information should appear
intentional---not broken.

------------------------------------------------------------------------

# 4. Enterprise Layout Principles

Every workspace should generally contain:

1.  Breadcrumb
2.  Page title
3.  Short description
4.  Primary actions
5.  Search / filters
6.  Main work area
7.  Supporting panels
8.  Status information

Navigation should remain persistent on desktop.

------------------------------------------------------------------------

# 5. Operational Workspaces

Design around work, not CRUD.

Typical workspace patterns include:

-   Dashboard
-   Search Workspace
-   Review Queue
-   Client Profile
-   Document Viewer
-   Administration
-   Audit Explorer

Each workspace should clearly answer:

-   What requires attention?
-   What changed?
-   What is blocked?
-   What should I do next?

------------------------------------------------------------------------

# 6. Forms

Forms should:

-   Group related information
-   Keep labels visible
-   Use inline validation
-   Reduce scrolling
-   Support keyboard navigation

Avoid unnecessary wizard flows.

------------------------------------------------------------------------

# 7. Tables

Operational data should primarily use tables instead of cards.

Tables should emphasize:

-   Readability
-   Sorting
-   Filtering
-   Status visibility
-   Auditability

------------------------------------------------------------------------

# 8. Empty States

Every empty state should explain:

-   Why the screen is empty
-   What action is available next
-   How users can continue

Avoid playful illustrations.

------------------------------------------------------------------------

# 9. State Design

The following states must have consistent visual language:

-   Loading
-   Empty
-   Success
-   Validation Error
-   Backend Failure
-   Unauthorized
-   Review Required
-   Duplicate Detected
-   Redacted
-   PII Masked
-   AI Refusal
-   Conflict Detected

Every state should explain the situation and the recommended next
action.

------------------------------------------------------------------------

# 10. Color Philosophy

Use color to communicate meaning.

Primary colors should represent:

-   Primary Action
-   Success
-   Warning
-   Error
-   Neutral

Never rely solely on color to communicate state.

------------------------------------------------------------------------

# 11. Typography

Typography should support scanning.

Differentiate clearly between:

-   Titles
-   Section headings
-   Metadata
-   Narrative content
-   Status labels

Avoid oversized headings inside operational panels.

------------------------------------------------------------------------

# 12. Motion

Animation should:

-   Reinforce user actions
-   Reduce perceived latency
-   Preserve context

Avoid decorative animation.

------------------------------------------------------------------------

# 13. Accessibility

IKMS targets WCAG AA.

Support:

-   Keyboard navigation
-   Screen readers
-   High contrast
-   Visible focus
-   Semantic controls

------------------------------------------------------------------------

# 14. Responsive Behaviour

Desktop-first.

On smaller screens:

-   Collapse layouts progressively
-   Preserve navigation context
-   Avoid horizontal scrolling
-   Prioritize read-only workflows where editing becomes impractical

------------------------------------------------------------------------

# 15. Role Experience

Each role should feel purpose-built.

Indexer: - Queue driven

Processor: - Evidence driven

Supervisor: - Decision driven

Administrator: - Configuration driven

------------------------------------------------------------------------

# 16. Design Priorities

When trade-offs exist, prioritize:

1.  Productivity
2.  Readability
3.  Consistency
4.  Information Density
5.  Accessibility
6.  Visual Polish

------------------------------------------------------------------------

# 17. Definition of Good Design

A successful screen allows users to:

-   Understand their current context immediately
-   Identify available actions
-   Locate required information quickly
-   Complete work with minimal clicks
-   Trust AI recommendations through supporting evidence
-   Recover easily from errors

------------------------------------------------------------------------

# 18. Related Documents

This document is part of the UI documentation set.

    docs/ui/

    01-design-principles.md     ← This document
    02-design-system.md
    03-layout-guidelines.md
    04-component-library.md
    05-enterprise-patterns.md
    06-ai-experience.md
    07-accessibility.md
    08-responsive.md
    09-design-tokens.md
    10-codex-ui-rules.md

------------------------------------------------------------------------

# Final Principle

> IKMS is an enterprise operations platform---not a showcase website.

Every design decision should help insurance professionals complete work
**faster, more accurately, and with greater confidence**.
