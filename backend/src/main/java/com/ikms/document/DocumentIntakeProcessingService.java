package com.ikms.document;

import com.ikms.ai.ClassificationService;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.ai.PromptInjectionDetectionService;
import com.ikms.client.ClientService;
import com.ikms.config.domain.DocumentType;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.observability.RequestContextHolder;
import com.ikms.review.ReviewQueueReason;
import com.ikms.review.ReviewQueueService;
import com.ikms.review.ReviewRoutingService;
import com.ikms.worker.extract.TextExtractionService;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentIntakeProcessingService {

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final TextExtractionService textExtractionService;
  private final VirusScanService virusScanService;
  private final LanguageDetectionService languageDetectionService;
  private final ClassificationService classificationService;
  private final DocumentMetadataExtractionService documentMetadataExtractionService;
  private final DocumentValidationService documentValidationService;
  private final DocumentConfidenceService documentConfidenceService;
  private final DocumentProcessingJobService documentProcessingJobService;
  private final ClientService clientService;
  private final DocumentTypeRepository documentTypeRepository;
  private final ReviewRoutingService reviewRoutingService;
  private final ReviewQueueService reviewQueueService;
  private final PromptInjectionDetectionService promptInjectionDetectionService;
  private final EmbeddingIndexService embeddingIndexService;
  private final DocumentPublishingService documentPublishingService;

  public DocumentIntakeProcessingService(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      TextExtractionService textExtractionService,
      VirusScanService virusScanService,
      LanguageDetectionService languageDetectionService,
      ClassificationService classificationService,
      DocumentMetadataExtractionService documentMetadataExtractionService,
      DocumentValidationService documentValidationService,
      DocumentConfidenceService documentConfidenceService,
      DocumentProcessingJobService documentProcessingJobService,
      ClientService clientService,
      DocumentTypeRepository documentTypeRepository,
      ReviewRoutingService reviewRoutingService,
      ReviewQueueService reviewQueueService,
      PromptInjectionDetectionService promptInjectionDetectionService,
      EmbeddingIndexService embeddingIndexService,
      DocumentPublishingService documentPublishingService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.textExtractionService = textExtractionService;
    this.virusScanService = virusScanService;
    this.languageDetectionService = languageDetectionService;
    this.classificationService = classificationService;
    this.documentMetadataExtractionService = documentMetadataExtractionService;
    this.documentValidationService = documentValidationService;
    this.documentConfidenceService = documentConfidenceService;
    this.documentProcessingJobService = documentProcessingJobService;
    this.clientService = clientService;
    this.documentTypeRepository = documentTypeRepository;
    this.reviewRoutingService = reviewRoutingService;
    this.reviewQueueService = reviewQueueService;
    this.promptInjectionDetectionService = promptInjectionDetectionService;
    this.embeddingIndexService = embeddingIndexService;
    this.documentPublishingService = documentPublishingService;
  }

  public void process(Document document, DocumentVersion version, UUID clientIdHint, byte[] fileBytes) {
    DocumentProcessingJob job = documentProcessingJobService.create(document, version, clientIdHint);
    String processingJobId = job.getId() == null ? UUID.randomUUID().toString() : job.getId().toString();
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.PROCESSING_JOB_ID, processingJobId)) {
      documentProcessingJobService.markRunning(job, DocumentProcessingStage.VIRUS_SCAN);
      var virusScan = virusScanService.scan(version.getFileName(), version.getMimeType(), fileBytes);
      if (!virusScan.clean()) {
        document.setProcessingStatus(DocumentProcessingStatus.FAILED);
        document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
        documentRepository.save(document);
        documentProcessingJobService.markFailed(job, "virus_scan_failed", "Virus scan flagged the document for manual handling.");
        reviewQueueService.createForDocument(document.getId(), ReviewQueueReason.PROCESSING_FAILED);
        return;
      }

      document.setProcessingStatus(DocumentProcessingStatus.VIRUS_SCANNED);
      documentRepository.save(document);
      documentProcessingJobService.advance(job, DocumentProcessingStage.OCR_TEXT_EXTRACTION);

      var extraction = textExtractionService.extract(new TextExtractionService.ExtractionRequest(
          version.getFileName(),
          version.getMimeType(),
          fileBytes,
          "configured-provider"));
      String detectedLanguage = languageDetectionService.detect(extraction.extractedText(), extraction.language());
      version.setExtractedText(extraction.extractedText());
      version.setLanguage(detectedLanguage);
      version.setOcrProvider(extraction.provider());
      documentVersionRepository.save(version);

      document.setProcessingStatus(DocumentProcessingStatus.OCR_COMPLETE);
      documentRepository.save(document);
      documentProcessingJobService.advance(job, DocumentProcessingStage.DOCUMENT_CLASSIFICATION);

      var classification = classificationService.classify(new ClassificationService.ClassificationRequest(
          clientIdHint,
          version.getFileName(),
          version.getMimeType(),
          extraction.extractedText(),
          detectedLanguage));
      document.setTitle(classification.suggestedTitle());
      if (document.getClient() == null && classification.suggestedClientId() != null) {
        document.setClient(clientService.requireClient(classification.suggestedClientId()));
      }
      document.setDocumentTypeId(resolveDocumentTypeId(classification.metadata().get("documentType")));
      document.setClientMatchConfidence(classification.clientMatchConfidence());
      document.setClassificationConfidence(classification.classificationConfidence());
      document.setExtractionConfidence(
          extraction.extractionConfidence() != null
              ? extraction.extractionConfidence()
              : classification.extractionConfidence());
      document.setProcessingStatus(DocumentProcessingStatus.CLASSIFIED);
      documentRepository.save(document);

      documentProcessingJobService.advance(job, DocumentProcessingStage.METADATA_EXTRACTION);
      var metadataBundle = documentMetadataExtractionService.extract(document, version, extraction, classification, detectedLanguage);
      documentProcessingJobService.replaceFields(job, metadataBundle.fields());

      BigDecimal duplicateConfidence = BigDecimal.ZERO;
      documentProcessingJobService.advance(job, DocumentProcessingStage.VALIDATION);
      var validation = documentValidationService.validate(
          document.getClient() == null ? clientIdHint : document.getClient().getId(),
          metadataBundle.fields(),
          extraction.extractionConfidence(),
          classification.classificationConfidence(),
          duplicateConfidence);
      documentProcessingJobService.replaceFindings(job, validation.findings());

      BigDecimal overallConfidence = documentConfidenceService.overallConfidence(
          extraction.extractionConfidence(),
          classification.classificationConfidence(),
          metadataBundle.metadataConfidence(),
          metadataBundle.businessReferenceConfidence(),
          validation.validationConfidence(),
          duplicateConfidence);
      documentProcessingJobService.updateConfidences(
          job,
          detectedLanguage,
          extraction.provider(),
          classification.metadata().get("provider"),
          extraction.extractionConfidence(),
          classification.classificationConfidence(),
          metadataBundle.metadataConfidence(),
          metadataBundle.businessReferenceConfidence(),
          validation.validationConfidence(),
          duplicateConfidence,
          overallConfidence);

      if (promptInjectionDetectionService.inspect(extraction.extractedText()).detected()) {
        document.setProcessingStatus(DocumentProcessingStatus.WAITING_REVIEW);
        document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
        documentRepository.save(document);
        documentProcessingJobService.markWaitingReview(job);
        reviewQueueService.createForDocument(document.getId(), ReviewQueueReason.PROMPT_INJECTION_RISK);
        return;
      }

      UUID effectiveClientId = document.getClient() == null ? clientIdHint : document.getClient().getId();
      var reviewDecision = reviewRoutingService.documentDecision(
          effectiveClientId,
          document.getId(),
          classification.clientMatchConfidence(),
          classification.classificationConfidence(),
          document.getExtractionConfidence());
      if (validation.requiresReview() || reviewDecision.requiresReview()) {
        document.setProcessingStatus(DocumentProcessingStatus.WAITING_REVIEW);
        document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
        documentRepository.save(document);
        documentProcessingJobService.markWaitingReview(job);
        reviewQueueService.createForDocument(document.getId(), validation.requiresReview() ? ReviewQueueReason.LOW_EXTRACTION_CONFIDENCE : reviewDecision.reason());
        return;
      }

      document.setReviewStatus(DocumentReviewStatus.APPROVED);
      document.setProcessingStatus(DocumentProcessingStatus.APPROVED);
      documentRepository.save(document);
      documentProcessingJobService.markApproved(job, "Auto-approved by processing pipeline");
      documentProcessingJobService.applyApprovalValues(job);

      if (document.getClient() != null) {
        embeddingIndexService.indexDocumentVersion(document.getClient().getId(), version, extraction.segments());
        documentPublishingService.publish(document);
        document.setProcessingStatus(DocumentProcessingStatus.INDEXED);
        documentRepository.save(document);
      }
      documentProcessingJobService.markPublished(job);
    } catch (RuntimeException exception) {
      document.setProcessingStatus(DocumentProcessingStatus.FAILED);
      document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
      documentRepository.save(document);
      documentProcessingJobService.markFailed(job, "processing_failed", exception.getMessage());
      reviewQueueService.createForDocument(document.getId(), ReviewQueueReason.PROCESSING_FAILED);
      throw exception;
    }
  }

  private UUID resolveDocumentTypeId(String documentTypeName) {
    if (documentTypeName == null || documentTypeName.isBlank()) {
      return null;
    }
    return documentTypeRepository.findAll().stream()
        .filter(item -> item.getName() != null && item.getName().trim().equalsIgnoreCase(documentTypeName.trim()))
        .map(DocumentType::getId)
        .findFirst()
        .orElseGet(() -> documentTypeRepository.findAll().stream()
            .filter(item -> item.getName() != null
                && item.getName().toLowerCase(Locale.ROOT).contains(documentTypeName.trim().toLowerCase(Locale.ROOT)))
            .map(DocumentType::getId)
            .findFirst()
            .orElse(null));
  }
}
