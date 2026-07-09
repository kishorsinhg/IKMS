# Deployment Model

## V1 Deployment

Single-tenant deployment for one broker.

Supported environments:

- On-premise server
- Private cloud server

## Runtime Profiles

- Web profile
- Worker profile

Both profiles are built from the same Spring Boot codebase.

## Core Dependencies

- PostgreSQL with pgvector
- Controlled document file storage
- IMAP mailbox access
- Internet access for configured AI provider

## Non-SaaS Boundary

V1 is not a multi-tenant SaaS platform. Tenant isolation is not part of the V1 design.

