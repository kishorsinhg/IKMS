package com.ikms.config.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mailbox_config")
public class MailboxConfig {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 120)
  private String name;

  @Column(nullable = false, length = 120)
  private String host;

  @Column(nullable = false, length = 120)
  private String username;

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
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
