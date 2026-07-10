package com.ikms.document;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetectionService {

  private final DocumentVersionRepository documentVersionRepository;

  public DuplicateDetectionService(DocumentVersionRepository documentVersionRepository) {
    this.documentVersionRepository = documentVersionRepository;
  }

  public Optional<DuplicateMatch> findExactDuplicate(String fileHash) {
    return documentVersionRepository.findByFileHash(fileHash)
        .map(version -> new DuplicateMatch(
            version.getDocument().getId(),
            version.getId(),
            version.getFileHash(),
            version.getOriginalStoragePath()));
  }

  public record DuplicateMatch(
      UUID documentId,
      UUID versionId,
      String fileHash,
      String originalStoragePath) {
  }
}
