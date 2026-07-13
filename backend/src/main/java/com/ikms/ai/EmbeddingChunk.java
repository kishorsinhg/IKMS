package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "embedding_chunk")
public class EmbeddingChunk {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "source_type", nullable = false, length = 32)
  private String sourceType;

  @Column(name = "source_id", nullable = false)
  private UUID sourceId;

  @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
  private String chunkText;

  @Column(name = "embedding_reference", length = 255)
  private String embeddingReference;

  @Column(name = "embedding_vector", columnDefinition = "TEXT")
  private String embeddingVector;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
  public String getChunkText() { return chunkText; }
  public void setChunkText(String chunkText) { this.chunkText = chunkText; }
  public String getEmbeddingReference() { return embeddingReference; }
  public void setEmbeddingReference(String embeddingReference) { this.embeddingReference = embeddingReference; }
  public String getEmbeddingVector() { return embeddingVector; }
  public void setEmbeddingVector(String embeddingVector) { this.embeddingVector = embeddingVector; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
