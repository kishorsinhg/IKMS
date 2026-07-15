package com.ikms.document;

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
@Table(name = "document_processing_job")
public class DocumentProcessingJob {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_version_id", nullable = false)
  private DocumentVersion documentVersion;

  @Column(name = "client_id")
  private UUID clientId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private DocumentProcessingJobStatus status = DocumentProcessingJobStatus.QUEUED;

  @Enumerated(EnumType.STRING)
  @Column(name = "current_stage", nullable = false, length = 48)
  private DocumentProcessingStage currentStage = DocumentProcessingStage.QUEUED;

  @Column(length = 32)
  private String language;

  @Column(name = "ocr_provider", length = 120)
  private String ocrProvider;

  @Column(name = "classification_provider", length = 120)
  private String classificationProvider;

  @Column(name = "overall_confidence", precision = 5, scale = 4)
  private BigDecimal overallConfidence;

  @Column(name = "ocr_confidence", precision = 5, scale = 4)
  private BigDecimal ocrConfidence;

  @Column(name = "classification_confidence", precision = 5, scale = 4)
  private BigDecimal classificationConfidence;

  @Column(name = "metadata_confidence", precision = 5, scale = 4)
  private BigDecimal metadataConfidence;

  @Column(name = "business_reference_confidence", precision = 5, scale = 4)
  private BigDecimal businessReferenceConfidence;

  @Column(name = "validation_confidence", precision = 5, scale = 4)
  private BigDecimal validationConfidence;

  @Column(name = "duplicate_confidence", precision = 5, scale = 4)
  private BigDecimal duplicateConfidence;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "last_error_code", length = 64)
  private String lastErrorCode;

  @Column(name = "last_error_message", columnDefinition = "TEXT")
  private String lastErrorMessage;

  @Column(name = "reviewer_comment", columnDefinition = "TEXT")
  private String reviewerComment;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "review_requested_at")
  private Instant reviewRequestedAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejected_at")
  private Instant rejectedAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

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
  public Document getDocument() { return document; }
  public void setDocument(Document document) { this.document = document; }
  public DocumentVersion getDocumentVersion() { return documentVersion; }
  public void setDocumentVersion(DocumentVersion documentVersion) { this.documentVersion = documentVersion; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public DocumentProcessingJobStatus getStatus() { return status; }
  public void setStatus(DocumentProcessingJobStatus status) { this.status = status; }
  public DocumentProcessingStage getCurrentStage() { return currentStage; }
  public void setCurrentStage(DocumentProcessingStage currentStage) { this.currentStage = currentStage; }
  public String getLanguage() { return language; }
  public void setLanguage(String language) { this.language = language; }
  public String getOcrProvider() { return ocrProvider; }
  public void setOcrProvider(String ocrProvider) { this.ocrProvider = ocrProvider; }
  public String getClassificationProvider() { return classificationProvider; }
  public void setClassificationProvider(String classificationProvider) { this.classificationProvider = classificationProvider; }
  public BigDecimal getOverallConfidence() { return overallConfidence; }
  public void setOverallConfidence(BigDecimal overallConfidence) { this.overallConfidence = overallConfidence; }
  public BigDecimal getOcrConfidence() { return ocrConfidence; }
  public void setOcrConfidence(BigDecimal ocrConfidence) { this.ocrConfidence = ocrConfidence; }
  public BigDecimal getClassificationConfidence() { return classificationConfidence; }
  public void setClassificationConfidence(BigDecimal classificationConfidence) { this.classificationConfidence = classificationConfidence; }
  public BigDecimal getMetadataConfidence() { return metadataConfidence; }
  public void setMetadataConfidence(BigDecimal metadataConfidence) { this.metadataConfidence = metadataConfidence; }
  public BigDecimal getBusinessReferenceConfidence() { return businessReferenceConfidence; }
  public void setBusinessReferenceConfidence(BigDecimal businessReferenceConfidence) { this.businessReferenceConfidence = businessReferenceConfidence; }
  public BigDecimal getValidationConfidence() { return validationConfidence; }
  public void setValidationConfidence(BigDecimal validationConfidence) { this.validationConfidence = validationConfidence; }
  public BigDecimal getDuplicateConfidence() { return duplicateConfidence; }
  public void setDuplicateConfidence(BigDecimal duplicateConfidence) { this.duplicateConfidence = duplicateConfidence; }
  public int getRetryCount() { return retryCount; }
  public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
  public String getLastErrorCode() { return lastErrorCode; }
  public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }
  public String getLastErrorMessage() { return lastErrorMessage; }
  public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
  public String getReviewerComment() { return reviewerComment; }
  public void setReviewerComment(String reviewerComment) { this.reviewerComment = reviewerComment; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Instant getReviewRequestedAt() { return reviewRequestedAt; }
  public void setReviewRequestedAt(Instant reviewRequestedAt) { this.reviewRequestedAt = reviewRequestedAt; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
  public Instant getRejectedAt() { return rejectedAt; }
  public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
  public Instant getPublishedAt() { return publishedAt; }
  public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
