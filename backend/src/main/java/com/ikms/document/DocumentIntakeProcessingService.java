package com.ikms.document;

import com.ikms.ai.ClassificationService;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.ai.PromptInjectionDetectionService;
import com.ikms.client.ClientService;
import com.ikms.config.domain.DocumentType;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.review.ReviewQueueReason;
import com.ikms.review.ReviewQueueService;
import com.ikms.review.ReviewRoutingService;
import com.ikms.worker.extract.TextExtractionService;
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
  private final ClassificationService classificationService;
  private final ClientService clientService;
  private final DocumentTypeRepository documentTypeRepository;
  private final ReviewRoutingService reviewRoutingService;
  private final ReviewQueueService reviewQueueService;
  private final PromptInjectionDetectionService promptInjectionDetectionService;
  private final EmbeddingIndexService embeddingIndexService;

  public DocumentIntakeProcessingService(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      TextExtractionService textExtractionService,
      ClassificationService classificationService,
      ClientService clientService,
      DocumentTypeRepository documentTypeRepository,
      ReviewRoutingService reviewRoutingService,
      ReviewQueueService reviewQueueService,
      PromptInjectionDetectionService promptInjectionDetectionService,
      EmbeddingIndexService embeddingIndexService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.textExtractionService = textExtractionService;
    this.classificationService = classificationService;
    this.clientService = clientService;
    this.documentTypeRepository = documentTypeRepository;
    this.reviewRoutingService = reviewRoutingService;
    this.reviewQueueService = reviewQueueService;
    this.promptInjectionDetectionService = promptInjectionDetectionService;
    this.embeddingIndexService = embeddingIndexService;
  }

  public void process(Document document, DocumentVersion version, UUID clientIdHint, byte[] fileBytes) {
    document.setProcessingStatus(DocumentProcessingStatus.EXTRACTING);
    documentRepository.save(document);

    var extraction = textExtractionService.extract(new TextExtractionService.ExtractionRequest(
        version.getFileName(),
        version.getMimeType(),
        fileBytes,
        "configured-provider"));
    version.setExtractedText(extraction.extractedText());
    version.setLanguage(extraction.language());
    version.setOcrProvider(extraction.provider());
    documentVersionRepository.save(version);

    document.setProcessingStatus(DocumentProcessingStatus.CLASSIFIED);
    var classification = classificationService.classify(new ClassificationService.ClassificationRequest(
        clientIdHint,
        version.getFileName(),
        version.getMimeType(),
        extraction.extractedText(),
        extraction.language()));
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
    documentRepository.save(document);

    if (promptInjectionDetectionService.inspect(extraction.extractedText()).detected()) {
      document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
      documentRepository.save(document);
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
    if (reviewDecision.requiresReview()) {
      document.setReviewStatus(DocumentReviewStatus.PENDING_REVIEW);
      documentRepository.save(document);
      reviewQueueService.createForDocument(document.getId(), reviewDecision.reason());
    }

    if (document.getClient() != null) {
      embeddingIndexService.indexDocumentVersion(document.getClient().getId(), version, extraction.segments());
      document.setProcessingStatus(DocumentProcessingStatus.INDEXED);
      documentRepository.save(document);
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
