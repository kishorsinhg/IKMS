package com.ikms.email;

import java.time.Instant;
import java.util.UUID;

public final class EmailContracts {

  private EmailContracts() {
  }

  public record EmailSummaryResponse(
      UUID id,
      UUID clientId,
      String subject,
      String sender,
      String recipients,
      String processingStatus,
      String reviewStatus,
      Instant receivedAt) {
  }
}
