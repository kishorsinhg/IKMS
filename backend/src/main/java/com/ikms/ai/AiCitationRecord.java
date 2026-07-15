package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_citation_record")
public class AiCitationRecord {

  @Id
  private UUID id;

  @Column(name = "interaction_id")
  private UUID interactionId;

  @Column(name = "conversation_id")
  private UUID conversationId;

  @Column(name = "source_type", nullable = false, length = 32)
  private String sourceType;

  @Column(name = "source_id")
  private UUID sourceId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String excerpt;

  @Column(name = "page_number")
  private Integer pageNumber;

  @Column(name = "chunk_index")
  private Integer chunkIndex;

  @Column(name = "source_section", length = 255)
  private String sourceSection;

  @Column(length = 32)
  private String confidence;

  @Column(name = "evidence_text", columnDefinition = "TEXT")
  private String evidenceText;

  @Column(name = "jump_target_id", length = 255)
  private String jumpTargetId;

  @Column(name = "retrieval_path", length = 64)
  private String retrievalPath;

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
  public UUID getInteractionId() { return interactionId; }
  public void setInteractionId(UUID interactionId) { this.interactionId = interactionId; }
  public UUID getConversationId() { return conversationId; }
  public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getExcerpt() { return excerpt; }
  public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
  public Integer getPageNumber() { return pageNumber; }
  public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
  public Integer getChunkIndex() { return chunkIndex; }
  public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
  public String getSourceSection() { return sourceSection; }
  public void setSourceSection(String sourceSection) { this.sourceSection = sourceSection; }
  public String getConfidence() { return confidence; }
  public void setConfidence(String confidence) { this.confidence = confidence; }
  public String getEvidenceText() { return evidenceText; }
  public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }
  public String getJumpTargetId() { return jumpTargetId; }
  public void setJumpTargetId(String jumpTargetId) { this.jumpTargetId = jumpTargetId; }
  public String getRetrievalPath() { return retrievalPath; }
  public void setRetrievalPath(String retrievalPath) { this.retrievalPath = retrievalPath; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
