package com.ikms.document;

import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataFieldRepository;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentProcessingJobService {

  private final DocumentProcessingJobRepository jobRepository;
  private final DocumentProcessingFieldRepository fieldRepository;
  private final DocumentProcessingFindingRepository findingRepository;
  private final MetadataFieldRepository metadataFieldRepository;
  private final MetadataValueRepository metadataValueRepository;

  public DocumentProcessingJobService(
      DocumentProcessingJobRepository jobRepository,
      DocumentProcessingFieldRepository fieldRepository,
      DocumentProcessingFindingRepository findingRepository,
      MetadataFieldRepository metadataFieldRepository,
      MetadataValueRepository metadataValueRepository) {
    this.jobRepository = jobRepository;
    this.fieldRepository = fieldRepository;
    this.findingRepository = findingRepository;
    this.metadataFieldRepository = metadataFieldRepository;
    this.metadataValueRepository = metadataValueRepository;
  }

  public DocumentProcessingJob create(Document document, DocumentVersion version, UUID clientIdHint) {
    DocumentProcessingJob job = new DocumentProcessingJob();
    job.setDocument(document);
    job.setDocumentVersion(version);
    job.setClientId(clientIdHint);
    job.setStatus(DocumentProcessingJobStatus.QUEUED);
    job.setCurrentStage(DocumentProcessingStage.QUEUED);
    return jobRepository.save(job);
  }

  public DocumentProcessingJob markRunning(DocumentProcessingJob job, DocumentProcessingStage stage) {
    job.setStatus(job.getRetryCount() > 0 ? DocumentProcessingJobStatus.RETRYING : DocumentProcessingJobStatus.RUNNING);
    job.setCurrentStage(stage);
    if (job.getStartedAt() == null) {
      job.setStartedAt(Instant.now());
    }
    return jobRepository.save(job);
  }

  public DocumentProcessingJob advance(DocumentProcessingJob job, DocumentProcessingStage stage) {
    job.setCurrentStage(stage);
    return jobRepository.save(job);
  }

  public void updateConfidences(
      DocumentProcessingJob job,
      String language,
      String ocrProvider,
      String classificationProvider,
      BigDecimal ocrConfidence,
      BigDecimal classificationConfidence,
      BigDecimal metadataConfidence,
      BigDecimal businessReferenceConfidence,
      BigDecimal validationConfidence,
      BigDecimal duplicateConfidence,
      BigDecimal overallConfidence) {
    job.setLanguage(language);
    job.setOcrProvider(ocrProvider);
    job.setClassificationProvider(classificationProvider);
    job.setOcrConfidence(ocrConfidence);
    job.setClassificationConfidence(classificationConfidence);
    job.setMetadataConfidence(metadataConfidence);
    job.setBusinessReferenceConfidence(businessReferenceConfidence);
    job.setValidationConfidence(validationConfidence);
    job.setDuplicateConfidence(duplicateConfidence);
    job.setOverallConfidence(overallConfidence);
    jobRepository.save(job);
  }

  public void replaceFields(DocumentProcessingJob job, List<DocumentMetadataExtractionService.ExtractedField> fields) {
    fieldRepository.findByJob_IdOrderByFieldKeyAsc(job.getId()).forEach(fieldRepository::delete);
    for (DocumentMetadataExtractionService.ExtractedField extracted : fields) {
      DocumentProcessingField field = new DocumentProcessingField();
      field.setJob(job);
      field.setFieldKey(extracted.fieldKey());
      field.setFieldLabel(extracted.fieldLabel());
      field.setFieldType(extracted.fieldType());
      field.setBusinessReferenceType(extracted.businessReferenceType());
      field.setExtractedValue(extracted.normalizedValue());
      field.setConfidence(extracted.confidence());
      field.setSourceType(extracted.sourceType());
      field.setExtractionMethod(extracted.extractionMethod());
      field.setSourcePage(extracted.sourcePage());
      field.setRequiredFlag(extracted.required());
      field.setValidationState(extracted.empty()
          ? DocumentProcessingFieldValidationState.MISSING
          : DocumentProcessingFieldValidationState.EXTRACTED);
      fieldRepository.save(field);
      syncMetadataValue(job.getDocument().getId(), extracted);
    }
  }

  public void replaceFindings(DocumentProcessingJob job, List<DocumentValidationService.FindingDraft> findings) {
    findingRepository.findByJob_IdOrderByCreatedAtAsc(job.getId()).forEach(findingRepository::delete);
    for (DocumentValidationService.FindingDraft draft : findings) {
      DocumentProcessingFinding finding = new DocumentProcessingFinding();
      finding.setJob(job);
      finding.setFindingCode(draft.code());
      finding.setSeverity(draft.severity());
      finding.setStage(draft.stage());
      finding.setFieldKey(draft.fieldKey());
      finding.setMessage(draft.message());
      finding.setEvidenceText(draft.evidenceText());
      finding.setSourcePage(draft.sourcePage());
      finding.setConfidence(draft.confidence());
      findingRepository.save(finding);
    }
  }

  public DocumentProcessingJob markWaitingReview(DocumentProcessingJob job) {
    job.setStatus(DocumentProcessingJobStatus.WAITING_REVIEW);
    job.setCurrentStage(DocumentProcessingStage.REVIEW_QUEUE);
    job.setReviewRequestedAt(Instant.now());
    return jobRepository.save(job);
  }

  public DocumentProcessingJob markApproved(DocumentProcessingJob job, String reviewerComment) {
    job.setStatus(DocumentProcessingJobStatus.APPROVED);
    job.setCurrentStage(DocumentProcessingStage.APPROVAL);
    job.setApprovedAt(Instant.now());
    job.setReviewerComment(reviewerComment);
    return jobRepository.save(job);
  }

  public DocumentProcessingJob markRejected(DocumentProcessingJob job, String reviewerComment) {
    job.setStatus(DocumentProcessingJobStatus.REJECTED);
    job.setCurrentStage(DocumentProcessingStage.REVIEW_CORRECTION);
    job.setRejectedAt(Instant.now());
    job.setReviewerComment(reviewerComment);
    return jobRepository.save(job);
  }

  public DocumentProcessingJob markPublished(DocumentProcessingJob job) {
    job.setCurrentStage(DocumentProcessingStage.RETRIEVAL_READY);
    job.setStatus(DocumentProcessingJobStatus.COMPLETED);
    job.setPublishedAt(Instant.now());
    job.setCompletedAt(Instant.now());
    return jobRepository.save(job);
  }

  public DocumentProcessingJob markFailed(DocumentProcessingJob job, String errorCode, String errorMessage) {
    job.setStatus(DocumentProcessingJobStatus.FAILED);
    job.setCurrentStage(DocumentProcessingStage.FAILED);
    job.setLastErrorCode(errorCode);
    job.setLastErrorMessage(errorMessage);
    job.setCompletedAt(Instant.now());
    return jobRepository.save(job);
  }

  public DocumentProcessingJob prepareRetry(DocumentProcessingJob job) {
    job.setRetryCount(job.getRetryCount() + 1);
    job.setStatus(DocumentProcessingJobStatus.RETRYING);
    job.setCurrentStage(DocumentProcessingStage.QUEUED);
    job.setLastErrorCode(null);
    job.setLastErrorMessage(null);
    job.setCompletedAt(null);
    return jobRepository.save(job);
  }

  public DocumentProcessingJob latestForDocument(UUID documentId) {
    return jobRepository.findTopByDocument_IdOrderByCreatedAtDesc(documentId).orElse(null);
  }

  public DocumentProcessingJob latestForVersion(UUID documentVersionId) {
    return jobRepository.findTopByDocumentVersion_IdOrderByCreatedAtDesc(documentVersionId).orElse(null);
  }

  public List<DocumentProcessingField> fields(UUID jobId) {
    return fieldRepository.findByJob_IdOrderByFieldKeyAsc(jobId);
  }

  public List<DocumentProcessingFinding> findings(UUID jobId) {
    return findingRepository.findByJob_IdOrderByCreatedAtAsc(jobId);
  }

  public void applyReviewerCorrections(DocumentProcessingJob job, Map<String, String> metadataValues, String reviewerComment) {
    for (Map.Entry<String, String> entry : metadataValues.entrySet()) {
      fieldRepository.findByJob_IdAndFieldKeyIgnoreCase(job.getId(), entry.getKey()).ifPresent(field -> {
        field.setCorrectedValue(entry.getValue() == null ? null : entry.getValue().trim());
        field.setValidationState(DocumentProcessingFieldValidationState.CORRECTED);
        fieldRepository.save(field);
      });
    }
    if (reviewerComment != null && !reviewerComment.isBlank()) {
      job.setReviewerComment(reviewerComment.trim());
      jobRepository.save(job);
    }
  }

  public void applyApprovalValues(DocumentProcessingJob job) {
    for (DocumentProcessingField field : fieldRepository.findByJob_IdOrderByFieldKeyAsc(job.getId())) {
      String approvedValue = field.getCorrectedValue() != null && !field.getCorrectedValue().isBlank()
          ? field.getCorrectedValue()
          : field.getExtractedValue();
      field.setApprovedValue(approvedValue);
      field.setValidationState(DocumentProcessingFieldValidationState.APPROVED);
      fieldRepository.save(field);
    }
  }

  private void syncMetadataValue(UUID documentId, DocumentMetadataExtractionService.ExtractedField extracted) {
    if (extracted.empty()) {
      return;
    }
    MetadataField field = metadataFieldRepository.findByFieldKeyIgnoreCase(extracted.fieldKey())
        .orElseGet(() -> {
          MetadataField created = new MetadataField();
          created.setFieldKey(extracted.fieldKey());
          created.setLabel(extracted.fieldLabel());
          created.setPii(false);
          created.setActive(true);
          return metadataFieldRepository.save(created);
        });

    MetadataValue metadataValue = metadataValueRepository
        .findByOwnerTypeAndOwnerIdAndField_Id("DOCUMENT", documentId, field.getId())
        .orElseGet(MetadataValue::new);
    metadataValue.setOwnerType("DOCUMENT");
    metadataValue.setOwnerId(documentId);
    metadataValue.setField(field);
    metadataValue.setTextValue(extracted.normalizedValue());
    metadataValueRepository.save(metadataValue);
  }
}
