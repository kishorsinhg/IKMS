package com.ikms.ai.context;

import java.time.Instant;
import java.util.UUID;

public record ContextEvidenceItem(
    String sourceType,
    UUID sourceId,
    String title,
    String excerpt,
    String section,
    Integer pageNumber,
    String retrievalPath,
    String confidence,
    Instant occurredAt,
    int estimatedTokens) {
}
