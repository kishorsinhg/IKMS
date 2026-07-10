package com.ikms.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id
  private UUID id;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "retained_until", nullable = false)
  private Instant retainedUntil;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "actor_username", length = 80)
  private String actorUsername;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(nullable = false, length = 64)
  private String category;

  @Column(nullable = false, length = 128)
  private String action;

  @Column(nullable = false, length = 32)
  private String outcome;

  @Column(name = "target_type", length = 64)
  private String targetType;

  @Column(name = "target_id", length = 128)
  private String targetId;

  @Column(name = "pii_access", nullable = false)
  private boolean piiAccess;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String details = "{}";

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    retainedUntil = retainedUntil == null ? occurredAt.plus(2555, ChronoUnit.DAYS) : retainedUntil;
    details = details == null || details.isBlank() ? "{}" : details;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Instant getOccurredAt() { return occurredAt; }
  public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
  public Instant getRetainedUntil() { return retainedUntil; }
  public void setRetainedUntil(Instant retainedUntil) { this.retainedUntil = retainedUntil; }
  public UUID getActorUserId() { return actorUserId; }
  public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
  public String getActorUsername() { return actorUsername; }
  public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getOutcome() { return outcome; }
  public void setOutcome(String outcome) { this.outcome = outcome; }
  public String getTargetType() { return targetType; }
  public void setTargetType(String targetType) { this.targetType = targetType; }
  public String getTargetId() { return targetId; }
  public void setTargetId(String targetId) { this.targetId = targetId; }
  public boolean isPiiAccess() { return piiAccess; }
  public void setPiiAccess(boolean piiAccess) { this.piiAccess = piiAccess; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
}
