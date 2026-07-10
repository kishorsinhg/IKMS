package com.ikms.document;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

  Optional<DocumentVersion> findByFileHash(String fileHash);

  Optional<DocumentVersion> findTopByDocument_IdOrderByVersionNumberDesc(UUID documentId);

  Optional<DocumentVersion> findByDocument_IdAndCurrentTrue(UUID documentId);
}
