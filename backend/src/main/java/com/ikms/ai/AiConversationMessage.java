package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversation_message")
public class AiConversationMessage {

  @Id
  private UUID id;

  @Column(name = "conversation_id", nullable = false)
  private UUID conversationId;

  @Column(nullable = false, length = 32)
  private String role;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "provider_name", length = 80)
  private String providerName;

  @Column(name = "model_name", length = 120)
  private String modelName;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "prompt_tokens")
  private Integer promptTokens;

  @Column(name = "completion_tokens")
  private Integer completionTokens;

  @Column(name = "total_tokens")
  private Integer totalTokens;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
    status = status == null ? "READY" : status;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getConversationId() { return conversationId; }
  public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getProviderName() { return providerName; }
  public void setProviderName(String providerName) { this.providerName = providerName; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getPromptTokens() { return promptTokens; }
  public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
  public Integer getCompletionTokens() { return completionTokens; }
  public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
  public Integer getTotalTokens() { return totalTokens; }
  public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
