package com.ikms.security.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 160)
  private String displayName;

  @Column(length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "failed_login_count", nullable = false)
  private int failedLoginCount = 0;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role_name", nullable = false, length = 64)
  @Enumerated(EnumType.STRING)
  private Set<UserRole> roles = new LinkedHashSet<>();

  @PrePersist
  void assignIdIfMissing() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  public void setFailedLoginCount(int failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public Set<UserRole> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserRole> roles) {
    this.roles = new LinkedHashSet<>(roles);
  }
}
