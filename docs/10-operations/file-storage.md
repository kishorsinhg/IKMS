# File Storage

## Rules

- Store original files outside PostgreSQL.
- Keep database references to file path/object key, hash, size, MIME type, and version.
- Never overwrite original files.
- Redacted copies and previews are generated separately.
- Document versions each have their own original file and hash.

## Supported V1 Files

- PDF
- DOCX

## Duplicate Handling

- Exact same hash is blocked for manual upload.
- Exact same hash is skipped for shared folder and email intake.
- Different hash may be added as a new document version when intentionally selected.

