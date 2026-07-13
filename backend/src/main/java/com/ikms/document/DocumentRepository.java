package com.ikms.document;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

  java.util.List<Document> findByClient_IdOrderByCreatedAtDesc(UUID clientId);

  long countByParentEmail_Id(UUID parentEmailId);
}
