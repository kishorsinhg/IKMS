package com.ikms.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikms.ai.ClassificationService;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.ai.PromptInjectionDetectionService;
import com.ikms.client.Client;
import com.ikms.client.ClientService;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.review.ReviewQueueReason;
import com.ikms.review.ReviewQueueService;
import com.ikms.review.ReviewRoutingService;
import com.ikms.worker.extract.TextExtractionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentIntakeProcessingServiceTest {

  private DocumentRepository documentRepository;
  private DocumentVersionRepository documentVersionRepository;
  private TextExtractionService textExtractionService;
  private ClassificationService classificationService;
  private ClientService clientService;
  private DocumentTypeRepository documentTypeRepository;
  private ReviewRoutingService reviewRoutingService;
  private ReviewQueueService reviewQueueService;
  private PromptInjectionDetectionService promptInjectionDetectionService;
  private EmbeddingIndexService embeddingIndexService;
  private DocumentIntakeProcessingService service;

  @BeforeEach
  void setUp() {
    documentRepository = mock(DocumentRepository.class);
    documentVersionRepository = mock(DocumentVersionRepository.class);
    textExtractionService = mock(TextExtractionService.class);
    classificationService = mock(ClassificationService.class);
    clientService = mock(ClientService.class);
    documentTypeRepository = mock(DocumentTypeRepository.class);
    reviewRoutingService = mock(ReviewRoutingService.class);
    reviewQueueService = mock(ReviewQueueService.class);
    promptInjectionDetectionService = mock(PromptInjectionDetectionService.class);
    embeddingIndexService = mock(EmbeddingIndexService.class);

    when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(documentVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(documentTypeRepository.findAll()).thenReturn(List.of());
    when(promptInjectionDetectionService.inspect(any())).thenReturn(new PromptInjectionDetectionService.DetectionResult(false, null));

    service = new DocumentIntakeProcessingService(
        documentRepository,
        documentVersionRepository,
        textExtractionService,
        classificationService,
        clientService,
        documentTypeRepository,
        reviewRoutingService,
        reviewQueueService,
        promptInjectionDetectionService,
        embeddingIndexService);
  }

  @Test
  void processUsesExtractionConfidenceFromOcrForReviewRouting() {
    UUID clientId = UUID.randomUUID();
    Document document = new Document();
    document.setId(UUID.randomUUID());
    document.setTitle("Incoming");
    Client client = new Client();
    client.setId(clientId);
    document.setClient(client);

    DocumentVersion version = new DocumentVersion();
    version.setId(UUID.randomUUID());
    version.setDocument(document);
    version.setFileName("scanned.pdf");
    version.setMimeType("application/pdf");

    when(textExtractionService.extract(any())).thenReturn(new TextExtractionService.ExtractionResult(
        "OCR extracted text",
        "en",
        "mistral-ocr-latest",
        List.of(new TextExtractionService.PageSegment(1, "OCR extracted text")),
        new BigDecimal("0.4000")));
    when(classificationService.classify(any())).thenReturn(new ClassificationService.ClassificationResult(
        clientId,
        "Scanned PDF",
        new BigDecimal("0.9800"),
        new BigDecimal("0.9200"),
        new BigDecimal("0.9100"),
        Map.of("documentType", "PDF Document", "language", "en")));
    when(reviewRoutingService.documentDecision(
        eq(clientId),
        eq(document.getId()),
        eq(new BigDecimal("0.9800")),
        eq(new BigDecimal("0.9200")),
        eq(new BigDecimal("0.4000"))))
        .thenReturn(new ReviewRoutingService.ReviewDecision(true, ReviewQueueReason.LOW_EXTRACTION_CONFIDENCE));

    service.process(document, version, clientId, "pdf".getBytes());

    assertThat(document.getExtractionConfidence()).isEqualByComparingTo(new BigDecimal("0.4000"));
    assertThat(document.getReviewStatus()).isEqualTo(DocumentReviewStatus.PENDING_REVIEW);
    verify(reviewQueueService).createForDocument(document.getId(), ReviewQueueReason.LOW_EXTRACTION_CONFIDENCE);
    verify(embeddingIndexService).indexDocumentVersion(eq(clientId), eq(version), any());
    verify(clientService, never()).requireClient(any());
  }
}
