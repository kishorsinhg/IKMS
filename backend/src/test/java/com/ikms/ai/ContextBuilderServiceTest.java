package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.ai.context.ContextAssembly;
import com.ikms.ai.context.ContextBuilderService;
import com.ikms.ai.context.TokenBudgetManager;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import com.ikms.ai.orchestration.RetrievalCoordinator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextBuilderServiceTest {

  @Test
  void buildShouldDeduplicateEvidenceAndRespectEvidenceBudget() {
    AiProviderSettingsService settingsService = mock(AiProviderSettingsService.class);
    when(settingsService.current()).thenReturn(new AiProviderSettingsService.ProviderSettings(
        "remote-chat",
        "mistral-large",
        "mistral-embed",
        "http://localhost",
        "secret",
        "tesseract",
        true));
    AiConversationMessageRepository messageRepository = mock(AiConversationMessageRepository.class);
    when(messageRepository.findTop12ByConversationIdOrderByCreatedAtDesc(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")))
        .thenReturn(List.of());

    ContextBuilderService service = new ContextBuilderService(settingsService, messageRepository, new TokenBudgetManager());
    EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        EnterpriseAiOperation.ASK,
        "Summarize renewal evidence for policy number POL-12345",
        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        Set.of(),
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        List.of(),
        Map.of());
    EnterpriseAiContracts.QueryPlan plan = new EnterpriseAiContracts.QueryPlan(
        EnterpriseAiOperation.ASK,
        "summarize renewal evidence for policy number pol-12345",
        "answer",
        EnterpriseAiContracts.QueryScope.CUSTOMER,
        List.of("LEXICAL", "VECTOR"),
        List.of(EnterpriseAiContracts.SourceType.DOCUMENT, EnterpriseAiContracts.SourceType.EMAIL),
        List.of(),
        new EnterpriseAiContracts.QueryDateRange(null, null),
        new EnterpriseAiContracts.BusinessReferenceFields("POL-12345", null, null, null, null, null, null, null, null),
        5,
        2,
        1400,
        20,
        EnterpriseAiContracts.SortOrder.RELEVANCE,
        EnterpriseAiContracts.VersionPreference.CURRENT_VERSION,
        EnterpriseAiContracts.EvidenceGranularity.DOCUMENT,
        List.of());

    UUID documentSourceId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    EnterpriseAiContracts.RetrievedEvidence duplicateA = evidence(documentSourceId, "DOCUMENT", "Renewal Schedule", "Same excerpt", 4, "document-version");
    EnterpriseAiContracts.RetrievedEvidence duplicateB = evidence(documentSourceId, "DOCUMENT", "Renewal Schedule", "Same excerpt", 4, "document-version");
    EnterpriseAiContracts.RetrievedEvidence email = evidence(
        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
        "EMAIL",
        "Insurer email",
        "Carrier confirms renewal",
        null,
        "email");

    ContextAssembly assembly = service.build(
        request,
        plan,
        new RetrievalCoordinator.RetrievalResult(List.of(duplicateA, duplicateB, email), "HYBRID_VECTOR", List.of()),
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    assertThat(assembly.evidence()).hasSize(2);
    assertThat(assembly.evidence().stream().map(item -> item.title()).toList())
        .containsExactly("Renewal Schedule", "Insurer email");
    int evidenceTokens = assembly.evidence().stream().mapToInt(item -> item.estimatedTokens()).sum();
    assertThat(evidenceTokens).isLessThanOrEqualTo(assembly.tokenBudget().get("evidence"));
  }

  private static EnterpriseAiContracts.RetrievedEvidence evidence(
      UUID sourceId,
      String sourceType,
      String title,
      String excerpt,
      Integer page,
      String section) {
    return new EnterpriseAiContracts.RetrievedEvidence(
        sourceType,
        sourceId,
        title,
        excerpt,
        title,
        page,
        section,
        "HYBRID_VECTOR",
        "HIGH",
        Instant.parse("2026-07-15T00:00:00Z"));
  }
}
