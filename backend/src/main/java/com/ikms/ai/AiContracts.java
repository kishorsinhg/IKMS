package com.ikms.ai;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiContracts {

  private AiContracts() {
  }

  public record AskClientRequest(@NotBlank(message = "Question is required.") String question) {
  }

  public record SourceCitation(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      Integer pageNumber,
      String sourceSection) {
  }

  public record AskClientResponse(
      UUID interactionId,
      String status,
      String answer,
      List<SourceCitation> citations,
      Instant createdAt) {
  }

  public record AiFeedbackRequest(
      Boolean helpful,
      String comment) {
  }
}
