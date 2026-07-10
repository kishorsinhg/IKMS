package com.ikms.client;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {

  private final ClientRepository clientRepository;
  private final ClientIdService clientIdService;
  private final AuditService auditService;

  public ClientService(
      ClientRepository clientRepository,
      ClientIdService clientIdService,
      AuditService auditService) {
    this.clientRepository = clientRepository;
    this.clientIdService = clientIdService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<ClientContracts.ClientSummaryResponse> search(String query) {
    return clientRepository.searchByQuery(query == null ? "" : query.trim()).stream()
        .map(this::toSummary)
        .toList();
  }

  public ClientContracts.ClientProfileResponse create(
      ClientContracts.CreateClientRequest request,
      UUID actorUserId) {
    ClientIdService.ResolvedClientId resolvedClientId = clientIdService.resolveForCreate(request.clientId());

    Client client = new Client();
    client.setClientId(resolvedClientId.clientId());
    client.setClientIdTemporary(resolvedClientId.temporary());
    client.setClientType(request.clientType() == null ? ClientType.INDIVIDUAL : request.clientType());
    client.setStatus(ClientStatus.ACTIVE);
    applyCommonFields(client, request.displayName(), request.legalName(), request.primaryEmail(),
        request.primaryPhone(), request.contactPerson());

    Client saved = clientRepository.save(client);
    auditService.write(new AuditEvent(
        Instant.now(),
        "CLIENT",
        "CLIENT_CREATED",
        AuditOutcome.SUCCESS,
        actorUserId,
        saved.getId(),
        "Client",
        saved.getId().toString(),
        false,
        Map.of("clientId", saved.getClientId(), "temporary", Boolean.toString(saved.isClientIdTemporary()))));
    return toProfile(saved);
  }

  @Transactional(readOnly = true)
  public ClientContracts.ClientProfileResponse get(UUID clientId) {
    return toProfile(requireClient(clientId));
  }

  public ClientContracts.ClientProfileResponse update(
      UUID clientId,
      ClientContracts.UpdateClientRequest request,
      UUID actorUserId) {
    Client client = requireClient(clientId);
    String previousClientId = client.getClientId();
    boolean previousTemporary = client.isClientIdTemporary();

    ClientIdService.ResolvedClientId resolvedClientId = clientIdService.resolveForUpdate(
        clientId,
        client.isClientIdTemporary(),
        request.clientId());

    client.setClientId(resolvedClientId.clientId());
    client.setClientIdTemporary(resolvedClientId.temporary());
    client.setClientType(request.clientType() == null ? client.getClientType() : request.clientType());
    client.setStatus(request.status() == null ? client.getStatus() : request.status());
    applyCommonFields(client, request.displayName(), request.legalName(), request.primaryEmail(),
        request.primaryPhone(), request.contactPerson());

    Client saved = clientRepository.save(client);

    if (!previousClientId.equals(saved.getClientId()) || previousTemporary != saved.isClientIdTemporary()) {
      auditService.write(new AuditEvent(
          Instant.now(),
          "CLIENT",
          "CLIENT_ID_UPDATED",
          AuditOutcome.SUCCESS,
          actorUserId,
          saved.getId(),
          "Client",
          saved.getId().toString(),
          false,
          Map.of(
              "previousClientId", previousClientId,
              "newClientId", saved.getClientId(),
              "temporary", Boolean.toString(saved.isClientIdTemporary()))));
    }

    return toProfile(saved);
  }

  public Client requireClient(UUID clientId) {
    return clientRepository.findById(clientId)
        .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
  }

  private void applyCommonFields(
      Client client,
      String displayName,
      String legalName,
      String primaryEmail,
      String primaryPhone,
      String contactPerson) {
    client.setDisplayName(displayName.trim());
    client.setLegalName(normalizeNullable(legalName));
    client.setPrimaryEmail(normalizeNullable(primaryEmail));
    client.setPrimaryPhone(normalizeNullable(primaryPhone));
    client.setContactPerson(normalizeNullable(contactPerson));
  }

  private String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private ClientContracts.ClientSummaryResponse toSummary(Client client) {
    return new ClientContracts.ClientSummaryResponse(
        client.getId(),
        client.getClientId(),
        client.isClientIdTemporary(),
        client.getClientType(),
        client.getStatus(),
        client.getDisplayName());
  }

  private ClientContracts.ClientProfileResponse toProfile(Client client) {
    return new ClientContracts.ClientProfileResponse(
        client.getId(),
        client.getClientId(),
        client.isClientIdTemporary(),
        client.getClientType(),
        client.getStatus(),
        client.getDisplayName(),
        client.getLegalName(),
        client.getPrimaryEmail(),
        client.getPrimaryPhone(),
        client.getContactPerson(),
        client.getCreatedAt(),
        client.getUpdatedAt());
  }
}
