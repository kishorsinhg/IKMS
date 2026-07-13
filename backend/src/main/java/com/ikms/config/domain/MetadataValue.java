package com.ikms.config.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "metadata_value")
public class MetadataValue {

  @Id
  private UUID id;

  @Column(name = "owner_type", nullable = false, length = 32)
  private String ownerType;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "field_id", nullable = false)
  private MetadataField field;

  @Column(name = "text_value", nullable = false, length = 4000)
  private String textValue;

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

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public MetadataField getField() {
    return field;
  }

  public void setField(MetadataField field) {
    this.field = field;
  }

  public String getTextValue() {
    return textValue;
  }

  public void setTextValue(String textValue) {
    this.textValue = textValue;
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
