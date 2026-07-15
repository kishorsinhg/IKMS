package com.ikms.review;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReviewContracts {

  private ReviewContracts() {
  }

  public record ReviewQueueItemResponse(
      UUID id,
      ReviewQueueItemType itemType,
      String itemId,
      ReviewQueueReason reason,
      ReviewQueueStatus status,
      UUID assignedTo,
      String title,
      UUID clientId,
      UUID documentTypeId,
      Map<String, String> metadataValues,
      ProcessingJobResponse processingJob) {
  }

  public record ProcessingJobResponse(
      UUID id,
      String status,
      String currentStage,
      Integer retryCount,
      BigDecimal overallConfidence,
      BigDecimal ocrConfidence,
      BigDecimal classificationConfidence,
      BigDecimal metadataConfidence,
      BigDecimal businessReferenceConfidence,
      BigDecimal validationConfidence,
      BigDecimal duplicateConfidence,
      String language,
      String ocrProvider,
      String classificationProvider,
      String lastErrorCode,
      String lastErrorMessage,
      String reviewerComment,
      Instant startedAt,
      Instant reviewRequestedAt,
      Instant approvedAt,
      Instant rejectedAt,
      Instant publishedAt,
      Instant completedAt,
      List<ProcessingFieldResponse> fields,
      List<ProcessingFindingResponse> findings) {
  }

  public record ProcessingFieldResponse(
      String fieldKey,
      String fieldLabel,
      String fieldType,
      String businessReferenceType,
      String extractedValue,
      String correctedValue,
      String approvedValue,
      BigDecimal confidence,
      String sourceType,
      String extractionMethod,
      Integer sourcePage,
      boolean required,
      String validationState) {
  }

  public record ProcessingFindingResponse(
      String findingCode,
      String severity,
      String stage,
      String fieldKey,
      String message,
      String evidenceText,
      Integer sourcePage,
      BigDecimal confidence,
      String status,
      String resolutionComment,
      Instant createdAt,
      Instant resolvedAt) {
  }

  public record LinkClientRequest(UUID clientId) {
  }

  public record CorrectMetadataRequest(String title, UUID documentTypeId, Map<String, String> metadataValues, String reviewerComment) {
  }

  public record ReviewDecisionRequest(String reason) {
  }

  public record RetryReviewJobRequest(String reviewerComment) {
  }
}
