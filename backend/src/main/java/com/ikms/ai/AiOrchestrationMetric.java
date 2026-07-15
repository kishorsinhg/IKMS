package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_orchestration_metric")
public class AiOrchestrationMetric {

  @Id
  private UUID id;

  @Column(name = "interaction_id")
  private UUID interactionId;

  @Column(name = "conversation_id")
  private UUID conversationId;

  @Column(name = "operation_type", nullable = false, length = 32)
  private String operationType;

  @Column(name = "total_latency_ms")
  private Long totalLatencyMs;

  @Column(name = "retrieval_latency_ms")
  private Long retrievalLatencyMs;

  @Column(name = "context_build_latency_ms")
  private Long contextBuildLatencyMs;

  @Column(name = "provider_latency_ms")
  private Long providerLatencyMs;

  @Column(name = "grounding_score")
  private Double groundingScore;

  @Column(name = "retrieval_precision")
  private Double retrievalPrecision;

  @Column(name = "citation_coverage")
  private Double citationCoverage;

  @Column(name = "answer_quality_score")
  private Double answerQualityScore;

  @Column(name = "evidence_count")
  private Integer evidenceCount;

  @Column(name = "warning_count")
  private Integer warningCount;

  @Column(name = "fallback_used", nullable = false)
  private boolean fallbackUsed;

  @Column(name = "provider_name", length = 80)
  private String providerName;

  @Column(name = "model_name", length = 120)
  private String modelName;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getInteractionId() { return interactionId; }
  public void setInteractionId(UUID interactionId) { this.interactionId = interactionId; }
  public UUID getConversationId() { return conversationId; }
  public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
  public String getOperationType() { return operationType; }
  public void setOperationType(String operationType) { this.operationType = operationType; }
  public Long getTotalLatencyMs() { return totalLatencyMs; }
  public void setTotalLatencyMs(Long totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
  public Long getRetrievalLatencyMs() { return retrievalLatencyMs; }
  public void setRetrievalLatencyMs(Long retrievalLatencyMs) { this.retrievalLatencyMs = retrievalLatencyMs; }
  public Long getContextBuildLatencyMs() { return contextBuildLatencyMs; }
  public void setContextBuildLatencyMs(Long contextBuildLatencyMs) { this.contextBuildLatencyMs = contextBuildLatencyMs; }
  public Long getProviderLatencyMs() { return providerLatencyMs; }
  public void setProviderLatencyMs(Long providerLatencyMs) { this.providerLatencyMs = providerLatencyMs; }
  public Double getGroundingScore() { return groundingScore; }
  public void setGroundingScore(Double groundingScore) { this.groundingScore = groundingScore; }
  public Double getRetrievalPrecision() { return retrievalPrecision; }
  public void setRetrievalPrecision(Double retrievalPrecision) { this.retrievalPrecision = retrievalPrecision; }
  public Double getCitationCoverage() { return citationCoverage; }
  public void setCitationCoverage(Double citationCoverage) { this.citationCoverage = citationCoverage; }
  public Double getAnswerQualityScore() { return answerQualityScore; }
  public void setAnswerQualityScore(Double answerQualityScore) { this.answerQualityScore = answerQualityScore; }
  public Integer getEvidenceCount() { return evidenceCount; }
  public void setEvidenceCount(Integer evidenceCount) { this.evidenceCount = evidenceCount; }
  public Integer getWarningCount() { return warningCount; }
  public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }
  public boolean isFallbackUsed() { return fallbackUsed; }
  public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
  public String getProviderName() { return providerName; }
  public void setProviderName(String providerName) { this.providerName = providerName; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
