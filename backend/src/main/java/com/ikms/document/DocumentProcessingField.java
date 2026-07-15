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
@Table(name = "document_processing_field")
public class DocumentProcessingField {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_id", nullable = false)
  private DocumentProcessingJob job;

  @Column(name = "field_key", nullable = false, length = 120)
  private String fieldKey;

  @Column(name = "field_label", nullable = false, length = 160)
  private String fieldLabel;

  @Enumerated(EnumType.STRING)
  @Column(name = "field_type", nullable = false, length = 48)
  private DocumentProcessingFieldType fieldType;

  @Column(name = "business_reference_type", length = 48)
  private String businessReferenceType;

  @Column(name = "extracted_value", columnDefinition = "TEXT")
  private String extractedValue;

  @Column(name = "corrected_value", columnDefinition = "TEXT")
  private String correctedValue;

  @Column(name = "approved_value", columnDefinition = "TEXT")
  private String approvedValue;

  @Column(precision = 5, scale = 4)
  private BigDecimal confidence;

  @Column(name = "source_type", nullable = false, length = 64)
  private String sourceType;

  @Column(name = "extraction_method", nullable = false, length = 64)
  private String extractionMethod;

  @Column(name = "source_page")
  private Integer sourcePage;

  @Column(name = "required_flag", nullable = false)
  private boolean requiredFlag;

  @Enumerated(EnumType.STRING)
  @Column(name = "validation_state", nullable = false, length = 32)
  private DocumentProcessingFieldValidationState validationState = DocumentProcessingFieldValidationState.EXTRACTED;

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
  public DocumentProcessingJob getJob() { return job; }
  public void setJob(DocumentProcessingJob job) { this.job = job; }
  public String getFieldKey() { return fieldKey; }
  public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
  public String getFieldLabel() { return fieldLabel; }
  public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
  public DocumentProcessingFieldType getFieldType() { return fieldType; }
  public void setFieldType(DocumentProcessingFieldType fieldType) { this.fieldType = fieldType; }
  public String getBusinessReferenceType() { return businessReferenceType; }
  public void setBusinessReferenceType(String businessReferenceType) { this.businessReferenceType = businessReferenceType; }
  public String getExtractedValue() { return extractedValue; }
  public void setExtractedValue(String extractedValue) { this.extractedValue = extractedValue; }
  public String getCorrectedValue() { return correctedValue; }
  public void setCorrectedValue(String correctedValue) { this.correctedValue = correctedValue; }
  public String getApprovedValue() { return approvedValue; }
  public void setApprovedValue(String approvedValue) { this.approvedValue = approvedValue; }
  public BigDecimal getConfidence() { return confidence; }
  public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public String getExtractionMethod() { return extractionMethod; }
  public void setExtractionMethod(String extractionMethod) { this.extractionMethod = extractionMethod; }
  public Integer getSourcePage() { return sourcePage; }
  public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }
  public boolean isRequiredFlag() { return requiredFlag; }
  public void setRequiredFlag(boolean requiredFlag) { this.requiredFlag = requiredFlag; }
  public DocumentProcessingFieldValidationState getValidationState() { return validationState; }
  public void setValidationState(DocumentProcessingFieldValidationState validationState) { this.validationState = validationState; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
