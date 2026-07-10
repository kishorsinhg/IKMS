package com.ikms.document;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class DocumentContracts {

  private DocumentContracts() {
  }

  public record UploadDocumentRequest(
      UUID clientId,
      @NotBlank(message = "Filename is required.") String filename,
      @NotBlank(message = "Mime type is required.") String mimeType,
      @NotBlank(message = "File hash is required.") String fileHash) {
  }

  public record UploadDocumentResponse(
      UUID documentId,
      UUID versionId,
      String outcome,
      String reviewStatus,
      String duplicateOfDocumentId) {
  }

  public record DocumentSummaryResponse(
      UUID id,
      UUID clientId,
      String title,
      String source,
      String processingStatus,
      String reviewStatus,
      String currentVersionId,
      String parentEmailId,
      Instant createdAt) {
  }
}
