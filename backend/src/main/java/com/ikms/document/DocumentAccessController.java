package com.ikms.document;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.security.AppUserPrincipal;
import com.ikms.security.SecurityTrimService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/documents")
public class DocumentAccessController {

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentRedactionService documentRedactionService;
  private final com.ikms.storage.FileStorageService fileStorageService;
  private final SecurityTrimService securityTrimService;
  private final AuditService auditService;

  public DocumentAccessController(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentRedactionService documentRedactionService,
      com.ikms.storage.FileStorageService fileStorageService,
      SecurityTrimService securityTrimService,
      AuditService auditService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentRedactionService = documentRedactionService;
    this.fileStorageService = fileStorageService;
    this.securityTrimService = securityTrimService;
    this.auditService = auditService;
  }

  @GetMapping("/{documentId}/preview")
  public ResponseEntity<Resource> preview(@PathVariable UUID documentId, Authentication authentication) {
    return serve(documentId, authentication, true);
  }

  @GetMapping("/{documentId}/download")
  public ResponseEntity<Resource> download(@PathVariable UUID documentId, Authentication authentication) {
    return serve(documentId, authentication, false);
  }

  private ResponseEntity<Resource> serve(UUID documentId, Authentication authentication, boolean preview) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Current document version not found: " + documentId));
    AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
    boolean containsPii = document.getClient() != null;

    if (containsPii && version.getRedactionStatus() != RedactionStatus.AVAILABLE) {
      try {
        version = documentRedactionService.ensureRedactedVariant(version, true);
      } catch (RuntimeException exception) {
        auditDenied(documentId, principal, preview ? "DOCUMENT_PREVIEW_DENIED" : "DOCUMENT_DOWNLOAD_DENIED", exception.getMessage());
      }
    }

    SecurityTrimService.SecurityTrimDecision decision = preview
        ? securityTrimService.evaluateDocumentPreview(
            principal.permissions(),
            containsPii,
            version.getRedactionStatus() == RedactionStatus.AVAILABLE && version.getRedactedStoragePath() != null)
        : securityTrimService.evaluateDocumentDownload(
            principal.permissions(),
            containsPii,
            version.getRedactionStatus() == RedactionStatus.AVAILABLE && version.getRedactedStoragePath() != null);

    if (!decision.allowed()) {
      auditDenied(documentId, principal, preview ? "DOCUMENT_PREVIEW_DENIED" : "DOCUMENT_DOWNLOAD_DENIED", decision.reason());
      throw new ResponseStatusException(FORBIDDEN, decision.reason());
    }

    boolean serveRedacted = decision.redactionRequired();
    String storageKey = serveRedacted ? version.getRedactedStoragePath() : version.getOriginalStoragePath();
    Resource resource = fileStorageService.load(storageKey);
    String filename = serveRedacted ? version.getFileName().replaceAll("\\.[^.]+$", "") + "-redacted.txt" : version.getFileName();
    String contentType = serveRedacted ? "text/plain" : version.getMimeType();

    auditService.write(new AuditEvent(
        Instant.now(),
        "DOCUMENT",
        preview
            ? (serveRedacted ? "DOCUMENT_PREVIEW_REDACTED" : "DOCUMENT_PREVIEW_ORIGINAL")
            : (serveRedacted ? "DOCUMENT_DOWNLOAD_REDACTED" : "DOCUMENT_DOWNLOAD_ORIGINAL"),
        AuditOutcome.SUCCESS,
        principal.id(),
        document.getClient() == null ? null : document.getClient().getId(),
        "Document",
        documentId.toString(),
        containsPii && !serveRedacted,
        Map.of("storageVariant", serveRedacted ? "REDACTED" : "ORIGINAL")));

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder(preview ? "inline" : "attachment")
            .filename(filename)
            .build()
            .toString())
        .body(resource);
  }

  private void auditDenied(UUID documentId, AppUserPrincipal principal, String action, String reason) {
    auditService.write(new AuditEvent(
        Instant.now(),
        "DOCUMENT",
        action,
        AuditOutcome.DENIED,
        principal.id(),
        null,
        "Document",
        documentId.toString(),
        false,
        Map.of("reason", reason == null ? "" : reason)));
  }
}
