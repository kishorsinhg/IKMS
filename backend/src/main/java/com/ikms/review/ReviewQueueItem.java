package com.ikms.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_queue_item")
public class ReviewQueueItem {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 32)
  private ReviewQueueItemType itemType;

  @Column(name = "item_id", nullable = false, length = 128)
  private String itemId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private ReviewQueueReason reason;

  @Column(name = "assigned_to")
  private UUID assignedTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ReviewQueueStatus status = ReviewQueueStatus.OPEN;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public ReviewQueueItemType getItemType() { return itemType; }
  public void setItemType(ReviewQueueItemType itemType) { this.itemType = itemType; }
  public String getItemId() { return itemId; }
  public void setItemId(String itemId) { this.itemId = itemId; }
  public ReviewQueueReason getReason() { return reason; }
  public void setReason(ReviewQueueReason reason) { this.reason = reason; }
  public UUID getAssignedTo() { return assignedTo; }
  public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
  public ReviewQueueStatus getStatus() { return status; }
  public void setStatus(ReviewQueueStatus status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getResolvedAt() { return resolvedAt; }
  public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
