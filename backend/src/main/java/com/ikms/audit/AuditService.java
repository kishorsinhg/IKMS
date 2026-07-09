package com.ikms.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface AuditService {

  void write(AuditEvent event);

  record AuditEvent(
      Instant occurredAt,
      String category,
      String action,
      AuditOutcome outcome,
      UUID actorUserId,
      UUID clientId,
      String targetType,
      String targetId,
      boolean piiAccess,
      Map<String, String> details) {

    public AuditEvent {
      occurredAt = occurredAt == null ? Instant.now() : occurredAt;
      details = details == null ? Map.of() : Map.copyOf(details);
    }
  }

  enum AuditOutcome {
    SUCCESS,
    FAILURE,
    DENIED
  }
}
