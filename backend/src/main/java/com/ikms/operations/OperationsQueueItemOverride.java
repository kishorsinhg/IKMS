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
@Table(name = "operations_queue_item_override")
public class OperationsQueueItemOverride {

  @Id
  private UUID id;

  @Column(name = "queue_key", nullable = false, length = 64)
  private String queueKey;

  @Column(name = "item_id", nullable = false, length = 128)
  private String itemId;

  @Column(nullable = false)
  private int priority = 100;

  @Column(nullable = false)
  private boolean cancelled;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    updatedAt = updatedAt == null ? Instant.now() : updatedAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getQueueKey() { return queueKey; }
  public void setQueueKey(String queueKey) { this.queueKey = queueKey; }
  public String getItemId() { return itemId; }
  public void setItemId(String itemId) { this.itemId = itemId; }
  public int getPriority() { return priority; }
  public void setPriority(int priority) { this.priority = priority; }
  public boolean isCancelled() { return cancelled; }
  public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public UUID getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}

