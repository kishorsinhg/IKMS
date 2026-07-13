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
  private final DocumentUploadService documentUploadService;
  private final DocumentIntakeProcessingService documentIntakeProcessingService;
  private final com.ikms.security.ContentSensitivityService contentSensitivityService;

  public DocumentController(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentUploadService documentUploadService,
      DocumentIntakeProcessingService documentIntakeProcessingService,
      com.ikms.security.ContentSensitivityService contentSensitivityService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentUploadService = documentUploadService;
    this.documentIntakeProcessingService = documentIntakeProcessingService;
    this.contentSensitivityService = contentSensitivityService;
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
