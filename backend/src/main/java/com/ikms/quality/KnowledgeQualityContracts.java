package com.ikms.quality;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KnowledgeQualityContracts {

  private KnowledgeQualityContracts() {
  }

  public record QualityDimensionScoreResponse(
      String key,
      String label,
      BigDecimal score,
      String summary) {
  }

  public record KnowledgeQualityIssueResponse(
      UUID id,
      UUID clientId,
      String sourceType,
      UUID sourceId,
      String category,
      String issueType,
      String severity,
      String status,
      String title,
      String detail,
      String recommendationType,
      String recommendationDetail,
      String businessReferenceKey,
      BigDecimal scoreImpact,
      Instant createdAt,
      Instant updatedAt) {
  }

  public record CustomerKnowledgeQualitySummaryResponse(
      UUID clientId,
      String customerName,
      String customerExternalId,
      BigDecimal overallScore,
      String readinessState,
      int issueCount,
      int openIssueCount,
      Instant evaluatedAt,
      List<QualityDimensionScoreResponse> dimensions,
      List<String> recommendationHighlights) {
  }

  public record CustomerKnowledgeQualityDetailResponse(
      CustomerKnowledgeQualitySummaryResponse summary,
      List<KnowledgeQualityIssueResponse> issues) {
  }

  public record KnowledgeQualityIssueQueueResponse(
      List<KnowledgeQualityIssueResponse> issues) {
  }

  public record KnowledgeQualityCustomerListResponse(
      List<CustomerKnowledgeQualitySummaryResponse> customers) {
  }

  public record QualityRevalidateRequest(
      @NotEmpty List<UUID> clientIds,
      @NotNull Boolean confirmed) {
  }

  public record QualityReindexRequest(
      @NotEmpty List<UUID> clientIds,
      @NotNull Boolean confirmed) {
  }

  public record BulkQualityCorrectionItemRequest(
      @NotNull UUID clientId,
      String sourceType,
      UUID sourceId,
      String fieldKey,
      String value,
      UUID targetClientId) {
  }

  public record BulkQualityCorrectionRequest(
      @NotNull String operationType,
      @NotNull Boolean confirmed,
      @NotEmpty List<BulkQualityCorrectionItemRequest> items) {
  }

  public record KnowledgeQualityActionResultResponse(
      String action,
      int affectedCustomers,
      int affectedItems,
      List<UUID> clientIds,
      Instant completedAt) {
  }

  public record KnowledgeQualityRecommendationResponse(
      String type,
      String label,
      String detail,
      boolean aiSuggested,
      Map<String, String> attributes) {
  }
}
