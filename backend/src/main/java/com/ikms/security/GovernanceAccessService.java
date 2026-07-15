package com.ikms.security;

import com.ikms.document.Document;
import com.ikms.governance.DocumentLifecycleState;
import com.ikms.governance.InformationClassification;
import com.ikms.security.domain.Permission;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GovernanceAccessService {

  public GovernanceDecision evaluateDocumentAccess(
      AppUserPrincipal principal,
      Document document,
      GovernanceAction action) {
    return evaluate(principal.permissions(), ActorAttributeContext.from(principal).asMap(), document, action);
  }

  public GovernanceDecision evaluate(
      Set<Permission> permissions,
      Map<String, String> actorAttributes,
      Document document,
      GovernanceAction action) {
    if (document == null) {
      return GovernanceDecision.allow();
    }
    if (document.getLifecycleState() == DocumentLifecycleState.DISPOSED) {
      return GovernanceDecision.deny("Document has been disposed and is no longer accessible.");
    }
    InformationClassification clearance = InformationClassification.parse(
        actorAttributes.get("securityClearance"),
        InformationClassification.INTERNAL);
    InformationClassification required = document.getClassification() == null
        ? InformationClassification.INTERNAL
        : document.getClassification();
    if (!clearance.satisfies(required)) {
      return GovernanceDecision.deny("Security clearance does not satisfy the document classification.");
    }
    if (!attributeMatches(actorAttributes.get("businessUnit"), document.getBusinessUnit())) {
      return GovernanceDecision.deny("Document business-unit scope does not match the user context.");
    }
    if (!attributeMatches(actorAttributes.get("department"), document.getDepartment())) {
      return GovernanceDecision.deny("Document department scope does not match the user context.");
    }
    if (!attributeMatches(actorAttributes.get("region"), document.getRegion())) {
      return GovernanceDecision.deny("Document region scope does not match the user context.");
    }
    if (!attributeMatches(actorAttributes.get("country"), document.getCountry())) {
      return GovernanceDecision.deny("Document country scope does not match the user context.");
    }
    if (!attributeMatches(actorAttributes.get("brokerOffice"), document.getBrokerOffice())) {
      return GovernanceDecision.deny("Document broker-office scope does not match the user context.");
    }
    if (action == GovernanceAction.EXPORT
        && document.isExportRestricted()
        && !permissions.contains(Permission.EXPORT_SENSITIVE_CONTENT)) {
      return GovernanceDecision.deny("Sensitive export permission is required for this document.");
    }
    return GovernanceDecision.allow();
  }

  private static boolean attributeMatches(String actorValue, String resourceValue) {
    if (resourceValue == null || resourceValue.isBlank()) {
      return true;
    }
    if (actorValue == null || actorValue.isBlank()) {
      return false;
    }
    return actorValue.trim().equalsIgnoreCase(resourceValue.trim());
  }

  public record GovernanceDecision(boolean allowed, String reason) {
    static GovernanceDecision allow() {
      return new GovernanceDecision(true, "Allowed");
    }

    static GovernanceDecision deny(String reason) {
      return new GovernanceDecision(false, reason);
    }
  }

  public enum GovernanceAction {
    SEARCH,
    AI,
    PREVIEW,
    DOWNLOAD,
    EXPORT;

    public static GovernanceAction forDocumentAccess(boolean preview) {
      return preview ? PREVIEW : DOWNLOAD;
    }

    public String auditLabel() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
