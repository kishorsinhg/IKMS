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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_processing_finding")
public class DocumentProcessingFinding {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_id", nullable = false)
  private DocumentProcessingJob job;

  @Column(name = "finding_code", nullable = false, length = 96)
  private String findingCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private DocumentProcessingFindingSeverity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 48)
  private DocumentProcessingStage stage;

  @Column(name = "field_key", length = 120)
  private String fieldKey;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "evidence_text", columnDefinition = "TEXT")
  private String evidenceText;

  @Column(name = "source_page")
  private Integer sourcePage;

  @Column(precision = 5, scale = 4)
  private BigDecimal confidence;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private DocumentProcessingFindingStatus status = DocumentProcessingFindingStatus.OPEN;

  @Column(name = "resolution_comment", columnDefinition = "TEXT")
  private String resolutionComment;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public DocumentProcessingJob getJob() { return job; }
  public void setJob(DocumentProcessingJob job) { this.job = job; }
  public String getFindingCode() { return findingCode; }
  public void setFindingCode(String findingCode) { this.findingCode = findingCode; }
  public DocumentProcessingFindingSeverity getSeverity() { return severity; }
  public void setSeverity(DocumentProcessingFindingSeverity severity) { this.severity = severity; }
  public DocumentProcessingStage getStage() { return stage; }
  public void setStage(DocumentProcessingStage stage) { this.stage = stage; }
  public String getFieldKey() { return fieldKey; }
  public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getEvidenceText() { return evidenceText; }
  public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }
  public Integer getSourcePage() { return sourcePage; }
  public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }
  public BigDecimal getConfidence() { return confidence; }
  public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
  public DocumentProcessingFindingStatus getStatus() { return status; }
  public void setStatus(DocumentProcessingFindingStatus status) { this.status = status; }
  public String getResolutionComment() { return resolutionComment; }
  public void setResolutionComment(String resolutionComment) { this.resolutionComment = resolutionComment; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getResolvedAt() { return resolvedAt; }
  public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
