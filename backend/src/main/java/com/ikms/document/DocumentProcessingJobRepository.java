package com.ikms.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentProcessingJobRepository extends JpaRepository<DocumentProcessingJob, UUID> {

  Optional<DocumentProcessingJob> findTopByDocument_IdOrderByCreatedAtDesc(UUID documentId);

  Optional<DocumentProcessingJob> findTopByDocumentVersion_IdOrderByCreatedAtDesc(UUID documentVersionId);

  List<DocumentProcessingJob> findByStatusOrderByCreatedAtDesc(DocumentProcessingJobStatus status);

  List<DocumentProcessingJob> findTop50ByOrderByCreatedAtDesc();
}
