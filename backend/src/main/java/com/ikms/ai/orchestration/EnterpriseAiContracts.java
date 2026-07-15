package com.ikms.ai.orchestration;

import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class EnterpriseAiContracts {

  private EnterpriseAiContracts() {
  }

  public record EnterpriseAiRequest(
      UUID clientId,
      EnterpriseAiOperation operation,
      String prompt,
      UUID actorUserId,
      Set<Permission> permissions,
      UUID conversationId,
      List<UUID> sourceIds,
      Map<String, Object> parameters) {
  }

  public record AuthorizationContext(
      UUID actorUserId,
      Set<Permission> permissions,
      EnterpriseAiOperation operation,
      boolean piiAccessAllowed) {
  }

  public record DetectedIntent(
      EnterpriseAiOperation operation,
      String normalizedPrompt,
      String reasoningMode,
      boolean comparisonRequested) {
  }

  public enum QueryScope {
    CUSTOMER,
    REVIEW_ITEM,
    DOCUMENT_VERSION,
    GLOBAL
  }

  public enum SourceType {
    DOCUMENT,
    DOCUMENT_VERSION,
    EMAIL,
    NOTE,
    REVIEW,
    OCR_TEXT,
    EXTRACTED_FIELD,
    CUSTOMER_ATTRIBUTE,
    AI_CONVERSATION
  }

  public enum SortOrder {
    RELEVANCE,
    NEWEST_FIRST,
    OLDEST_FIRST
  }

  public enum VersionPreference {
    CURRENT_VERSION,
    PREVIOUS_VERSION,
    VERSION_NEUTRAL
  }

  public enum EvidenceGranularity {
    PAGE,
    CHUNK,
    DOCUMENT
  }

  public record QueryDateRange(
      String from,
      String to) {
  }

  public record BusinessReferenceFields(
      String policyNumber,
      String claimNumber,
      String insurer,
      String policyType,
      String effectiveDate,
      String expiryDate,
      String renewalDate,
      String brokerReference,
      String externalReference) {

    public boolean hasValues() {
      return Stream.of(
              policyNumber,
              claimNumber,
              insurer,
              policyType,
              effectiveDate,
              expiryDate,
              renewalDate,
              brokerReference,
              externalReference)
          .anyMatch(value -> value != null && !value.isBlank());
    }
  }

  public record QueryPlan(
      EnterpriseAiOperation operation,
      String normalizedPrompt,
      String reasoningMode,
      QueryScope scope,
      List<String> retrievalModes,
      List<SourceType> sourceTypes,
      List<String> documentTypes,
      QueryDateRange dateRange,
      BusinessReferenceFields businessReferenceFields,
      int maxEvidenceItems,
      int maxChunksPerSource,
      int tokenBudget,
      int resultLimit,
      SortOrder sortOrder,
      VersionPreference versionPreference,
      EvidenceGranularity requiredEvidenceGranularity,
      List<UUID> sourceIds) {
  }

  public record RetrievedEvidence(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      String citation,
      Integer pageNumber,
      String sourceSection,
      String retrievalPath,
      String citationQuality,
      Instant occurredAt) {
  }

  public record CitationReference(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      Integer pageNumber,
      Integer chunkIndex,
      String section,
      String confidence,
      String jumpTargetId,
      String retrievalPath) {
  }

  public record SourceReference(
      String key,
      String label,
      String kind) {
  }

  public record EvidenceReference(
      String key,
      String label,
      String target,
      String detail,
      boolean disabled) {
  }

  public record MetricsSnapshot(
      long totalLatencyMs,
      int evidenceCount,
      String retrievalMode,
      boolean fallbackUsed) {
  }

  public record GroundingValidation(
      boolean grounded,
      double groundingScore,
      double citationCoverage,
      List<String> warnings) {
  }

  public record EnterpriseAiResponse(
      EnterpriseAiOperation operation,
      UUID conversationId,
      UUID interactionId,
      String status,
      String answer,
      List<CitationReference> citations,
      List<EvidenceReference> evidenceReferences,
      List<SourceReference> sourceReferences,
      List<String> warnings,
      MetricsSnapshot metrics,
      GroundingValidation grounding,
      Map<String, Object> structuredPayload) {
  }
}
