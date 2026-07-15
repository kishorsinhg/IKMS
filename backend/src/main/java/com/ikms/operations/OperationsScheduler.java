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
@Table(name = "operations_scheduler")
public class OperationsScheduler {

  @Id
  @Column(name = "scheduler_key", length = 64)
  private String schedulerKey;

  @Column(name = "display_name", nullable = false, length = 160)
  private String displayName;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "run_interval_seconds", nullable = false)
  private long runIntervalSeconds;

  @Column(name = "next_execution_at")
  private Instant nextExecutionAt;

  @Column(name = "last_execution_at")
  private Instant lastExecutionAt;

  @Column(name = "last_status", length = 32)
  private String lastStatus;

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

  public String getSchedulerKey() { return schedulerKey; }
  public void setSchedulerKey(String schedulerKey) { this.schedulerKey = schedulerKey; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public long getRunIntervalSeconds() { return runIntervalSeconds; }
  public void setRunIntervalSeconds(long runIntervalSeconds) { this.runIntervalSeconds = runIntervalSeconds; }
  public Instant getNextExecutionAt() { return nextExecutionAt; }
  public void setNextExecutionAt(Instant nextExecutionAt) { this.nextExecutionAt = nextExecutionAt; }
  public Instant getLastExecutionAt() { return lastExecutionAt; }
  public void setLastExecutionAt(Instant lastExecutionAt) { this.lastExecutionAt = lastExecutionAt; }
  public String getLastStatus() { return lastStatus; }
  public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public UUID getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}

