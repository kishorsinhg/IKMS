package com.ikms.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ClientContracts {

  private ClientContracts() {
  }

  public record CreateClientRequest(
      String clientId,
      ClientType clientType,
      @NotBlank(message = "Display name is required.")
      @Size(max = 200, message = "Display name must be 200 characters or fewer.")
      String displayName,
      @Size(max = 200, message = "Legal name must be 200 characters or fewer.")
      String legalName,
      @Size(max = 255, message = "Primary email must be 255 characters or fewer.")
      String primaryEmail,
      @Size(max = 64, message = "Primary phone must be 64 characters or fewer.")
      String primaryPhone,
      @Size(max = 255, message = "Contact person must be 255 characters or fewer.")
      String contactPerson) {
  }

  public record ClientProfileResponse(
      UUID id,
      String clientId,
      boolean clientIdTemporary,
      ClientType clientType,
      ClientStatus status,
      String displayName,
      String legalName,
      String primaryEmail,
      String primaryPhone,
      String contactPerson,
      Instant createdAt,
      Instant updatedAt) {
  }

  public record ClientSummaryResponse(
      UUID id,
      String clientId,
      boolean clientIdTemporary,
      ClientType clientType,
      ClientStatus status,
      String displayName) {
  }

  public record UpdateClientRequest(
      String clientId,
      ClientType clientType,
      @NotBlank(message = "Display name is required.")
      @Size(max = 200, message = "Display name must be 200 characters or fewer.")
      String displayName,
      @Size(max = 200, message = "Legal name must be 200 characters or fewer.")
      String legalName,
      @Size(max = 255, message = "Primary email must be 255 characters or fewer.")
      String primaryEmail,
      @Size(max = 64, message = "Primary phone must be 64 characters or fewer.")
      String primaryPhone,
      @Size(max = 255, message = "Contact person must be 255 characters or fewer.")
      String contactPerson,
      ClientStatus status) {
  }
}
