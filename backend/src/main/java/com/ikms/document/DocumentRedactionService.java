package com.ikms.document;

import com.ikms.storage.FileStorageService;
import com.ikms.storage.FileStorageService.FileVariant;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentRedactionService {

  private final DocumentVersionRepository documentVersionRepository;
  private final FileStorageService fileStorageService;

  public DocumentRedactionService(
      DocumentVersionRepository documentVersionRepository,
      FileStorageService fileStorageService) {
    this.documentVersionRepository = documentVersionRepository;
    this.fileStorageService = fileStorageService;
  }

  public DocumentVersion ensureRedactedVariant(DocumentVersion version, boolean containsPii) {
    if (!containsPii) {
      version.setRedactionStatus(RedactionStatus.NOT_NEEDED);
      return documentVersionRepository.save(version);
    }
    if (version.getRedactionStatus() == RedactionStatus.AVAILABLE
        && version.getRedactedStoragePath() != null
        && !version.getRedactedStoragePath().isBlank()) {
      return version;
    }

    try {
      String sanitizedName = version.getFileName().replaceAll("\\.[^.]+$", "") + "-redacted.txt";
      String payload = "REDACTED COPY\nOriginal file: " + version.getFileName() + "\n";
      if (version.getExtractedText() != null && !version.getExtractedText().isBlank()) {
        payload += version.getExtractedText().replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+", "[REDACTED]");
      }
      var stored = fileStorageService.store(new FileStorageService.StoreRequest(
          sanitizedName,
          "text/plain",
          FileVariant.REDACTED,
          payload.getBytes(StandardCharsets.UTF_8).length,
          new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8))));
      version.setRedactedStoragePath(stored.storageKey());
      version.setRedactionStatus(RedactionStatus.AVAILABLE);
      return documentVersionRepository.save(version);
    } catch (RuntimeException exception) {
      version.setRedactionStatus(RedactionStatus.FAILED);
      documentVersionRepository.save(version);
      throw exception;
    }
  }
}
