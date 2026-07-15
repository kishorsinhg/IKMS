package com.ikms.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

  public record BusinessReferenceFieldResponse(
      String key,
      String label,
      String value) {
  }

  public record KnowledgeEvidenceReferenceResponse(
      String sourceType,
      UUID sourceId,
      UUID sourceVersionId,
      String title,
      String detail,
      Integer pageNumber,
      String section,
      String jumpTargetId) {
  }

  public record CustomerKnowledgeTimelineEventResponse(
      String eventId,
      UUID customerId,
      String eventType,
      String sourceType,
      UUID sourceId,
      UUID sourceVersionId,
      String title,
      String summary,
      Instant occurredAt,
      Instant recordedAt,
      String actor,
      String documentType,
      List<BusinessReferenceFieldResponse> businessReferenceFields,
      String status,
      List<KnowledgeEvidenceReferenceResponse> evidenceReferences,
      List<String> availableActions,
      String permissionState,
      String correlationId) {
  }

  public record CustomerKnowledgeTimelineFiltersResponse(
      String query,
      String from,
      String to,
      String sourceType,
      String eventType,
      String documentType,
      String reviewStatus,
      String policyNumber,
      String claimNumber,
      String insurer,
      String actor,
      String sortDirection,
      int limit) {
  }

  public record CustomerKnowledgeTimelinePageResponse(
      List<CustomerKnowledgeTimelineEventResponse> events,
      String nextCursor,
      boolean hasMore,
      CustomerKnowledgeTimelineFiltersResponse appliedFilters) {
  }

  public record RelatedKnowledgeLinkResponse(
      String relationshipId,
      UUID customerId,
      String sourceType,
      UUID sourceId,
      String sourceTitle,
      String relatedSourceType,
      UUID relatedSourceId,
      String relatedTitle,
      String relationshipType,
      Double score,
      String explanation,
      Map<String, String> supportingFields,
      List<KnowledgeEvidenceReferenceResponse> evidenceReferences,
      String derivationType,
      Instant createdAt,
      boolean inferred) {
  }

  public record RelatedKnowledgeResponse(
      UUID customerId,
      String sourceType,
      UUID sourceId,
      List<RelatedKnowledgeLinkResponse> links,
      String restrictedContentNotice) {
  }

  public record DocumentVersionSummaryResponse(
      UUID id,
      UUID documentId,
      int versionNumber,
      String fileName,
      String mimeType,
      String redactionStatus,
      boolean current,
      String fileHash,
      Instant createdAt,
      UUID createdBy) {
  }
}
