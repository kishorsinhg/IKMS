package com.ikms.review;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.client.ClientService;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataFieldRepository;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentReviewStatus;
import com.ikms.email.Email;
import com.ikms.email.EmailRepository;
import com.ikms.email.EmailReviewStatus;
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
  private final DocumentTypeRepository documentTypeRepository;
  private final MetadataFieldRepository metadataFieldRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final AuditService auditService;

  public ReviewQueueService(
      ReviewQueueRepository reviewQueueRepository,
      ClientService clientService,
      DocumentRepository documentRepository,
      EmailRepository emailRepository,
      DocumentTypeRepository documentTypeRepository,
      MetadataFieldRepository metadataFieldRepository,
      MetadataValueRepository metadataValueRepository,
      AuditService auditService) {
    this.reviewQueueRepository = reviewQueueRepository;
    this.clientService = clientService;
    this.documentRepository = documentRepository;
    this.emailRepository = emailRepository;
    this.documentTypeRepository = documentTypeRepository;
    this.metadataFieldRepository = metadataFieldRepository;
    this.metadataValueRepository = metadataValueRepository;
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
    var client = clientService.requireClient(clientId);

    switch (item.getItemType()) {
      case DOCUMENT -> {
        Document document = requireDocument(item.getItemId());
        document.setClient(client);
        documentRepository.save(document);
      }
      case EMAIL -> {
        Email email = requireEmail(item.getItemId());
        email.setClient(client);
        emailRepository.save(email);
      }
      case DOCUMENT_VERSION -> throw new IllegalArgumentException("Client linking is not implemented for document versions yet.");
    }

    item.setStatus(ReviewQueueStatus.IN_PROGRESS);
    audit("REVIEW_LINK_CLIENT", item, AuditOutcome.SUCCESS, Map.of("clientId", clientId.toString()));
    return toResponse(reviewQueueRepository.save(item));
  }

  public ReviewContracts.ReviewQueueItemResponse correctMetadata(
      UUID itemId,
      String title,
      UUID documentTypeId,
      Map<String, String> metadataValues) {
    ReviewQueueItem item = requireItem(itemId);
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
    }

    item.setStatus(ReviewQueueStatus.IN_PROGRESS);
    audit("REVIEW_CORRECT_METADATA", item, AuditOutcome.SUCCESS, Map.of(
        "title", title.trim(),
        "documentTypeId", documentTypeId == null ? "" : documentTypeId.toString(),
        "metadataFieldCount", Integer.toString(metadataValues == null ? 0 : metadataValues.size())));
    return toResponse(reviewQueueRepository.save(item));
  }

  public ReviewContracts.ReviewQueueItemResponse approve(UUID itemId) {
    ReviewQueueItem item = requireItem(itemId);
    switch (item.getItemType()) {
      case DOCUMENT -> {
        Document document = requireDocument(item.getItemId());
        document.setReviewStatus(DocumentReviewStatus.APPROVED);
        documentRepository.save(document);
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

  public ReviewContracts.ReviewQueueItemResponse reject(UUID itemId, String reason) {
    ReviewQueueItem item = requireItem(itemId);
    switch (item.getItemType()) {
      case DOCUMENT -> {
        Document document = requireDocument(item.getItemId());
        document.setReviewStatus(DocumentReviewStatus.REJECTED);
        documentRepository.save(document);
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
        metadataValues);
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
