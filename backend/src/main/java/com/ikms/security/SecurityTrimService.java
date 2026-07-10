package com.ikms.security;

import com.ikms.security.domain.Permission;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SecurityTrimService {

  private final PiiMaskingService piiMaskingService;

  public SecurityTrimService(PiiMaskingService piiMaskingService) {
    this.piiMaskingService = piiMaskingService;
  }

  public SecurityTrimDecision evaluateDocumentPreview(Set<Permission> permissions, boolean containsPii,
      boolean redactionAvailable) {
    return evaluateDocumentAccess(permissions, containsPii, redactionAvailable, "preview");
  }

  public SecurityTrimDecision evaluateDocumentDownload(Set<Permission> permissions, boolean containsPii,
      boolean redactionAvailable) {
    return evaluateDocumentAccess(permissions, containsPii, redactionAvailable, "download");
  }

  public SecurityTrimDecision evaluateSearchRetrieval(Set<Permission> permissions, boolean containsPii) {
    if (!permissions.contains(Permission.SEARCH_CLIENT_KNOWLEDGE)) {
      return SecurityTrimDecision.denied("Missing search permission.");
    }
    if (containsPii && !permissions.contains(Permission.VIEW_PII)) {
      return SecurityTrimDecision.allowed(true, false, "PII-bearing search results must be trimmed.");
    }
    return SecurityTrimDecision.allowed(false, containsPii && permissions.contains(Permission.VIEW_PII), "Allowed");
  }

  public SecurityTrimDecision evaluateAiContextAssembly(Set<Permission> permissions, boolean containsPii) {
    if (!permissions.contains(Permission.ASK_CLIENT_AI)) {
      return SecurityTrimDecision.denied("Missing AI permission.");
    }
    if (containsPii && !permissions.contains(Permission.VIEW_PII)) {
      return SecurityTrimDecision.allowed(true, false, "PII-bearing context must be trimmed.");
    }
    return SecurityTrimDecision.allowed(false, containsPii && permissions.contains(Permission.VIEW_PII), "Allowed");
  }

  public String trimSearchResult(Set<Permission> permissions, String text, boolean containsPii) {
    SecurityTrimDecision decision = evaluateSearchRetrieval(permissions, containsPii);
    if (!decision.allowed()) {
      throw new IllegalArgumentException(decision.reason());
    }
    return decision.redactionRequired() ? piiMaskingService.trimFreeText(text, permissions) : text;
  }

  public String trimAiContext(Set<Permission> permissions, String text, boolean containsPii) {
    SecurityTrimDecision decision = evaluateAiContextAssembly(permissions, containsPii);
    if (!decision.allowed()) {
      throw new IllegalArgumentException(decision.reason());
    }
    return decision.redactionRequired() ? piiMaskingService.trimFreeText(text, permissions) : text;
  }

  private SecurityTrimDecision evaluateDocumentAccess(Set<Permission> permissions, boolean containsPii,
      boolean redactionAvailable, String operation) {
    if (!containsPii) {
      return SecurityTrimDecision.allowed(false, false, "Allowed");
    }
    if (permissions.contains(Permission.VIEW_ORIGINAL_DOCUMENTS) && permissions.contains(Permission.VIEW_PII)) {
      return SecurityTrimDecision.allowed(false, true, "Original " + operation + " allowed.");
    }
    if (!permissions.contains(Permission.VIEW_REDACTED_DOCUMENTS)) {
      return SecurityTrimDecision.denied("Missing redacted document permission.");
    }
    if (!redactionAvailable) {
      return SecurityTrimDecision.denied("PII access denied because no redacted content is available.");
    }
    return SecurityTrimDecision.allowed(true, false, "Redacted " + operation + " required.");
  }

  public record SecurityTrimDecision(boolean allowed, boolean redactionRequired, boolean piiPermitted, String reason) {

    static SecurityTrimDecision allowed(boolean redactionRequired, boolean piiPermitted, String reason) {
      return new SecurityTrimDecision(true, redactionRequired, piiPermitted, reason);
    }

    static SecurityTrimDecision denied(String reason) {
      return new SecurityTrimDecision(false, false, false, reason);
    }
  }
}
