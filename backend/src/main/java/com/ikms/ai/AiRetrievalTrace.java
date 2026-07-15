package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_retrieval_trace")
public class AiRetrievalTrace {

  @Id
  private UUID id;

  @Column(name = "interaction_id")
  private UUID interactionId;

  @Column(name = "conversation_id")
  private UUID conversationId;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(name = "retrieval_mode", nullable = false, length = 64)
  private String retrievalMode;

  @Column(name = "source_type", nullable = false, length = 32)
  private String sourceType;

  @Column(name = "source_id")
  private UUID sourceId;

  @Column(name = "page_number")
  private Integer pageNumber;

  @Column(name = "source_section", length = 255)
  private String sourceSection;

  @Column(name = "chunk_index")
  private Integer chunkIndex;

  @Column(name = "lexical_score")
  private Double lexicalScore;

  @Column(name = "vector_score")
  private Double vectorScore;

  @Column(name = "metadata_score")
  private Double metadataScore;

  @Column(name = "relationship_score")
  private Double relationshipScore;

  @Column(name = "final_score")
  private Double finalScore;

  @Column(name = "citation_quality", length = 32)
  private String citationQuality;

  @Column(name = "permission_trimmed", nullable = false)
  private boolean permissionTrimmed;

  @Column(name = "pii_masked", nullable = false)
  private boolean piiMasked;

  @Column(name = "prompt_injection_flagged", nullable = false)
  private boolean promptInjectionFlagged;

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
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getRetrievalMode() { return retrievalMode; }
  public void setRetrievalMode(String retrievalMode) { this.retrievalMode = retrievalMode; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
  public Integer getPageNumber() { return pageNumber; }
  public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
  public String getSourceSection() { return sourceSection; }
  public void setSourceSection(String sourceSection) { this.sourceSection = sourceSection; }
  public Integer getChunkIndex() { return chunkIndex; }
  public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
  public Double getLexicalScore() { return lexicalScore; }
  public void setLexicalScore(Double lexicalScore) { this.lexicalScore = lexicalScore; }
  public Double getVectorScore() { return vectorScore; }
  public void setVectorScore(Double vectorScore) { this.vectorScore = vectorScore; }
  public Double getMetadataScore() { return metadataScore; }
  public void setMetadataScore(Double metadataScore) { this.metadataScore = metadataScore; }
  public Double getRelationshipScore() { return relationshipScore; }
  public void setRelationshipScore(Double relationshipScore) { this.relationshipScore = relationshipScore; }
  public Double getFinalScore() { return finalScore; }
  public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }
  public String getCitationQuality() { return citationQuality; }
  public void setCitationQuality(String citationQuality) { this.citationQuality = citationQuality; }
  public boolean isPermissionTrimmed() { return permissionTrimmed; }
  public void setPermissionTrimmed(boolean permissionTrimmed) { this.permissionTrimmed = permissionTrimmed; }
  public boolean isPiiMasked() { return piiMasked; }
  public void setPiiMasked(boolean piiMasked) { this.piiMasked = piiMasked; }
  public boolean isPromptInjectionFlagged() { return promptInjectionFlagged; }
  public void setPromptInjectionFlagged(boolean promptInjectionFlagged) { this.promptInjectionFlagged = promptInjectionFlagged; }
  public String getRetrievalPath() { return retrievalPath; }
  public void setRetrievalPath(String retrievalPath) { this.retrievalPath = retrievalPath; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
