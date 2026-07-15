package com.ikms.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.security.domain.AppUserRepository;
import com.ikms.observability.RequestContextHolder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingAuditService implements AuditService {

  private static final Logger log = LoggerFactory.getLogger(LoggingAuditService.class);
  private final AuditLogRepository auditLogRepository;
  private final AppUserRepository appUserRepository;
  private final ObjectMapper objectMapper;

  public LoggingAuditService(
      AuditLogRepository auditLogRepository,
      AppUserRepository appUserRepository,
      ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.appUserRepository = appUserRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void write(AuditEvent event) {
    Map<String, Object> details = enrichDetails(event.details());
    AuditLog auditLog = new AuditLog();
    auditLog.setOccurredAt(event.occurredAt());
    auditLog.setActorUserId(event.actorUserId());
    auditLog.setActorUsername(resolveActorUsername(event.actorUserId()));
    auditLog.setClientId(event.clientId());
    auditLog.setCategory(event.category());
    auditLog.setAction(event.action());
    auditLog.setOutcome(event.outcome().name());
    auditLog.setTargetType(event.targetType());
    auditLog.setTargetId(event.targetId());
    auditLog.setPiiAccess(event.piiAccess());
    auditLog.setDetails(writeDetails(details));
    auditLogRepository.save(auditLog);

    log.info(
        "audit category={} action={} outcome={} actorUserId={} clientId={} targetType={} targetId={} piiAccess={} requestId={} correlationId={} details={}",
        event.category(),
        event.action(),
        event.outcome(),
        event.actorUserId(),
        event.clientId(),
        event.targetType(),
        event.targetId(),
        event.piiAccess(),
        RequestContextHolder.requestId(),
        RequestContextHolder.correlationId(),
        details);
  }

  private String resolveActorUsername(java.util.UUID actorUserId) {
    if (actorUserId == null) {
      return null;
    }
    return appUserRepository.findById(actorUserId)
        .map(user -> user.getUsername())
        .orElse(null);
  }

  private Map<String, Object> enrichDetails(Map<String, ?> eventDetails) {
    LinkedHashMap<String, Object> details = new LinkedHashMap<>();
    if (eventDetails != null) {
      details.putAll(eventDetails);
    }
    RequestContextHolder.traceIdentifiers().forEach(details::putIfAbsent);
    return Map.copyOf(details);
  }

  private String writeDetails(Map<String, Object> details) {
    try {
      return objectMapper.writeValueAsString(details);
    } catch (Exception exception) {
      return "{\"raw\":\"serialization_failed\"}";
    }
  }
}
