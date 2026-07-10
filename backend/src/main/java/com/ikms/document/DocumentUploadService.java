package com.ikms.document;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.client.Client;
import com.ikms.client.ClientService;
import com.ikms.storage.FileStorageService;
import com.ikms.storage.FileStorageService.FileVariant;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentUploadService {

  private final DocumentRepository documentRepository;
  private final DuplicateDetectionService duplicateDetectionService;
  private final DocumentVersionService documentVersionService;
  private final ClientService clientService;
  private final FileStorageService fileStorageService;
  private final AuditService auditService;

  public DocumentUploadService(
      DocumentRepository documentRepository,
      DuplicateDetectionService duplicateDetectionService,
      DocumentVersionService documentVersionService,
      ClientService clientService,
      FileStorageService fileStorageService,
      AuditService auditService) {
    this.documentRepository = documentRepository;
    this.duplicateDetectionService = duplicateDetectionService;
    this.documentVersionService = documentVersionService;
    this.clientService = clientService;
    this.fileStorageService = fileStorageService;
    this.auditService = auditService;
  }

  public UploadResult upload(UploadCommand command) {
    var duplicate = duplicateDetectionService.findExactDuplicate(command.fileHash());
    if (duplicate.isPresent()) {
      var match = duplicate.get();
      auditService.write(new AuditEvent(
          Instant.now(),
          "DOCUMENT",
          "DOCUMENT_UPLOAD_DUPLICATE",
          AuditOutcome.DENIED,
          command.actorUserId(),
          command.clientId(),
          "Document",
          match.documentId().toString(),
          false,
          Map.of("fileHash", command.fileHash())));
      return UploadResult.duplicate(match.documentId(), match.versionId());
    }

    Client client = command.clientId() == null ? null : clientService.requireClient(command.clientId());

    Document document = new Document();
    document.setClient(client);
    document.setTitle(command.filename());
    document.setSource(DocumentSource.MANUAL_UPLOAD);
    document.setProcessingStatus(DocumentProcessingStatus.INTAKE_RECEIVED);
    document.setReviewStatus(client == null ? DocumentReviewStatus.PENDING_REVIEW : DocumentReviewStatus.NOT_REQUIRED);
    Document savedDocument = documentRepository.save(document);

    var storedFile = fileStorageService.store(new FileStorageService.StoreRequest(
        command.filename(),
        command.mimeType(),
        FileVariant.ORIGINAL,
        command.fileBytes().length,
        new ByteArrayInputStream(command.fileBytes())));

    DocumentVersion version = documentVersionService.createInitialVersion(
        savedDocument,
        command.fileHash(),
        command.filename(),
        command.mimeType(),
        command.fileBytes().length,
        storedFile.storageKey(),
        command.actorUserId());

    savedDocument.setCurrentVersionId(version.getId());
    savedDocument.setProcessingStatus(DocumentProcessingStatus.CLASSIFIED);
    documentRepository.save(savedDocument);

    auditService.write(new AuditEvent(
        Instant.now(),
        "DOCUMENT",
        "DOCUMENT_UPLOADED",
        AuditOutcome.SUCCESS,
        command.actorUserId(),
        command.clientId(),
        "Document",
        savedDocument.getId().toString(),
        false,
        Map.of(
            "fileHash", command.fileHash(),
            "reviewStatus", savedDocument.getReviewStatus().name())));

    return UploadResult.created(savedDocument.getId(), version.getId(), savedDocument.getReviewStatus());
  }

  public record UploadCommand(
      UUID clientId,
      UUID actorUserId,
      String filename,
      String mimeType,
      String fileHash,
      byte[] fileBytes) {
  }

  public record UploadResult(
      UploadOutcome outcome,
      UUID documentId,
      UUID versionId,
      UUID duplicateOfDocumentId,
      DocumentReviewStatus reviewStatus) {

    static UploadResult duplicate(UUID documentId, UUID versionId) {
      return new UploadResult(UploadOutcome.DUPLICATE, null, null, documentId, DocumentReviewStatus.PENDING_REVIEW);
    }

    static UploadResult created(UUID documentId, UUID versionId, DocumentReviewStatus reviewStatus) {
      return new UploadResult(UploadOutcome.CREATED, documentId, versionId, null, reviewStatus);
    }
  }

  public enum UploadOutcome {
    CREATED,
    DUPLICATE
  }
}
