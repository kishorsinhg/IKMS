package com.ikms.quality;

import com.ikms.client.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_quality_snapshot")
public class KnowledgeQualitySnapshot {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(name = "overall_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal overallScore;

  @Column(name = "completeness_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal completenessScore;

  @Column(name = "business_reference_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal businessReferenceScore;

  @Column(name = "linkage_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal linkageScore;

  @Column(name = "duplicate_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal duplicateScore;

  @Column(name = "timeline_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal timelineScore;

  @Column(name = "version_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal versionScore;

  @Column(name = "retrieval_readiness_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal retrievalReadinessScore;

  @Column(name = "ai_quality_score", nullable = false, precision = 5, scale = 4)
  private BigDecimal aiQualityScore;

  @Column(name = "issue_count", nullable = false)
  private int issueCount;

  @Column(name = "open_issue_count", nullable = false)
  private int openIssueCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "readiness_state", nullable = false, length = 32)
  private KnowledgeQualityReadinessState readinessState;

  @Column(name = "summary_text", columnDefinition = "TEXT")
  private String summaryText;

  @Column(name = "evaluated_at", nullable = false)
  private Instant evaluatedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    evaluatedAt = evaluatedAt == null ? now : evaluatedAt;
    updatedAt = updatedAt == null ? now : updatedAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Client getClient() { return client; }
  public void setClient(Client client) { this.client = client; }
  public BigDecimal getOverallScore() { return overallScore; }
  public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }
  public BigDecimal getCompletenessScore() { return completenessScore; }
  public void setCompletenessScore(BigDecimal completenessScore) { this.completenessScore = completenessScore; }
  public BigDecimal getBusinessReferenceScore() { return businessReferenceScore; }
  public void setBusinessReferenceScore(BigDecimal businessReferenceScore) { this.businessReferenceScore = businessReferenceScore; }
  public BigDecimal getLinkageScore() { return linkageScore; }
  public void setLinkageScore(BigDecimal linkageScore) { this.linkageScore = linkageScore; }
  public BigDecimal getDuplicateScore() { return duplicateScore; }
  public void setDuplicateScore(BigDecimal duplicateScore) { this.duplicateScore = duplicateScore; }
  public BigDecimal getTimelineScore() { return timelineScore; }
  public void setTimelineScore(BigDecimal timelineScore) { this.timelineScore = timelineScore; }
  public BigDecimal getVersionScore() { return versionScore; }
  public void setVersionScore(BigDecimal versionScore) { this.versionScore = versionScore; }
  public BigDecimal getRetrievalReadinessScore() { return retrievalReadinessScore; }
  public void setRetrievalReadinessScore(BigDecimal retrievalReadinessScore) { this.retrievalReadinessScore = retrievalReadinessScore; }
  public BigDecimal getAiQualityScore() { return aiQualityScore; }
  public void setAiQualityScore(BigDecimal aiQualityScore) { this.aiQualityScore = aiQualityScore; }
  public int getIssueCount() { return issueCount; }
  public void setIssueCount(int issueCount) { this.issueCount = issueCount; }
  public int getOpenIssueCount() { return openIssueCount; }
  public void setOpenIssueCount(int openIssueCount) { this.openIssueCount = openIssueCount; }
  public KnowledgeQualityReadinessState getReadinessState() { return readinessState; }
  public void setReadinessState(KnowledgeQualityReadinessState readinessState) { this.readinessState = readinessState; }
  public String getSummaryText() { return summaryText; }
  public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
  public Instant getEvaluatedAt() { return evaluatedAt; }
  public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
