package com.ikms.document;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class DocumentController {

  private static final List<String> SUPPORTED_MIME_TYPES = List.of(
      MediaType.APPLICATION_PDF_VALUE,
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final com.ikms.client.ClientKnowledgeService clientKnowledgeService;
  private final DocumentUploadService documentUploadService;
  private final DocumentIntakeProcessingService documentIntakeProcessingService;
  private final com.ikms.security.ContentSensitivityService contentSensitivityService;
  private final com.ikms.storage.FileStorageService fileStorageService;

  public DocumentController(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      com.ikms.client.ClientKnowledgeService clientKnowledgeService,
      DocumentUploadService documentUploadService,
      DocumentIntakeProcessingService documentIntakeProcessingService,
      com.ikms.security.ContentSensitivityService contentSensitivityService,
      com.ikms.storage.FileStorageService fileStorageService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.clientKnowledgeService = clientKnowledgeService;
    this.documentUploadService = documentUploadService;
    this.documentIntakeProcessingService = documentIntakeProcessingService;
    this.contentSensitivityService = contentSensitivityService;
    this.fileStorageService = fileStorageService;
  }

  @PostMapping(path = "/api/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentContracts.UploadDocumentResponse upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "clientId", required = false) UUID clientId) throws IOException {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is required.");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Filename is required.");
    }

    String mimeType = normalizeMimeType(file.getContentType(), filename);
    validateSupportedFile(filename, mimeType);

    byte[] fileBytes = file.getBytes();
    var result = documentUploadService.upload(new DocumentUploadService.UploadCommand(
        clientId,
        null,
        filename,
        mimeType,
        sha256(fileBytes),
        fileBytes));

    if (result.documentId() != null && result.versionId() != null) {
      Document document = documentRepository.findById(result.documentId())
          .orElseThrow(() -> new IllegalStateException("Uploaded document not found: " + result.documentId()));
      DocumentVersion version = documentVersionRepository.findById(result.versionId())
          .orElseThrow(() -> new IllegalStateException("Uploaded version not found: " + result.versionId()));
      documentIntakeProcessingService.process(document, version, clientId, fileBytes);
    }

    return new DocumentContracts.UploadDocumentResponse(
        result.documentId(),
        result.versionId(),
        result.outcome().name(),
        result.reviewStatus().name(),
        result.duplicateOfDocumentId() == null ? null : result.duplicateOfDocumentId().toString());
  }

  @GetMapping("/api/clients/{clientId}/documents")
  public List<DocumentContracts.DocumentSummaryResponse> listClientDocuments(@PathVariable UUID clientId) {
    return documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId).stream()
        .map(document -> {
          DocumentVersion currentVersion = documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())
              .orElse(null);
          boolean containsPii = contentSensitivityService.documentContainsPii(document.getId());
          return new DocumentContracts.DocumentSummaryResponse(
              document.getId(),
              document.getClient() == null ? null : document.getClient().getId(),
              document.getTitle(),
              document.getSource().name(),
              document.getProcessingStatus().name(),
              document.getReviewStatus().name(),
              currentVersion == null ? RedactionStatus.PENDING.name() : currentVersion.getRedactionStatus().name(),
              containsPii,
              document.getCurrentVersionId() == null ? null : document.getCurrentVersionId().toString(),
              document.getParentEmail() == null ? null : document.getParentEmail().getId().toString(),
              document.getCreatedAt());
        })
        .toList();
  }

  @GetMapping("/api/documents/{documentId}/versions")
  public List<com.ikms.client.ClientContracts.DocumentVersionSummaryResponse> listDocumentVersions(
      @PathVariable UUID documentId) {
    return clientKnowledgeService.listDocumentVersions(documentId);
  }

  @PostMapping("/api/documents/process")
  public DocumentContracts.UploadDocumentResponse reprocessDocument(
      @RequestParam("documentId") UUID documentId,
      @RequestParam(name = "clientId", required = false) UUID clientId) throws IOException {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Current document version not found: " + documentId));
    byte[] fileBytes = fileStorageService.load(version.getOriginalStoragePath()).getInputStream().readAllBytes();
    documentIntakeProcessingService.process(
        document,
        version,
        clientId == null && document.getClient() != null ? document.getClient().getId() : clientId,
        fileBytes);
    return new DocumentContracts.UploadDocumentResponse(
        document.getId(),
        version.getId(),
        DocumentUploadService.UploadOutcome.CREATED.name(),
        document.getReviewStatus().name(),
        null);
  }

  private static void validateSupportedFile(String filename, String mimeType) {
    String lowercaseName = filename.toLowerCase();
    boolean supportedExtension = lowercaseName.endsWith(".pdf") || lowercaseName.endsWith(".docx");
    if (!supportedExtension || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
      throw new IllegalArgumentException("Only PDF and DOCX uploads are supported.");
    }
  }

  private static String normalizeMimeType(String contentType, String filename) {
    if (contentType != null && SUPPORTED_MIME_TYPES.contains(contentType)) {
      return contentType;
    }
    String lowercaseName = filename.toLowerCase();
    if (lowercaseName.endsWith(".pdf")) {
      return MediaType.APPLICATION_PDF_VALUE;
    }
    if (lowercaseName.endsWith(".docx")) {
      return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
    return contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
