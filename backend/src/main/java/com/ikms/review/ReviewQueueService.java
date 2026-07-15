package com.ikms.review;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.client.ClientService;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataFieldRepository;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentProcessingField;
import com.ikms.document.DocumentProcessingFinding;
import com.ikms.document.DocumentProcessingJob;
import com.ikms.document.DocumentProcessingJobService;
import com.ikms.document.DocumentProcessingStatus;
import com.ikms.document.DocumentPublishingService;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.document.DocumentReviewStatus;
import com.ikms.email.Email;
import com.ikms.email.EmailRepository;
import com.ikms.email.EmailReviewStatus;
import com.ikms.observability.RequestContextHolder;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewQueueService {

  private final ReviewQueueRepository reviewQueueRepository;
  private final ClientService clientService;
  private final DocumentRepository documentRepository;
  private final EmailRepository emailRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentTypeRepository documentTypeRepository;
  private final MetadataFieldRepository metadataFieldRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final EmbeddingIndexService embeddingIndexService;
  private final DocumentProcessingJobService documentProcessingJobService;
  private final DocumentPublishingService documentPublishingService;
  private final AuditService auditService;

  public ReviewQueueService(
      ReviewQueueRepository reviewQueueRepository,
      ClientService clientService,
      DocumentRepository documentRepository,
      EmailRepository emailRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentTypeRepository documentTypeRepository,
      MetadataFieldRepository metadataFieldRepository,
      MetadataValueRepository metadataValueRepository,
      EmbeddingIndexService embeddingIndexService,
      DocumentProcessingJobService documentProcessingJobService,
      DocumentPublishingService documentPublishingService,
      AuditService auditService) {
    this.reviewQueueRepository = reviewQueueRepository;
    this.clientService = clientService;
    this.documentRepository = documentRepository;
    this.emailRepository = emailRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentTypeRepository = documentTypeRepository;
    this.metadataFieldRepository = metadataFieldRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.embeddingIndexService = embeddingIndexService;
    this.documentProcessingJobService = documentProcessingJobService;
    this.documentPublishingService = documentPublishingService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<ReviewContracts.ReviewQueueItemResponse> list(ReviewQueueStatus status, ReviewQueueReason reason) {
    return reviewQueueRepository.findByOptionalStatusAndReason(status, reason).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ReviewContracts.ReviewQueueItemResponse get(UUID itemId) {
    return toResponse(requireItem(itemId));
  }

  public ReviewContracts.ReviewQueueItemResponse createForDocument(UUID documentId, ReviewQueueReason reason) {
    return toResponse(saveIfMissing(ReviewQueueItemType.DOCUMENT, documentId.toString(), reason));
  }

  public ReviewContracts.ReviewQueueItemResponse createForEmail(UUID emailId, ReviewQueueReason reason) {
    return toResponse(saveIfMissing(ReviewQueueItemType.EMAIL, emailId.toString(), reason));
  }

  public ReviewContracts.ReviewQueueItemResponse linkClient(UUID itemId, UUID clientId) {
    ReviewQueueItem item = requireItem(itemId);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.REVIEW_ID, item.getId().toString())) {
      var client = clientService.requireClient(clientId);

      switch (item.getItemType()) {
        case DOCUMENT -> {
          Document document = requireDocument(item.getItemId());
          document.setClient(client);
          documentRepository.save(document);
          documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())
              .ifPresent(version -> embeddingIndexService.indexDocumentVersion(client.getId(), version));
        }
        case EMAIL -> {
          Email email = requireEmail(item.getItemId());
          email.setClient(client);
          emailRepository.save(email);
          embeddingIndexService.indexEmail(client.getId(), email);
          for (Document document : documentRepository.findByClient_IdOrderByCreatedAtDesc(client.getId())) {
            if (document.getParentEmail() != null && email.getId().equals(document.getParentEmail().getId())) {
              documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())
                  .ifPresent(version -> embeddingIndexService.indexDocumentVersion(client.getId(), version));
            }
          }
        }
        case DOCUMENT_VERSION -> throw new IllegalArgumentException("Client linking is not implemented for document versions yet.");
      }

      item.setStatus(ReviewQueueStatus.IN_PROGRESS);
      audit("REVIEW_LINK_CLIENT", item, AuditOutcome.SUCCESS, Map.of("clientId", clientId.toString()));
      return toResponse(reviewQueueRepository.save(item));
    }
  }

  public ReviewContracts.ReviewQueueItemResponse correctMetadata(
      UUID itemId,
      String title,
      UUID documentTypeId,
      Map<String, String> metadataValues,
      String reviewerComment) {
    ReviewQueueItem item = requireItem(itemId);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.REVIEW_ID, item.getId().toString())) {
      if (title == null || title.trim().isEmpty()) {
        throw new IllegalArgumentException("Title is required.");
      }

      if (item.getItemType() == ReviewQueueItemType.DOCUMENT) {
        Document document = requireDocument(item.getItemId());
        document.setTitle(title.trim());
        if (documentTypeId != null) {
          documentTypeRepository.findById(documentTypeId)
              .orElseThrow(() -> new IllegalArgumentException("Document type not found: " + documentTypeId));
          document.setDocumentTypeId(documentTypeId);
        }
        documentRepository.save(document);
        syncMetadataValues(document.getId(), metadataValues);
        DocumentProcessingJob job = documentProcessingJobService.latestForDocument(document.getId());
        if (job != null) {
          documentProcessingJobService.applyReviewerCorrections(job, metadataValues == null ? Map.of() : metadataValues, reviewerComment);
        }
      }

      item.setStatus(ReviewQueueStatus.IN_PROGRESS);
      audit("REVIEW_CORRECT_METADATA", item, AuditOutcome.SUCCESS, Map.of(
          "title", title.trim(),
          "documentTypeId", documentTypeId == null ? "" : documentTypeId.toString(),
          "metadataFieldCount", Integer.toString(metadataValues == null ? 0 : metadataValues.size()),
          "reviewerComment", reviewerComment == null ? "" : reviewerComment));
      return toResponse(reviewQueueRepository.save(item));
    }
  }

  public ReviewContracts.ReviewQueueItemResponse approve(UUID itemId) {
    ReviewQueueItem item = requireItem(itemId);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.REVIEW_ID, item.getId().toString())) {
      switch (item.getItemType()) {
        case DOCUMENT -> {
          Document document = requireDocument(item.getItemId());
          document.setReviewStatus(DocumentReviewStatus.APPROVED);
          document.setProcessingStatus(DocumentProcessingStatus.APPROVED);
          documentRepository.save(document);
          DocumentProcessingJob job = documentProcessingJobService.latestForDocument(document.getId());
          if (job != null) {
            documentProcessingJobService.markApproved(job, "Approved by reviewer");
            documentProcessingJobService.applyApprovalValues(job);
          }
          if (document.getClient() != null) {
            documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())
                .ifPresent(version -> embeddingIndexService.indexDocumentVersion(document.getClient().getId(), version));
            documentPublishingService.publish(document);
            document.setProcessingStatus(DocumentProcessingStatus.INDEXED);
            documentRepository.save(document);
          }
          if (job != null) {
            documentProcessingJobService.markPublished(job);
          }
        }
        case EMAIL -> {
          Email email = requireEmail(item.getItemId());
          email.setReviewStatus(EmailReviewStatus.APPROVED);
          emailRepository.save(email);
        }
        case DOCUMENT_VERSION -> {
        }
      }
      item.setStatus(ReviewQueueStatus.RESOLVED);
      item.setResolvedAt(Instant.now());
      audit("REVIEW_APPROVE", item, AuditOutcome.SUCCESS, Map.of());
      return toResponse(reviewQueueRepository.save(item));
    }
  }

  public ReviewContracts.ReviewQueueItemResponse reject(UUID itemId, String reason) {
    ReviewQueueItem item = requireItem(itemId);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.REVIEW_ID, item.getId().toString())) {
      switch (item.getItemType()) {
        case DOCUMENT -> {
          Document document = requireDocument(item.getItemId());
          document.setReviewStatus(DocumentReviewStatus.REJECTED);
          document.setProcessingStatus(DocumentProcessingStatus.WAITING_REVIEW);
          documentRepository.save(document);
          DocumentProcessingJob job = documentProcessingJobService.latestForDocument(document.getId());
          if (job != null) {
            documentProcessingJobService.markRejected(job, reason);
          }
        }
        case EMAIL -> {
          Email email = requireEmail(item.getItemId());
          email.setReviewStatus(EmailReviewStatus.REJECTED);
          emailRepository.save(email);
        }
        case DOCUMENT_VERSION -> {
        }
      }
      item.setStatus(ReviewQueueStatus.REJECTED);
      item.setResolvedAt(Instant.now());
      audit("REVIEW_REJECT", item, AuditOutcome.SUCCESS, Map.of("reason", reason == null ? "" : reason));
      return toResponse(reviewQueueRepository.save(item));
    }
  }

  public ReviewContracts.ReviewQueueItemResponse retry(UUID itemId, String reviewerComment) {
    ReviewQueueItem item = requireItem(itemId);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.REVIEW_ID, item.getId().toString())) {
      if (item.getItemType() != ReviewQueueItemType.DOCUMENT) {
        throw new IllegalArgumentException("Retry is only supported for document processing jobs.");
      }

      Document document = requireDocument(item.getItemId());
      DocumentProcessingJob job = documentProcessingJobService.latestForDocument(document.getId());
      if (job == null) {
        throw new IllegalArgumentException("No processing job exists for document " + document.getId());
      }
      documentProcessingJobService.prepareRetry(job);
      if (reviewerComment != null && !reviewerComment.isBlank()) {
        job.setReviewerComment(reviewerComment.trim());
      }
      item.setStatus(ReviewQueueStatus.IN_PROGRESS);
      document.setProcessingStatus(DocumentProcessingStatus.INTAKE_RECEIVED);
      document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
      documentRepository.save(document);
      audit("REVIEW_RETRY", item, AuditOutcome.SUCCESS, Map.of("jobId", job.getId().toString()));
      return toResponse(reviewQueueRepository.save(item));
    }
  }

  private ReviewQueueItem saveIfMissing(ReviewQueueItemType itemType, String itemId, ReviewQueueReason reason) {
    return reviewQueueRepository.findByItemTypeAndItemIdAndStatusIn(
            itemType,
            itemId,
            List.of(ReviewQueueStatus.OPEN, ReviewQueueStatus.IN_PROGRESS))
        .map(existing -> {
          if (reason == ReviewQueueReason.PROMPT_INJECTION_RISK && existing.getReason() != ReviewQueueReason.PROMPT_INJECTION_RISK) {
            existing.setReason(reason);
            return reviewQueueRepository.save(existing);
          }
          return existing;
        })
        .orElseGet(() -> {
          ReviewQueueItem item = new ReviewQueueItem();
          item.setItemType(itemType);
          item.setItemId(itemId);
          item.setReason(reason);
          item.setStatus(ReviewQueueStatus.OPEN);
          return reviewQueueRepository.save(item);
        });
  }

  private ReviewQueueItem requireItem(UUID itemId) {
    return reviewQueueRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("Review queue item not found: " + itemId));
  }

  private Document requireDocument(String itemId) {
    return documentRepository.findById(UUID.fromString(itemId))
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + itemId));
  }

  private Email requireEmail(String itemId) {
    return emailRepository.findById(UUID.fromString(itemId))
        .orElseThrow(() -> new IllegalArgumentException("Email not found: " + itemId));
  }

  private void audit(String action, ReviewQueueItem item, AuditOutcome outcome, Map<String, String> details) {
    auditService.write(new AuditEvent(
        Instant.now(),
        "REVIEW",
        action,
        outcome,
        null,
        null,
        "ReviewQueueItem",
        item.getId().toString(),
        false,
        details));
  }

  private ReviewContracts.ReviewQueueItemResponse toResponse(ReviewQueueItem item) {
    String title = null;
    UUID clientId = null;
    UUID documentTypeId = null;
    Map<String, String> metadataValues = Map.of();

    if (item.getItemType() == ReviewQueueItemType.DOCUMENT) {
      Document document = requireDocument(item.getItemId());
      title = document.getTitle();
      clientId = document.getClient() == null ? null : document.getClient().getId();
      documentTypeId = document.getDocumentTypeId();
      metadataValues = metadataMap("DOCUMENT", document.getId());
    } else if (item.getItemType() == ReviewQueueItemType.EMAIL) {
      Email email = requireEmail(item.getItemId());
      title = email.getSubject();
      clientId = email.getClient() == null ? null : email.getClient().getId();
    }

    return new ReviewContracts.ReviewQueueItemResponse(
        item.getId(),
        item.getItemType(),
        item.getItemId(),
        item.getReason(),
        item.getStatus(),
        item.getAssignedTo(),
        title,
        clientId,
        documentTypeId,
        metadataValues,
        processingJob(item));
  }

  private ReviewContracts.ProcessingJobResponse processingJob(ReviewQueueItem item) {
    if (item.getItemType() != ReviewQueueItemType.DOCUMENT) {
      return null;
    }
    DocumentProcessingJob job = documentProcessingJobService.latestForDocument(UUID.fromString(item.getItemId()));
    if (job == null) {
      return null;
    }
    List<DocumentProcessingField> fields = documentProcessingJobService.fields(job.getId());
    List<DocumentProcessingFinding> findings = documentProcessingJobService.findings(job.getId());
    return new ReviewContracts.ProcessingJobResponse(
        job.getId(),
        job.getStatus().name(),
        job.getCurrentStage().name(),
        job.getRetryCount(),
        job.getOverallConfidence(),
        job.getOcrConfidence(),
        job.getClassificationConfidence(),
        job.getMetadataConfidence(),
        job.getBusinessReferenceConfidence(),
        job.getValidationConfidence(),
        job.getDuplicateConfidence(),
        job.getLanguage(),
        job.getOcrProvider(),
        job.getClassificationProvider(),
        job.getLastErrorCode(),
        job.getLastErrorMessage(),
        job.getReviewerComment(),
        job.getStartedAt(),
        job.getReviewRequestedAt(),
        job.getApprovedAt(),
        job.getRejectedAt(),
        job.getPublishedAt(),
        job.getCompletedAt(),
        fields.stream()
            .map(field -> new ReviewContracts.ProcessingFieldResponse(
                field.getFieldKey(),
                field.getFieldLabel(),
                field.getFieldType().name(),
                field.getBusinessReferenceType(),
                field.getExtractedValue(),
                field.getCorrectedValue(),
                field.getApprovedValue(),
                field.getConfidence(),
                field.getSourceType(),
                field.getExtractionMethod(),
                field.getSourcePage(),
                field.isRequiredFlag(),
                field.getValidationState().name()))
            .toList(),
        findings.stream()
            .map(finding -> new ReviewContracts.ProcessingFindingResponse(
                finding.getFindingCode(),
                finding.getSeverity().name(),
                finding.getStage().name(),
                finding.getFieldKey(),
                finding.getMessage(),
                finding.getEvidenceText(),
                finding.getSourcePage(),
                finding.getConfidence(),
                finding.getStatus().name(),
                finding.getResolutionComment(),
                finding.getCreatedAt(),
                finding.getResolvedAt()))
            .toList());
  }

  private void syncMetadataValues(UUID documentId, Map<String, String> metadataValues) {
    if (metadataValues == null || metadataValues.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> entry : metadataValues.entrySet()) {
      String fieldKey = entry.getKey() == null ? "" : entry.getKey().trim();
      String value = entry.getValue() == null ? "" : entry.getValue().trim();
      if (fieldKey.isEmpty() || value.isEmpty()) {
        continue;
      }

      MetadataField field = metadataFieldRepository.findByFieldKeyIgnoreCase(fieldKey)
          .orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + fieldKey));

      MetadataValue metadataValue = metadataValueRepository
          .findByOwnerTypeAndOwnerIdAndField_Id("DOCUMENT", documentId, field.getId())
          .orElseGet(MetadataValue::new);
      metadataValue.setOwnerType("DOCUMENT");
      metadataValue.setOwnerId(documentId);
      metadataValue.setField(field);
      metadataValue.setTextValue(value);
      metadataValueRepository.save(metadataValue);
    }
  }

  private Map<String, String> metadataMap(String ownerType, UUID ownerId) {
    Map<String, String> values = new LinkedHashMap<>();
    for (MetadataValue metadataValue : metadataValueRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)) {
      values.put(metadataValue.getField().getFieldKey(), metadataValue.getTextValue());
    }
    return values;
  }
}
