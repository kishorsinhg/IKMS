package com.ikms.config;

import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AdminConfigurationContracts {

  private AdminConfigurationContracts() {
  }

  public record AdminUserResponse(UUID id, String username, String displayName, String email, UserStatus status, Set<UserRole> roles) {
  }

  public record DocumentTypeRequest(@NotBlank(message = "Name is required.") String name, String description, boolean active) {
  }
  public record DocumentTypeResponse(UUID id, String name, String description, boolean active, Instant createdAt) {
  }

  public record MetadataFieldRequest(@NotBlank(message = "Field key is required.") String fieldKey, @NotBlank(message = "Label is required.") String label, boolean pii, boolean active) {
  }
  public record MetadataFieldResponse(UUID id, String fieldKey, String label, boolean pii, boolean active, Instant createdAt) {
  }

  public record SharedFolderRequest(@NotBlank(message = "Path is required.") String path, boolean active) {
  }
  public record SharedFolderResponse(UUID id, String path, boolean active, Instant createdAt) {
  }

  public record MailboxConfigRequest(@NotBlank(message = "Name is required.") String name, @NotBlank(message = "Host is required.") String host, @NotBlank(message = "Username is required.") String username, boolean active) {
  }
  public record MailboxConfigResponse(UUID id, String name, String host, String username, boolean active, Instant createdAt) {
  }

  public record ReviewSettingRequest(@NotBlank(message = "Mode is required.") String mode, double lowConfidenceThreshold) {
  }
  public record ReviewSettingResponse(UUID id, String mode, double lowConfidenceThreshold, Instant updatedAt) {
  }

  public record AiProviderSettingRequest(
      @NotBlank(message = "Provider name is required.") String providerName,
      @NotBlank(message = "Model name is required.") String modelName,
      String apiBaseUrl,
      String apiKey,
      @NotBlank(message = "OCR provider is required.") String ocrProvider,
      boolean active) {
  }
  public record AiProviderSettingResponse(
      UUID id,
      String providerName,
      String modelName,
      String apiBaseUrl,
      boolean apiKeyConfigured,
      String ocrProvider,
      boolean active,
      Instant updatedAt) {
  }
}
