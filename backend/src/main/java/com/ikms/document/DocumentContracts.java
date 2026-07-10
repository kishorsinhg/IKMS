package com.ikms.document;

import jakarta.validation.constraints.NotBlank;
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
}
