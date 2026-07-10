package com.ikms.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditExportService {

  private final AuditSearchService auditSearchService;

  public AuditExportService(AuditSearchService auditSearchService) {
    this.auditSearchService = auditSearchService;
  }

  public String exportCsv(
      String actorUsername,
      String action,
      UUID clientId,
      Instant fromTime,
      Instant toTime) {
    StringBuilder csv = new StringBuilder();
    csv.append("occurredAt,actorUsername,category,action,outcome,clientId,targetType,targetId,piiAccess,retainedUntil,details\n");
    auditSearchService.search(actorUsername, action, clientId, fromTime, toTime).forEach(log -> {
      csv.append(escape(log.occurredAt()));
      csv.append(',');
      csv.append(escape(log.actorUsername()));
      csv.append(',');
      csv.append(escape(log.category()));
      csv.append(',');
      csv.append(escape(log.action()));
      csv.append(',');
      csv.append(escape(log.outcome()));
      csv.append(',');
      csv.append(escape(log.clientId()));
      csv.append(',');
      csv.append(escape(log.targetType()));
      csv.append(',');
      csv.append(escape(log.targetId()));
      csv.append(',');
      csv.append(escape(log.piiAccess()));
      csv.append(',');
      csv.append(escape(log.retainedUntil()));
      csv.append(',');
      csv.append(escape(log.details().toString()));
      csv.append('\n');
    });
    return csv.toString();
  }

  private String escape(Object value) {
    String text = value == null ? "" : value.toString();
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
