package com.ikms.ai;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EnterpriseKnowledgeContracts {

  private EnterpriseKnowledgeContracts() {
  }

  public record KnowledgeQueryPlanResponse(
      String scope,
      String queryText,
      Map<String, String> businessReferenceFields,
      String versionPreference,
      String requiredEvidenceGranularity) {
  }

  public record KnowledgeResultResponse(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      Map<String, String> supportingAttributes) {
  }

  public record KnowledgeSearchResponse(
      KnowledgeQueryPlanResponse queryPlan,
      List<KnowledgeResultResponse> results) {
  }

  public record AskCitationResponse(
      String sourceType,
      UUID sourceId,
      String title,
      Map<String, String> supportingAttributes) {
  }

  public record GlobalAskResponse(
      String status,
      String answer,
      String groundingStatus,
      List<AskCitationResponse> citations,
      String restrictedContentNotice,
      String auditCorrelationId) {
  }

  public record EvidenceRecord(
      String sourceType,
      UUID sourceId,
      String title,
      Map<String, String> supportingAttributes) {
  }

  public record EvidenceExpansionResponse(
      UUID interactionId,
      List<EvidenceRecord> evidence) {
  }

  public record ConversationContinuationResponse(
      UUID conversationId,
      int historyCount,
      KnowledgeQueryPlanResponse queryPlan) {
  }

  public record StreamEventResponse(
      String type,
      String data) {
  }

  public record StreamResponse(
      boolean streaming,
      String status,
      List<StreamEventResponse> events,
      List<String> warnings) {
  }
}
