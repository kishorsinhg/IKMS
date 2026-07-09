# AI And RAG Design

## V1 AI Capabilities

- OCR/ICR for PDF where needed.
- DOCX text extraction.
- Document classification.
- Metadata extraction.
- PII detection assistance.
- Prompt injection detection.
- Embedding generation.
- Client-level RAG Q&A.

## Provider Strategy

AI provider is configurable. Mistral Cloud is the first tested provider.

Configuration should include:

- Provider name
- API key/secret reference
- OCR model
- Chat model
- Embedding model
- Timeout and retry settings
- Enable/disable flags

## RAG Rules

- Client-level only in V1.
- Retrieval must filter by Client before search.
- Retrieval must filter by user role and PII permissions before model context assembly.
- Use current document version by default.
- Use original OCR/email/note chunks as retrieval source.
- AI answer must cite sources.
- If no evidence is found, answer must say no evidence was found.
- Decision-making requests must be refused.

## Language Support

- UI is English only.
- Documents and RAG must support English and German content.
- Embedding model must support English and German.

