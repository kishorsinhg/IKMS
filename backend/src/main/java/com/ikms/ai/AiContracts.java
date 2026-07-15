package com.ikms.ai;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
      String retrievalMode,
      List<String> warnings,
      Instant createdAt) {
  }

  public record EnterprisePromptRequest(
      @NotBlank(message = "Prompt is required.") String prompt,
      UUID conversationId,
      List<UUID> sourceIds,
      Map<String, Object> parameters) {
  }

  public record GlobalAskRequest(
      @NotBlank(message = "Question is required.") String question,
      UUID customerId,
      Map<String, Object> parameters) {
  }

  public record ConversationContinueRequest(
      @NotBlank(message = "Prompt is required.") String prompt,
      List<UUID> sourceIds,
      Map<String, Object> parameters) {
  }

  public record StreamRequest(
      @NotBlank(message = "Prompt is required.") String prompt,
      UUID customerId,
      UUID conversationId,
      List<UUID> sourceIds,
      Map<String, Object> parameters) {
  }

  public record CompareRequest(
      @NotBlank(message = "Prompt is required.") String prompt,
      List<UUID> sourceIds,
      UUID conversationId,
      Map<String, Object> parameters) {
  }

  public record AiFeedbackRequest(
      Boolean helpful,
      String comment) {
  }
}
