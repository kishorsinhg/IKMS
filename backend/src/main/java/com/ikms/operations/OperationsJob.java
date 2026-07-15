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
@Table(name = "operations_job")
public class OperationsJob {

  @Id
  private UUID id;

  @Column(name = "job_type", nullable = false, length = 64)
  private String jobType;

  @Column(name = "submitted_by")
  private UUID submittedBy;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(nullable = false)
  private int progress;

  @Column(name = "error_summary", columnDefinition = "TEXT")
  private String errorSummary;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "target_type", length = 64)
  private String targetType;

  @Column(name = "target_id", length = 128)
  private String targetId;

  @Column(name = "queue_key", length = 64)
  private String queueKey;

  @Column(name = "cancel_requested", nullable = false)
  private boolean cancelRequested;

  @Column(nullable = false)
  private int priority = 100;

  @Column(columnDefinition = "TEXT")
  private String details;

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
    submittedAt = submittedAt == null ? now : submittedAt;
    createdAt = createdAt == null ? now : createdAt;
    updatedAt = updatedAt == null ? now : updatedAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getJobType() { return jobType; }
  public void setJobType(String jobType) { this.jobType = jobType; }
  public UUID getSubmittedBy() { return submittedBy; }
  public void setSubmittedBy(UUID submittedBy) { this.submittedBy = submittedBy; }
  public Instant getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public Long getDurationMs() { return durationMs; }
  public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public int getProgress() { return progress; }
  public void setProgress(int progress) { this.progress = progress; }
  public String getErrorSummary() { return errorSummary; }
  public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
  public int getRetryCount() { return retryCount; }
  public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
  public String getTargetType() { return targetType; }
  public void setTargetType(String targetType) { this.targetType = targetType; }
  public String getTargetId() { return targetId; }
  public void setTargetId(String targetId) { this.targetId = targetId; }
  public String getQueueKey() { return queueKey; }
  public void setQueueKey(String queueKey) { this.queueKey = queueKey; }
  public boolean isCancelRequested() { return cancelRequested; }
  public void setCancelRequested(boolean cancelRequested) { this.cancelRequested = cancelRequested; }
  public int getPriority() { return priority; }
  public void setPriority(int priority) { this.priority = priority; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

