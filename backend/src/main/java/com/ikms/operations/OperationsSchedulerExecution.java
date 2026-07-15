package com.ikms.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operations_scheduler_execution")
public class OperationsSchedulerExecution {

  @Id
  private UUID id;

  @Column(name = "scheduler_key", nullable = false, length = 64)
  private String schedulerKey;

  @Column(name = "triggered_by")
  private UUID triggeredBy;

  @Column(name = "trigger_source", nullable = false, length = 32)
  private String triggerSource;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(columnDefinition = "TEXT")
  private String details;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    startedAt = startedAt == null ? Instant.now() : startedAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getSchedulerKey() { return schedulerKey; }
  public void setSchedulerKey(String schedulerKey) { this.schedulerKey = schedulerKey; }
  public UUID getTriggeredBy() { return triggeredBy; }
  public void setTriggeredBy(UUID triggeredBy) { this.triggeredBy = triggeredBy; }
  public String getTriggerSource() { return triggerSource; }
  public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

