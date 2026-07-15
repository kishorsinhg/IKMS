package com.ikms.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GovernanceContracts {

  private GovernanceContracts() {
  }

  public record ClassificationPolicyResponse(
      List<String> levels,
      String defaultClassification,
      String aiRestrictionThreshold,
      String exportRestrictionThreshold,
      Instant updatedAt) {
  }

  public record ClassificationPolicyRequest(
      @NotEmpty(message = "At least one classification level is required.") List<String> levels,
      @NotBlank(message = "Default classification is required.") String defaultClassification,
      @NotBlank(message = "AI restriction threshold is required.") String aiRestrictionThreshold,
      @NotBlank(message = "Export restriction threshold is required.") String exportRestrictionThreshold) {
  }

  public record RetentionPolicyEntry(
      @NotBlank(message = "Content type is required.") String contentType,
      int retentionDays,
      Integer reviewAfterDays,
      Integer archivalAfterDays,
      Integer disposalAfterDays) {
  }

  public record RetentionPolicyResponse(
      List<RetentionPolicyEntry> policies,
      Instant updatedAt) {
  }

  public record RetentionPolicyRequest(
      @NotEmpty(message = "Retention policies are required.") List<RetentionPolicyEntry> policies) {
  }

  public record LegalHoldResponse(
      UUID id,
      String targetType,
      String targetId,
      UUID clientId,
      boolean legalHold,
      String holdType,
      String retentionPolicyKey,
      Instant reviewAt,
      Instant archivalEligibleAt,
      Instant disposalEligibleAt,
      Instant executedAt,
      String reason) {
  }

  public record LegalHoldRequest(
      @NotBlank(message = "Target type is required.") String targetType,
      @NotBlank(message = "Target ID is required.") String targetId,
      UUID clientId,
      @NotBlank(message = "Hold type is required.") String holdType,
      String reason) {
  }

  public record ReclassifyRequest(
      @NotBlank(message = "Classification is required.") String classification,
      String sensitivityLevel,
      String confidentiality,
      String dataResidency,
      String businessUnit,
      String department,
      String region,
      String country,
      String brokerOffice,
      boolean exportRestricted,
      boolean watermarkRequired) {
  }

  public record AiGovernancePolicyResponse(
      List<String> approvedModels,
      String promptPolicyVersion,
      String responsePolicyVersion,
      boolean citationRequired,
      boolean groundingValidationRequired,
      Instant updatedAt) {
  }

  public record AiGovernancePolicyRequest(
      @NotEmpty(message = "Approved models are required.") List<String> approvedModels,
      @NotBlank(message = "Prompt policy version is required.") String promptPolicyVersion,
      @NotBlank(message = "Response policy version is required.") String responsePolicyVersion,
      boolean citationRequired,
      boolean groundingValidationRequired) {
  }

  public record SecurityPolicyResponse(
      String encryptionAtRest,
      String encryptionInTransit,
      String keyManagement,
      String secretManagement,
      boolean exportApprovalRequired,
      boolean watermarkByDefault,
      Instant updatedAt) {
  }

  public record SecurityPolicyRequest(
      @NotBlank(message = "Encryption-at-rest guidance is required.") String encryptionAtRest,
      @NotBlank(message = "Encryption-in-transit guidance is required.") String encryptionInTransit,
      @NotBlank(message = "Key-management guidance is required.") String keyManagement,
      @NotBlank(message = "Secret-management guidance is required.") String secretManagement,
      boolean exportApprovalRequired,
      boolean watermarkByDefault) {
  }

  public record ComplianceReportResponse(
      int activeLegalHolds,
      int retentionExceptions,
      int sensitiveDocuments,
      int restrictedDocuments,
      int piiAuditEvents,
      int exportEvents,
      int aiInteractions,
      List<String> stewardshipSignals) {
  }
}
