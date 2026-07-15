package com.ikms.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentProcessingFieldRepository extends JpaRepository<DocumentProcessingField, UUID> {

  List<DocumentProcessingField> findByJob_IdOrderByFieldKeyAsc(UUID jobId);

  Optional<DocumentProcessingField> findByJob_IdAndFieldKeyIgnoreCase(UUID jobId, String fieldKey);
}
