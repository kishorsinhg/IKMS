package com.ikms.review;

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
      Map<String, String> metadataValues) {
  }

  public record LinkClientRequest(UUID clientId) {
  }

  public record CorrectMetadataRequest(String title, UUID documentTypeId, Map<String, String> metadataValues) {
  }

  public record ReviewDecisionRequest(String reason) {
  }
}
