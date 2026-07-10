package com.ikms.retention;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RetentionWorkflowService {

  private final AuditService auditService;

  public RetentionWorkflowService(AuditService auditService) {
    this.auditService = auditService;
  }

  public RetentionDecision evaluate(RetentionRequest request) {
    if (request.action() == RetentionAction.DELETE || request.action() == RetentionAction.ANONYMIZE) {
      if (request.legalHold()) {
        return auditAndReturn(request, false, "Legal hold prevents destructive retention actions.");
      }
      if (request.minimumRetentionUntil() != null && request.minimumRetentionUntil().isAfter(Instant.now())) {
        return auditAndReturn(request, false, "Minimum retention period has not elapsed.");
      }
    }

    if (request.action() == RetentionAction.RELEASE_LEGAL_HOLD && !request.legalHold()) {
      return auditAndReturn(request, false, "Cannot release a legal hold that is not active.");
    }

    return auditAndReturn(request, true, "Retention action accepted for workflow execution.");
  }

  private RetentionDecision auditAndReturn(RetentionRequest request, boolean approved, String reason) {
    auditService.write(new AuditEvent(
        Instant.now(),
        "RETENTION",
        request.action().name(),
        approved ? AuditOutcome.SUCCESS : AuditOutcome.DENIED,
        request.requestedBy(),
        request.clientId(),
        request.targetType(),
        request.targetId(),
        false,
        Map.of("reason", reason)));

    return new RetentionDecision(approved, reason);
  }

  public record RetentionRequest(
      String targetType,
      String targetId,
      UUID clientId,
      UUID requestedBy,
      RetentionAction action,
      boolean legalHold,
      Instant minimumRetentionUntil,
      String reason) {
  }

  public record RetentionDecision(boolean approved, String reason) {
  }

  public enum RetentionAction {
    APPLY_LEGAL_HOLD,
    RELEASE_LEGAL_HOLD,
    DELETE,
    ANONYMIZE
  }
}
