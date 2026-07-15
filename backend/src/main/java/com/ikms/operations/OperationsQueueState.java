package com.ikms.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operations_queue_state")
public class OperationsQueueState {

  @Id
  @Column(name = "queue_key", length = 64)
  private String queueKey;

  @Column(nullable = false)
  private boolean paused;

  @Column(name = "paused_at")
  private Instant pausedAt;

  @Column(name = "resumed_at")
  private Instant resumedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @PrePersist
  void onCreate() {
    updatedAt = updatedAt == null ? Instant.now() : updatedAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public String getQueueKey() { return queueKey; }
  public void setQueueKey(String queueKey) { this.queueKey = queueKey; }
  public boolean isPaused() { return paused; }
  public void setPaused(boolean paused) { this.paused = paused; }
  public Instant getPausedAt() { return pausedAt; }
  public void setPausedAt(Instant pausedAt) { this.pausedAt = pausedAt; }
  public Instant getResumedAt() { return resumedAt; }
  public void setResumedAt(Instant resumedAt) { this.resumedAt = resumedAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public UUID getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}

