# Processing Pipeline

## Intake Sources

- Manual upload
- Shared folder polling
- IMAP email polling

## Pipeline

1. Detect or receive item.
2. Validate file type.
3. Calculate hash.
4. Check duplicate.
5. Store original file.
6. Create intake item.
7. Extract text/OCR.
8. Classify document type.
9. Extract metadata.
10. Detect PII and prompt injection risk.
11. Generate chunks and embeddings.
12. Generate redacted copy if needed.
13. Match or suggest Client.
14. Route to review queue when needed.
15. Release to Client profile after approved/linking.

## Background Jobs

Use PostgreSQL-backed job tables for V1. Avoid Kafka/RabbitMQ unless future scale requires it.

