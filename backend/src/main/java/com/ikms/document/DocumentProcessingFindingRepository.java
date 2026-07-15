package com.ikms.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentProcessingFindingRepository extends JpaRepository<DocumentProcessingFinding, UUID> {

  List<DocumentProcessingFinding> findByJob_IdOrderByCreatedAtAsc(UUID jobId);
}
