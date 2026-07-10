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
}
