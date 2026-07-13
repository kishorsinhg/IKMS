package com.ikms.retention;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionRecordRepository extends JpaRepository<RetentionRecord, UUID> {

  Optional<RetentionRecord> findByTargetTypeAndTargetId(String targetType, String targetId);
}
