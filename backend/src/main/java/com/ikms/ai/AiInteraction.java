package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_interaction")
public class AiInteraction {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String question;

  @Column(columnDefinition = "TEXT")
  private String answer;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "cited_sources", columnDefinition = "TEXT")
  private String citedSources;

  @Column(name = "operation_type", nullable = false, length = 32)
  private String operationType;

  @Column(name = "conversation_id")
  private UUID conversationId;

  @Column(name = "provider_name", length = 80)
  private String providerName;

  @Column(name = "model_name", length = 120)
  private String modelName;

  @Column(name = "retrieval_mode", length = 64)
  private String retrievalMode;

  @Column(name = "total_latency_ms")
  private Long totalLatencyMs;

  @Column(name = "prompt_tokens")
  private Integer promptTokens;

  @Column(name = "completion_tokens")
  private Integer completionTokens;

  @Column(name = "total_tokens")
  private Integer totalTokens;

  @Column(name = "grounding_score")
  private Double groundingScore;

  @Column(name = "citation_coverage")
  private Double citationCoverage;

  @Column(name = "fallback_used", nullable = false)
  private boolean fallbackUsed;

  @Column(name = "warning_summary", columnDefinition = "TEXT")
  private String warningSummary;

  @Column(name = "helpful_feedback")
  private Boolean helpfulFeedback;

  @Column(name = "feedback_comment", columnDefinition = "TEXT")
  private String feedbackComment;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = createdAt == null ? now : createdAt;
    updatedAt = updatedAt == null ? now : updatedAt;
    operationType = operationType == null ? "ASK" : operationType;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getQuestion() { return question; }
  public void setQuestion(String question) { this.question = question; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getCitedSources() { return citedSources; }
  public void setCitedSources(String citedSources) { this.citedSources = citedSources; }
  public String getOperationType() { return operationType; }
  public void setOperationType(String operationType) { this.operationType = operationType; }
  public UUID getConversationId() { return conversationId; }
  public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
  public String getProviderName() { return providerName; }
  public void setProviderName(String providerName) { this.providerName = providerName; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public String getRetrievalMode() { return retrievalMode; }
  public void setRetrievalMode(String retrievalMode) { this.retrievalMode = retrievalMode; }
  public Long getTotalLatencyMs() { return totalLatencyMs; }
  public void setTotalLatencyMs(Long totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
  public Integer getPromptTokens() { return promptTokens; }
  public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
  public Integer getCompletionTokens() { return completionTokens; }
  public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
  public Integer getTotalTokens() { return totalTokens; }
  public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
  public Double getGroundingScore() { return groundingScore; }
  public void setGroundingScore(Double groundingScore) { this.groundingScore = groundingScore; }
  public Double getCitationCoverage() { return citationCoverage; }
  public void setCitationCoverage(Double citationCoverage) { this.citationCoverage = citationCoverage; }
  public boolean isFallbackUsed() { return fallbackUsed; }
  public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
  public String getWarningSummary() { return warningSummary; }
  public void setWarningSummary(String warningSummary) { this.warningSummary = warningSummary; }
  public Boolean getHelpfulFeedback() { return helpfulFeedback; }
  public void setHelpfulFeedback(Boolean helpfulFeedback) { this.helpfulFeedback = helpfulFeedback; }
  public String getFeedbackComment() { return feedbackComment; }
  public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
