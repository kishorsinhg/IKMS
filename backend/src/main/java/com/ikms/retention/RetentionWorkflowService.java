package com.ikms.retention;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.EmailRepository;
import com.ikms.note.NoteRepository;
import com.ikms.storage.FileStorageService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RetentionWorkflowService {

  private final AuditService auditService;
  private final RetentionRecordRepository retentionRecordRepository;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final FileStorageService fileStorageService;

  public RetentionWorkflowService(
      AuditService auditService,
      RetentionRecordRepository retentionRecordRepository,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      MetadataValueRepository metadataValueRepository,
      EmbeddingChunkRepository embeddingChunkRepository,
      FileStorageService fileStorageService) {
    this.auditService = auditService;
    this.retentionRecordRepository = retentionRecordRepository;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.fileStorageService = fileStorageService;
  }

  public RetentionDecision evaluate(RetentionRequest request) {
    RetentionRecord record = retentionRecordRepository.findByTargetTypeAndTargetId(request.targetType(), request.targetId())
        .orElseGet(RetentionRecord::new);
    record.setTargetType(request.targetType());
    record.setTargetId(request.targetId());
    record.setClientId(request.clientId());
    if (request.minimumRetentionUntil() != null) {
      record.setMinimumRetentionUntil(request.minimumRetentionUntil());
    }
    record.setHoldType(request.holdType());
    record.setRetentionPolicyKey(request.retentionPolicyKey());
    record.setReviewAt(request.reviewAt());
    record.setArchivalEligibleAt(request.archivalEligibleAt());
    record.setDisposalEligibleAt(request.disposalEligibleAt());
    if (request.action() == RetentionAction.APPLY_LEGAL_HOLD) {
      record.setLegalHold(true);
    } else if (request.action() == RetentionAction.RELEASE_LEGAL_HOLD) {
      record.setLegalHold(false);
    } else {
      record.setLegalHold(request.legalHold() || record.isLegalHold());
    }

    if (request.action() == RetentionAction.DELETE || request.action() == RetentionAction.ANONYMIZE) {
      if (record.isLegalHold()) {
        return auditAndReturn(request, record, false, "Legal hold prevents destructive retention actions.");
      }
      if (record.getMinimumRetentionUntil() != null && record.getMinimumRetentionUntil().isAfter(Instant.now())) {
        return auditAndReturn(request, record, false, "Minimum retention period has not elapsed.");
      }
    }

    if (request.action() == RetentionAction.RELEASE_LEGAL_HOLD
        && retentionRecordRepository.findByTargetTypeAndTargetId(request.targetType(), request.targetId())
            .map(existing -> !existing.isLegalHold())
            .orElse(!request.legalHold())) {
      return auditAndReturn(request, record, false, "Cannot release a legal hold that is not active.");
    }

    execute(request);
    return auditAndReturn(request, record, true, "Retention action accepted for workflow execution.");
  }

  private void execute(RetentionRequest request) {
    switch (request.action()) {
      case APPLY_LEGAL_HOLD, RELEASE_LEGAL_HOLD -> {
      }
      case DELETE -> deleteTarget(request);
      case ANONYMIZE -> anonymizeTarget(request);
    }
  }

  private void deleteTarget(RetentionRequest request) {
    switch (request.targetType().toUpperCase()) {
      case "NOTE" -> noteRepository.findById(UUID.fromString(request.targetId())).ifPresent(noteRepository::delete);
      case "DOCUMENT" -> documentRepository.findById(UUID.fromString(request.targetId())).ifPresent(document -> {
        metadataValueRepository.deleteByOwnerTypeAndOwnerId("DOCUMENT", document.getId());
        embeddingChunkRepository.deleteBySourceTypeAndSourceId("DOCUMENT", document.getCurrentVersionId() == null ? document.getId() : document.getCurrentVersionId());
        documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).ifPresent(this::deleteVersionFiles);
        documentVersionRepository.findTopByDocument_IdOrderByVersionNumberDesc(document.getId()).ifPresent(this::deleteVersionFiles);
        documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).ifPresent(documentVersionRepository::delete);
        documentRepository.delete(document);
      });
      case "EMAIL" -> emailRepository.findById(UUID.fromString(request.targetId())).ifPresent(email -> {
        if (documentRepository.countByParentEmail_Id(email.getId()) > 0) {
          throw new IllegalStateException("Cannot delete email while linked documents still exist.");
        }
        emailRepository.delete(email);
      });
      default -> throw new IllegalArgumentException("Unsupported retention target: " + request.targetType());
    }
  }

  private void anonymizeTarget(RetentionRequest request) {
    switch (request.targetType().toUpperCase()) {
      case "NOTE" -> noteRepository.findById(UUID.fromString(request.targetId())).ifPresent(note -> {
        note.setNoteText("Anonymized due to retention workflow.");
        noteRepository.save(note);
      });
      case "EMAIL" -> emailRepository.findById(UUID.fromString(request.targetId())).ifPresent(email -> {
        email.setSubject("Anonymized email");
        email.setSender("redacted@retention.local");
        email.setRecipients("");
        email.setCc("");
        email.setBodyText("Anonymized due to retention workflow.");
        email.setBodyHtmlStoragePath(null);
        emailRepository.save(email);
      });
      case "DOCUMENT" -> documentRepository.findById(UUID.fromString(request.targetId())).ifPresent(document -> {
        document.setTitle("Anonymized document");
        documentRepository.save(document);
        metadataValueRepository.deleteByOwnerTypeAndOwnerId("DOCUMENT", document.getId());
        documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).ifPresent(version -> {
          version.setExtractedText("Anonymized due to retention workflow.");
          documentVersionRepository.save(version);
        });
      });
      default -> throw new IllegalArgumentException("Unsupported retention target: " + request.targetType());
    }
  }

  private void deleteVersionFiles(DocumentVersion version) {
    fileStorageService.delete(version.getOriginalStoragePath());
    if (version.getRedactedStoragePath() != null && !version.getRedactedStoragePath().isBlank()) {
      fileStorageService.delete(version.getRedactedStoragePath());
    }
  }

  private RetentionDecision auditAndReturn(RetentionRequest request, RetentionRecord record, boolean approved, String reason) {
    record.setLastAction(request.action().name());
    record.setLastOutcome(approved ? "APPROVED" : "DENIED");
    record.setLastReason(reason);
    record.setExecutedAt(approved ? Instant.now() : null);
    retentionRecordRepository.save(record);

    auditService.write(new AuditEvent(
        Instant.now(),
        "RETENTION",
        request.action().name(),
        approved ? AuditOutcome.SUCCESS : AuditOutcome.DENIED,
        request.requestedBy(),
        request.clientId(),
        request.targetType(),
        request.targetId(),
        false,
        Map.of("reason", reason)));

    return new RetentionDecision(approved, reason);
  }

  public record RetentionRequest(
      String targetType,
      String targetId,
      UUID clientId,
      UUID requestedBy,
      RetentionAction action,
      boolean legalHold,
      Instant minimumRetentionUntil,
      String reason,
      String holdType,
      String retentionPolicyKey,
      Instant reviewAt,
      Instant archivalEligibleAt,
      Instant disposalEligibleAt) {

    public RetentionRequest(
        String targetType,
        String targetId,
        UUID clientId,
        UUID requestedBy,
        RetentionAction action,
        boolean legalHold,
        Instant minimumRetentionUntil,
        String reason) {
      this(
          targetType,
          targetId,
          clientId,
          requestedBy,
          action,
          legalHold,
          minimumRetentionUntil,
          reason,
          null,
          null,
          null,
          null,
          null);
    }
  }

  public record RetentionDecision(boolean approved, String reason) {
  }

  public enum RetentionAction {
    APPLY_LEGAL_HOLD,
    RELEASE_LEGAL_HOLD,
    DELETE,
    ANONYMIZE
  }
}
