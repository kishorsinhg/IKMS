package com.ikms.config.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_setting")
public class ReviewSetting {

  @Id
  private UUID id;

  @Column(nullable = false, length = 40)
  private String mode;

  @Column(name = "low_confidence_threshold", nullable = false)
  private double lowConfidenceThreshold;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    updatedAt = updatedAt == null ? Instant.now() : updatedAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }
  public double getLowConfidenceThreshold() { return lowConfidenceThreshold; }
  public void setLowConfidenceThreshold(double lowConfidenceThreshold) { this.lowConfidenceThreshold = lowConfidenceThreshold; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
