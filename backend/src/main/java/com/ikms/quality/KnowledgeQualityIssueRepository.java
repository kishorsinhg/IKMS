package com.ikms.quality;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeQualityIssueRepository extends JpaRepository<KnowledgeQualityIssue, UUID> {

  List<KnowledgeQualityIssue> findBySnapshot_IdOrderBySeverityDescCreatedAtAsc(UUID snapshotId);

  List<KnowledgeQualityIssue> findByStatusOrderBySeverityDescCreatedAtAsc(KnowledgeQualityIssueStatus status);

  List<KnowledgeQualityIssue> findByClientIdAndStatusOrderBySeverityDescCreatedAtAsc(UUID clientId, KnowledgeQualityIssueStatus status);

  void deleteBySnapshot_Id(UUID snapshotId);
}
