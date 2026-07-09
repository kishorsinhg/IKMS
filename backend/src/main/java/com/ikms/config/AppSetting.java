package com.ikms.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_setting")
public class AppSetting {

  @Id
  @Column(name = "setting_key", nullable = false, length = 160)
  private String key;

  @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
  private String value;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
