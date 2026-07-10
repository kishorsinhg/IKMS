package com.ikms.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingAuditService implements AuditService {

  private static final Logger log = LoggerFactory.getLogger(LoggingAuditService.class);

  @Override
  public void write(AuditEvent event) {
    log.info(
        "audit category={} action={} outcome={} actorUserId={} clientId={} targetType={} targetId={} piiAccess={} details={}",
        event.category(),
        event.action(),
        event.outcome(),
        event.actorUserId(),
        event.clientId(),
        event.targetType(),
        event.targetId(),
        event.piiAccess(),
        event.details());
  }
}
