package com.ikms.client;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClientIdService {

  private final ClientRepository clientRepository;

  public ClientIdService(ClientRepository clientRepository) {
    this.clientRepository = clientRepository;
  }

  public ResolvedClientId resolveForCreate(String requestedClientId) {
    if (requestedClientId == null || requestedClientId.isBlank()) {
      return new ResolvedClientId(generateTemporaryClientId(), true);
    }

    String normalized = normalize(requestedClientId);
    assertUnique(normalized, null);
    return new ResolvedClientId(normalized, false);
  }

  public ResolvedClientId resolveForUpdate(UUID clientInternalId, boolean currentTemporary, String requestedClientId) {
    if (requestedClientId == null || requestedClientId.isBlank()) {
      if (currentTemporary) {
        return new ResolvedClientId(generateTemporaryClientId(), true);
      }
      throw new IllegalArgumentException("Client ID is required.");
    }

    String normalized = normalize(requestedClientId);
    assertUnique(normalized, clientInternalId);
    return new ResolvedClientId(normalized, normalized.startsWith("TMP-"));
  }

  private String generateTemporaryClientId() {
    String candidate;
    do {
      candidate = "TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    } while (clientRepository.findByClientIdIgnoreCase(candidate).isPresent());
    return candidate;
  }

  private void assertUnique(String clientId, UUID excludedClientId) {
    clientRepository.findByClientIdIgnoreCase(clientId).ifPresent(existing -> {
      if (excludedClientId == null || !existing.getId().equals(excludedClientId)) {
        throw new IllegalArgumentException("Client ID must be unique.");
      }
    });
  }

  private String normalize(String clientId) {
    return clientId.trim().toUpperCase(Locale.ROOT);
  }

  public record ResolvedClientId(String clientId, boolean temporary) {
  }
}
