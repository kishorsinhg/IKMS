package com.ikms.document;

import com.ikms.client.Client;
import com.ikms.email.Email;
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
@Table(name = "document")
public class Document {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id")
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_email_id")
  private Email parentEmail;

  @Column(name = "document_type_id")
  private UUID documentTypeId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(name = "current_version_id")
  private UUID currentVersionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 32)
  private DocumentProcessingStatus processingStatus = DocumentProcessingStatus.INTAKE_RECEIVED;

  @Enumerated(EnumType.STRING)
  @Column(name = "review_status", nullable = false, length = 32)
  private DocumentReviewStatus reviewStatus = DocumentReviewStatus.PENDING_REVIEW;

  @Column(name = "client_match_confidence", precision = 5, scale = 4)
  private BigDecimal clientMatchConfidence;

  @Column(name = "classification_confidence", precision = 5, scale = 4)
  private BigDecimal classificationConfidence;

  @Column(name = "extraction_confidence", precision = 5, scale = 4)
  private BigDecimal extractionConfidence;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private DocumentSource source = DocumentSource.MANUAL_UPLOAD;

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
  public Client getClient() { return client; }
  public void setClient(Client client) { this.client = client; }
  public Email getParentEmail() { return parentEmail; }
  public void setParentEmail(Email parentEmail) { this.parentEmail = parentEmail; }
  public UUID getDocumentTypeId() { return documentTypeId; }
  public void setDocumentTypeId(UUID documentTypeId) { this.documentTypeId = documentTypeId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public UUID getCurrentVersionId() { return currentVersionId; }
  public void setCurrentVersionId(UUID currentVersionId) { this.currentVersionId = currentVersionId; }
  public DocumentProcessingStatus getProcessingStatus() { return processingStatus; }
  public void setProcessingStatus(DocumentProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
  public DocumentReviewStatus getReviewStatus() { return reviewStatus; }
  public void setReviewStatus(DocumentReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
  public BigDecimal getClientMatchConfidence() { return clientMatchConfidence; }
  public void setClientMatchConfidence(BigDecimal clientMatchConfidence) { this.clientMatchConfidence = clientMatchConfidence; }
  public BigDecimal getClassificationConfidence() { return classificationConfidence; }
  public void setClassificationConfidence(BigDecimal classificationConfidence) { this.classificationConfidence = classificationConfidence; }
  public BigDecimal getExtractionConfidence() { return extractionConfidence; }
  public void setExtractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; }
  public DocumentSource getSource() { return source; }
  public void setSource(DocumentSource source) { this.source = source; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
