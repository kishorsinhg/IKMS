package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversation")
public class AiConversation {

  @Id
  private UUID id;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "scope_type", nullable = false, length = 32)
  private String scopeType;

  @Column(name = "scope_id")
  private UUID scopeId;

  @Column(name = "operation_type", nullable = false, length = 32)
  private String operationType;

  @Column(length = 255)
  private String title;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = createdAt == null ? now : createdAt;
    updatedAt = updatedAt == null ? now : updatedAt;
    status = status == null ? "ACTIVE" : status;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public UUID getActorUserId() { return actorUserId; }
  public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
  public String getScopeType() { return scopeType; }
  public void setScopeType(String scopeType) { this.scopeType = scopeType; }
  public UUID getScopeId() { return scopeId; }
  public void setScopeId(UUID scopeId) { this.scopeId = scopeId; }
  public String getOperationType() { return operationType; }
  public void setOperationType(String operationType) { this.operationType = operationType; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
