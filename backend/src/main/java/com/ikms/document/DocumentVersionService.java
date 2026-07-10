package com.ikms.document;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentVersionService {

  private final DocumentVersionRepository documentVersionRepository;

  public DocumentVersionService(DocumentVersionRepository documentVersionRepository) {
    this.documentVersionRepository = documentVersionRepository;
  }

  public DocumentVersion createInitialVersion(
      Document document,
      String fileHash,
      String fileName,
      String mimeType,
      long fileSizeBytes,
      String originalStoragePath,
      UUID actorUserId) {
    return createNextVersion(document, fileHash, fileName, mimeType, fileSizeBytes, originalStoragePath, actorUserId);
  }

  public DocumentVersion createNextVersion(
      Document document,
      String fileHash,
      String fileName,
      String mimeType,
      long fileSizeBytes,
      String originalStoragePath,
      UUID actorUserId) {
    documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).ifPresent(existing -> {
      existing.setCurrent(false);
      documentVersionRepository.save(existing);
    });

    int nextVersionNumber = documentVersionRepository.findTopByDocument_IdOrderByVersionNumberDesc(document.getId())
        .map(existing -> existing.getVersionNumber() + 1)
        .orElse(1);

    DocumentVersion version = new DocumentVersion();
    version.setDocument(document);
    version.setVersionNumber(nextVersionNumber);
    version.setFileHash(fileHash);
    version.setFileName(fileName);
    version.setMimeType(mimeType);
    version.setFileSizeBytes(fileSizeBytes);
    version.setOriginalStoragePath(originalStoragePath);
    version.setRedactionStatus(RedactionStatus.PENDING);
    version.setCurrent(true);
    version.setCreatedBy(actorUserId);
    return documentVersionRepository.save(version);
  }
}
