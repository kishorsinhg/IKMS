package com.ikms.worker.intake;

import com.ikms.ai.ClassificationService;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentUploadService;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.review.ReviewQueueReason;
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
  private final TextExtractionService textExtractionService;
  private final ClassificationService classificationService;
  private final ReviewQueueService reviewQueueService;

  public SharedFolderIntakeWorker(
      DocumentUploadService documentUploadService,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      TextExtractionService textExtractionService,
      ClassificationService classificationService,
      ReviewQueueService reviewQueueService) {
    this.documentUploadService = documentUploadService;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.textExtractionService = textExtractionService;
    this.classificationService = classificationService;
    this.reviewQueueService = reviewQueueService;
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

    var extraction = textExtractionService.extract(new TextExtractionService.ExtractionRequest(
        file.filename(),
        file.mimeType(),
        file.fileBytes(),
        "shared-folder-placeholder"));
    version.setExtractedText(extraction.extractedText());
    version.setLanguage(extraction.language());
    version.setOcrProvider(extraction.provider());
    documentVersionRepository.save(version);

    var classification = classificationService.classify(new ClassificationService.ClassificationRequest(
        file.clientId(),
        file.filename(),
        file.mimeType(),
        extraction.extractedText(),
        extraction.language()));
    document.setTitle(classification.suggestedTitle());
    document.setClientMatchConfidence(classification.clientMatchConfidence());
    document.setClassificationConfidence(classification.classificationConfidence());
    document.setExtractionConfidence(classification.extractionConfidence());
    documentRepository.save(document);

    if (file.clientId() == null || classification.clientMatchConfidence().doubleValue() < 0.5d) {
      reviewQueueService.createForDocument(document.getId(), ReviewQueueReason.UNLINKED);
    }

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
