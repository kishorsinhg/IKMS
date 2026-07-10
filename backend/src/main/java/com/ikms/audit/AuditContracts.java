package com.ikms.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditContracts {

  private AuditContracts() {
  }

  public record AuditLogResponse(
      UUID id,
      Instant occurredAt,
      Instant retainedUntil,
      UUID actorUserId,
      String actorUsername,
      UUID clientId,
      String category,
      String action,
      String outcome,
      String targetType,
      String targetId,
      boolean piiAccess,
      Map<String, String> details) {
  }
}
