package com.ikms.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbeddingChunkRepository extends JpaRepository<EmbeddingChunk, UUID> {

  List<EmbeddingChunk> findByClientIdOrderByCreatedAtDesc(UUID clientId);

  void deleteBySourceTypeAndSourceId(String sourceType, UUID sourceId);

  List<EmbeddingChunk> findBySourceTypeAndSourceIdOrderByChunkIndexAsc(String sourceType, UUID sourceId);
}
