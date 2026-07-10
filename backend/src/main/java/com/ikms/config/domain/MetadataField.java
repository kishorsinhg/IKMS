package com.ikms.config.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "metadata_field")
public class MetadataField {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 120)
  private String fieldKey;

  @Column(nullable = false, length = 120)
  private String label;

  @Column(nullable = false)
  private boolean pii = false;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getFieldKey() { return fieldKey; }
  public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public boolean isPii() { return pii; }
  public void setPii(boolean pii) { this.pii = pii; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
