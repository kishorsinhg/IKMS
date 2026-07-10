package com.ikms.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditSearchService {

  private static final TypeReference<Map<String, String>> DETAILS_TYPE = new TypeReference<>() {
  };

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public AuditSearchService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
  }

  public List<AuditContracts.AuditLogResponse> search(
      String actorUsername,
      String action,
      UUID clientId,
      Instant fromTime,
      Instant toTime) {
    return auditLogRepository.search(
            normalize(actorUsername),
            normalize(action),
            clientId,
            fromTime,
            toTime,
            PageRequest.of(0, 200))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  AuditContracts.AuditLogResponse toResponse(AuditLog auditLog) {
    return new AuditContracts.AuditLogResponse(
        auditLog.getId(),
        auditLog.getOccurredAt(),
        auditLog.getRetainedUntil(),
        auditLog.getActorUserId(),
        auditLog.getActorUsername(),
        auditLog.getClientId(),
        auditLog.getCategory(),
        auditLog.getAction(),
        auditLog.getOutcome(),
        auditLog.getTargetType(),
        auditLog.getTargetId(),
        auditLog.isPiiAccess(),
        parseDetails(auditLog.getDetails()));
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Map<String, String> parseDetails(String details) {
    if (details == null || details.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(details, DETAILS_TYPE);
    } catch (Exception exception) {
      return Map.of("raw", details);
    }
  }
}
