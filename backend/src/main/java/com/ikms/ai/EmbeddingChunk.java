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

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(name = "token_count")
  private Integer tokenCount;

  @Column(name = "source_title", length = 255)
  private String sourceTitle;

  @Column(name = "source_section", length = 255)
  private String sourceSection;

  @Column(name = "page_number")
  private Integer pageNumber;

  @Column(name = "metadata_summary", columnDefinition = "TEXT")
  private String metadataSummary;

  @Column(length = 32)
  private String language;

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
  public int getChunkIndex() { return chunkIndex; }
  public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
  public Integer getTokenCount() { return tokenCount; }
  public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
  public String getSourceTitle() { return sourceTitle; }
  public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
  public String getSourceSection() { return sourceSection; }
  public void setSourceSection(String sourceSection) { this.sourceSection = sourceSection; }
  public Integer getPageNumber() { return pageNumber; }
  public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
  public String getMetadataSummary() { return metadataSummary; }
  public void setMetadataSummary(String metadataSummary) { this.metadataSummary = metadataSummary; }
  public String getLanguage() { return language; }
  public void setLanguage(String language) { this.language = language; }
  public String getEmbeddingReference() { return embeddingReference; }
  public void setEmbeddingReference(String embeddingReference) { this.embeddingReference = embeddingReference; }
  public String getEmbeddingVector() { return embeddingVector; }
  public void setEmbeddingVector(String embeddingVector) { this.embeddingVector = embeddingVector; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
