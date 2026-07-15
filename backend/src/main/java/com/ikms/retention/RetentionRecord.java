package com.ikms.retention;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retention_record")
public class RetentionRecord {

  @Id
  private UUID id;

  @Column(name = "target_type", nullable = false, length = 32)
  private String targetType;

  @Column(name = "target_id", nullable = false, length = 128)
  private String targetId;

  @Column(name = "client_id")
  private UUID clientId;

  @Column(name = "legal_hold", nullable = false)
  private boolean legalHold;

  @Column(name = "minimum_retention_until")
  private Instant minimumRetentionUntil;

  @Column(name = "hold_type", length = 64)
  private String holdType;

  @Column(name = "retention_policy_key", length = 120)
  private String retentionPolicyKey;

  @Column(name = "review_at")
  private Instant reviewAt;

  @Column(name = "archival_eligible_at")
  private Instant archivalEligibleAt;

  @Column(name = "disposal_eligible_at")
  private Instant disposalEligibleAt;

  @Column(name = "last_action", length = 32)
  private String lastAction;

  @Column(name = "last_outcome", length = 32)
  private String lastOutcome;

  @Column(name = "last_reason", length = 4000)
  private String lastReason;

  @Column(name = "executed_at")
  private Instant executedAt;

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
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public void setClientId(UUID clientId) {
    this.clientId = clientId;
  }

  public boolean isLegalHold() {
    return legalHold;
  }

  public void setLegalHold(boolean legalHold) {
    this.legalHold = legalHold;
  }

  public Instant getMinimumRetentionUntil() {
    return minimumRetentionUntil;
  }

  public void setMinimumRetentionUntil(Instant minimumRetentionUntil) {
    this.minimumRetentionUntil = minimumRetentionUntil;
  }

  public String getHoldType() {
    return holdType;
  }

  public void setHoldType(String holdType) {
    this.holdType = holdType;
  }

  public String getRetentionPolicyKey() {
    return retentionPolicyKey;
  }

  public void setRetentionPolicyKey(String retentionPolicyKey) {
    this.retentionPolicyKey = retentionPolicyKey;
  }

  public Instant getReviewAt() {
    return reviewAt;
  }

  public void setReviewAt(Instant reviewAt) {
    this.reviewAt = reviewAt;
  }

  public Instant getArchivalEligibleAt() {
    return archivalEligibleAt;
  }

  public void setArchivalEligibleAt(Instant archivalEligibleAt) {
    this.archivalEligibleAt = archivalEligibleAt;
  }

  public Instant getDisposalEligibleAt() {
    return disposalEligibleAt;
  }

  public void setDisposalEligibleAt(Instant disposalEligibleAt) {
    this.disposalEligibleAt = disposalEligibleAt;
  }

  public String getLastAction() {
    return lastAction;
  }

  public void setLastAction(String lastAction) {
    this.lastAction = lastAction;
  }

  public String getLastOutcome() {
    return lastOutcome;
  }

  public void setLastOutcome(String lastOutcome) {
    this.lastOutcome = lastOutcome;
  }

  public String getLastReason() {
    return lastReason;
  }

  public void setLastReason(String lastReason) {
    this.lastReason = lastReason;
  }

  public Instant getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(Instant executedAt) {
    this.executedAt = executedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
