package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.security.domain.Permission;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityTrimBoundaryTest {

  private SecurityTrimService securityTrimService;

  @BeforeEach
  void setUp() {
    securityTrimService = new SecurityTrimService();
  }

  @Test
  void processorShouldReceiveRedactedPreviewWhenPiiAndRedactionAreAvailable() {
    SecurityTrimService.SecurityTrimDecision decision = securityTrimService.evaluateDocumentPreview(
        Set.of(Permission.VIEW_REDACTED_DOCUMENTS),
        true,
        true);

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.redactionRequired()).isTrue();
    assertThat(decision.piiPermitted()).isFalse();
  }

  @Test
  void processorShouldBeDeniedWhenPiiDocumentHasNoRedactedVariant() {
    SecurityTrimService.SecurityTrimDecision decision = securityTrimService.evaluateDocumentDownload(
        Set.of(Permission.VIEW_REDACTED_DOCUMENTS),
        true,
        false);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reason()).contains("no redacted content");
  }

  @Test
  void supervisorShouldBeAllowedOriginalAccessForPiiContent() {
    SecurityTrimService.SecurityTrimDecision decision = securityTrimService.evaluateDocumentPreview(
        Set.of(Permission.VIEW_ORIGINAL_DOCUMENTS, Permission.VIEW_PII),
        true,
        false);

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.redactionRequired()).isFalse();
    assertThat(decision.piiPermitted()).isTrue();
  }

  @Test
  void processorShouldBeDeniedPiiSearchAndAiContext() {
    SecurityTrimService.SecurityTrimDecision searchDecision = securityTrimService.evaluateSearchRetrieval(
        Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE),
        true);
    SecurityTrimService.SecurityTrimDecision aiDecision = securityTrimService.evaluateAiContextAssembly(
        Set.of(Permission.ASK_CLIENT_AI),
        true);

    assertThat(searchDecision.allowed()).isFalse();
    assertThat(aiDecision.allowed()).isFalse();
  }
}
