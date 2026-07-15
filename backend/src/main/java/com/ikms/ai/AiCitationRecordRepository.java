package com.ikms.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCitationRecordRepository extends JpaRepository<AiCitationRecord, UUID> {

  java.util.List<AiCitationRecord> findByInteractionIdOrderByCreatedAtAsc(UUID interactionId);
}
