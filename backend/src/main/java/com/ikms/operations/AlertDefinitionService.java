package com.ikms.operations;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertDefinitionService {

  public List<OperationsContracts.AlertDefinitionResponse> definitions() {
    return List.of(
        alert("ALT-OCR-001", AlertSeverity.HIGH, AlertCategory.OCR, ">= 5 failed OCR jobs in 15 minutes", "15 minutes", AlertEscalationLevel.PLATFORM_OPERATIONS, "Validate OCR provider health, inspect failed processing jobs, and retry affected documents after provider recovery."),
        alert("ALT-AI-001", AlertSeverity.HIGH, AlertCategory.AI, ">= 3 failed AI requests in 10 minutes", "10 minutes", AlertEscalationLevel.ENGINEERING, "Verify approved model configuration, provider timeout behavior, and grounding-safe fallback responses."),
        alert("ALT-RET-001", AlertSeverity.WARNING, AlertCategory.RETRIEVAL, "vector retrieval fallback rate > 20% over 30 minutes", "30 minutes", AlertEscalationLevel.PLATFORM_OPERATIONS, "Review embedding freshness, pgvector availability, and degraded retrieval warnings before reindexing."),
        alert("ALT-PRC-001", AlertSeverity.HIGH, AlertCategory.PROCESSING, "processing failures > 10 per hour", "60 minutes", AlertEscalationLevel.PLATFORM_OPERATIONS, "Inspect processing stages, storage access, and review-queue spillover to restore intake throughput."),
        alert("ALT-QUE-001", AlertSeverity.HIGH, AlertCategory.QUEUE, "queue backlog exceeds 100 queued items for 30 minutes", "30 minutes", AlertEscalationLevel.PLATFORM_OPERATIONS, "Assess paused queues, worker saturation, and retry storms; prioritize recovery before bulk rebuilds."),
        alert("ALT-SCH-001", AlertSeverity.HIGH, AlertCategory.SCHEDULER, "scheduled execution misses 2 consecutive runs", "120 minutes", AlertEscalationLevel.PLATFORM_OPERATIONS, "Check scheduler enablement, recent execution history, and dependent job failures."),
        alert("ALT-GOV-001", AlertSeverity.CRITICAL, AlertCategory.GOVERNANCE, "legal hold or retention workflow error detected", "0 minutes", AlertEscalationLevel.DATA_GOVERNANCE, "Pause destructive actions, confirm hold metadata, and review retention execution traces before retry."),
        alert("ALT-SEC-001", AlertSeverity.CRITICAL, AlertCategory.SECURITY, "unauthorized access or export restriction violation detected", "0 minutes", AlertEscalationLevel.SECURITY_OFFICER, "Contain the session, review audit correlation IDs, validate ABAC policy state, and escalate per incident runbook."),
        alert("ALT-QLT-001", AlertSeverity.WARNING, AlertCategory.QUALITY, "knowledge readiness blocked for > 10 customers", "24 hours", AlertEscalationLevel.DATA_GOVERNANCE, "Review stewardship backlog, failed projections, and repeated validation findings before go-live."));
  }

  private static OperationsContracts.AlertDefinitionResponse alert(
      String alertId,
      AlertSeverity severity,
      AlertCategory category,
      String threshold,
      String suppressionWindow,
      AlertEscalationLevel escalationLevel,
      String resolutionGuidance) {
    return new OperationsContracts.AlertDefinitionResponse(
        alertId,
        severity.name(),
        category.name(),
        threshold,
        suppressionWindow,
        escalationLevel.name(),
        resolutionGuidance);
  }
}
