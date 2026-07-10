package com.ikms.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ikms.audit.AuditService;
import com.ikms.retention.RetentionWorkflowService.RetentionAction;
import com.ikms.retention.RetentionWorkflowService.RetentionDecision;
import com.ikms.retention.RetentionWorkflowService.RetentionRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RetentionWorkflowTest {

  private AuditService auditService;
  private RetentionWorkflowService retentionWorkflowService;

  @BeforeEach
  void setUp() {
    auditService = mock(AuditService.class);
    retentionWorkflowService = new RetentionWorkflowService(auditService);
  }

  @Test
  void deleteShouldBeDeniedWhenLegalHoldIsActive() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.DELETE,
        true,
        Instant.now().minus(2, ChronoUnit.DAYS)));

    assertThat(decision.approved()).isFalse();
    assertThat(decision.reason()).contains("Legal hold");
    verify(auditService).write(any());
  }

  @Test
  void anonymizeShouldBeDeniedBeforeMinimumRetentionHasElapsed() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.ANONYMIZE,
        false,
        Instant.now().plus(10, ChronoUnit.DAYS)));

    assertThat(decision.approved()).isFalse();
    assertThat(decision.reason()).contains("Minimum retention period");
    verify(auditService).write(any());
  }

  @Test
  void applyLegalHoldShouldBeAccepted() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.APPLY_LEGAL_HOLD,
        false,
        null));

    assertThat(decision.approved()).isTrue();
    verify(auditService).write(any());
  }

  private RetentionRequest request(RetentionAction action, boolean legalHold, Instant minimumRetentionUntil) {
    return new RetentionRequest(
        "Document",
        "doc-123",
        UUID.randomUUID(),
        UUID.randomUUID(),
        action,
        legalHold,
        minimumRetentionUntil,
        "Retention workflow test");
  }
}
