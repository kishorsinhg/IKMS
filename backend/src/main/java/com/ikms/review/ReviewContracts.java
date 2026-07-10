package com.ikms.review;

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
      UUID assignedTo) {
  }

  public record LinkClientRequest(UUID clientId) {
  }

  public record CorrectMetadataRequest(String title) {
  }

  public record ReviewDecisionRequest(String reason) {
  }
}
