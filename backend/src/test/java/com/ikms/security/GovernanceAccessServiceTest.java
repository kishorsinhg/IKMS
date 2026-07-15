package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.document.Document;
import com.ikms.governance.DocumentLifecycleState;
import com.ikms.governance.InformationClassification;
import com.ikms.security.domain.Permission;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GovernanceAccessServiceTest {

  private GovernanceAccessService governanceAccessService;

  @BeforeEach
  void setUp() {
    governanceAccessService = new GovernanceAccessService();
  }

  @Test
  void shouldDenyWhenClearanceIsBelowDocumentClassification() {
    Document document = new Document();
    document.setClassification(InformationClassification.RESTRICTED);

    GovernanceAccessService.GovernanceDecision decision = governanceAccessService.evaluate(
        Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE),
        Map.of("securityClearance", "CONFIDENTIAL"),
        document,
        GovernanceAccessService.GovernanceAction.SEARCH);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reason()).contains("Security clearance");
  }

  @Test
  void shouldDenyWhenDocumentAttributesDoNotMatchActorContext() {
    Document document = new Document();
    document.setClassification(InformationClassification.INTERNAL);
    document.setRegion("EMEA");

    GovernanceAccessService.GovernanceDecision decision = governanceAccessService.evaluate(
        Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE),
        Map.of("securityClearance", "INTERNAL", "region", "APAC"),
        document,
        GovernanceAccessService.GovernanceAction.SEARCH);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reason()).contains("region");
  }

  @Test
  void shouldAllowMatchingRestrictedDocumentExportForPrivilegedUser() {
    Document document = new Document();
    document.setClassification(InformationClassification.RESTRICTED);
    document.setExportRestricted(true);
    document.setLifecycleState(DocumentLifecycleState.ACTIVE);
    document.setBusinessUnit("Commercial");

    GovernanceAccessService.GovernanceDecision decision = governanceAccessService.evaluate(
        Set.of(Permission.EXPORT_SENSITIVE_CONTENT, Permission.VIEW_ORIGINAL_DOCUMENTS),
        Map.of("securityClearance", "HIGHLY_RESTRICTED", "businessUnit", "Commercial"),
        document,
        GovernanceAccessService.GovernanceAction.EXPORT);

    assertThat(decision.allowed()).isTrue();
  }
}
