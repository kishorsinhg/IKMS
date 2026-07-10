package com.ikms.audit;

import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

  private final AuditSearchService auditSearchService;
  private final AuditExportService auditExportService;

  public AuditController(AuditSearchService auditSearchService, AuditExportService auditExportService) {
    this.auditSearchService = auditSearchService;
    this.auditExportService = auditExportService;
  }

  @GetMapping
  public List<AuditContracts.AuditLogResponse> search(
      @RequestParam(name = "actor", required = false) String actorUsername,
      @RequestParam(name = "action", required = false) String action,
      @RequestParam(name = "clientId", required = false) UUID clientId,
      @RequestParam(name = "from", required = false) Instant fromTime,
      @RequestParam(name = "to", required = false) Instant toTime,
      Authentication authentication) {
    requirePermission(authentication, Permission.VIEW_AUDIT);
    return auditSearchService.search(actorUsername, action, clientId, fromTime, toTime);
  }

  @GetMapping(value = "/export", produces = "text/csv")
  public ResponseEntity<String> export(
      @RequestParam(name = "actor", required = false) String actorUsername,
      @RequestParam(name = "action", required = false) String action,
      @RequestParam(name = "clientId", required = false) UUID clientId,
      @RequestParam(name = "from", required = false) Instant fromTime,
      @RequestParam(name = "to", required = false) Instant toTime,
      Authentication authentication) {
    requirePermission(authentication, Permission.EXPORT_AUDIT);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log-export.csv\"")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(auditExportService.exportCsv(actorUsername, action, clientId, fromTime, toTime));
  }

  private void requirePermission(Authentication authentication, Permission permission) {
    AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
    if (!principal.permissions().contains(permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access audit logs.");
    }
  }
}
