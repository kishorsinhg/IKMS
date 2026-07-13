package com.ikms.worker.intake;

import com.ikms.ai.ClassificationService;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.ai.PromptInjectionDetectionService;
import com.ikms.client.ClientService;
import com.ikms.document.DocumentIntakeProcessingService;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentUploadService;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.document.DocumentReviewStatus;
import com.ikms.review.ReviewQueueReason;
import com.ikms.review.ReviewRoutingService;
import com.ikms.review.ReviewQueueService;
import com.ikms.worker.extract.TextExtractionService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SharedFolderIntakeWorker {

  private final DocumentUploadService documentUploadService;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentIntakeProcessingService documentIntakeProcessingService;

  public SharedFolderIntakeWorker(
      DocumentUploadService documentUploadService,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentIntakeProcessingService documentIntakeProcessingService) {
    this.documentUploadService = documentUploadService;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentIntakeProcessingService = documentIntakeProcessingService;
  }

  public DocumentUploadService.UploadResult processFile(IntakeFile file) {
    DocumentUploadService.UploadResult uploadResult = documentUploadService.upload(new DocumentUploadService.UploadCommand(
        file.clientId(),
        file.actorUserId(),
        file.filename(),
        file.mimeType(),
        file.fileHash(),
        file.fileBytes()));

    if (uploadResult.documentId() == null) {
      return uploadResult;
    }

    Document document = documentRepository.findById(uploadResult.documentId())
        .orElseThrow(() -> new IllegalStateException("Uploaded document not found: " + uploadResult.documentId()));
    DocumentVersion version = documentVersionRepository.findById(uploadResult.versionId())
        .orElseThrow(() -> new IllegalStateException("Uploaded version not found: " + uploadResult.versionId()));

    documentIntakeProcessingService.process(document, version, file.clientId(), file.fileBytes());

    return uploadResult;
  }

  public record IntakeFile(
      UUID clientId,
      UUID actorUserId,
      String filename,
      String mimeType,
      String fileHash,
      byte[] fileBytes) {
  }
}
