package com.ikms.quality;

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
@Table(name = "knowledge_quality_issue")
public class KnowledgeQualityIssue {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "snapshot_id")
  private KnowledgeQualitySnapshot snapshot;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(name = "source_type", length = 32)
  private String sourceType;

  @Column(name = "source_id")
  private UUID sourceId;

  @Column(nullable = false, length = 64)
  private String category;

  @Column(name = "issue_type", nullable = false, length = 64)
  private String issueType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private KnowledgeQualityIssueSeverity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private KnowledgeQualityIssueStatus status = KnowledgeQualityIssueStatus.OPEN;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(name = "detail_text", columnDefinition = "TEXT")
  private String detailText;

  @Column(name = "recommendation_type", length = 64)
  private String recommendationType;

  @Column(name = "recommendation_detail", columnDefinition = "TEXT")
  private String recommendationDetail;

  @Column(name = "business_reference_key", length = 64)
  private String businessReferenceKey;

  @Column(name = "score_impact", precision = 5, scale = 4)
  private BigDecimal scoreImpact;

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
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public KnowledgeQualitySnapshot getSnapshot() { return snapshot; }
  public void setSnapshot(KnowledgeQualitySnapshot snapshot) { this.snapshot = snapshot; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getIssueType() { return issueType; }
  public void setIssueType(String issueType) { this.issueType = issueType; }
  public KnowledgeQualityIssueSeverity getSeverity() { return severity; }
  public void setSeverity(KnowledgeQualityIssueSeverity severity) { this.severity = severity; }
  public KnowledgeQualityIssueStatus getStatus() { return status; }
  public void setStatus(KnowledgeQualityIssueStatus status) { this.status = status; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDetailText() { return detailText; }
  public void setDetailText(String detailText) { this.detailText = detailText; }
  public String getRecommendationType() { return recommendationType; }
  public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
  public String getRecommendationDetail() { return recommendationDetail; }
  public void setRecommendationDetail(String recommendationDetail) { this.recommendationDetail = recommendationDetail; }
  public String getBusinessReferenceKey() { return businessReferenceKey; }
  public void setBusinessReferenceKey(String businessReferenceKey) { this.businessReferenceKey = businessReferenceKey; }
  public BigDecimal getScoreImpact() { return scoreImpact; }
  public void setScoreImpact(BigDecimal scoreImpact) { this.scoreImpact = scoreImpact; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
