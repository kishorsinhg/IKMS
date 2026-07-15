package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.ai.orchestration.BusinessReferenceExtractionService;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import com.ikms.ai.orchestration.IntentDetectionService;
import com.ikms.ai.orchestration.QueryPlanningService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnterpriseQueryPlanningValidationTest {

  private final IntentDetectionService intentDetectionService = new IntentDetectionService();
  private final QueryPlanningService queryPlanningService = new QueryPlanningService(new BusinessReferenceExtractionService());

  @Test
  void plansShouldCoverCustomerGlobalAndDocumentVersionScopes() {
    List<Scenario> scenarios = List.of(
        new Scenario(request(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            EnterpriseAiOperation.ASK,
            "Summarize this customer and insurer correspondence for policy number POL-12345",
            Map.of()),
            EnterpriseAiContracts.QueryScope.CUSTOMER,
            EnterpriseAiContracts.SortOrder.RELEVANCE,
            EnterpriseAiContracts.VersionPreference.CURRENT_VERSION,
            EnterpriseAiContracts.EvidenceGranularity.DOCUMENT,
            20),
        new Scenario(request(
            null,
            EnterpriseAiOperation.SEARCH,
            "Find the latest correspondence for claim number CLM-9988",
            Map.of("dateFrom", "2026-01-01", "dateTo", "2026-07-15")),
            EnterpriseAiContracts.QueryScope.GLOBAL,
            EnterpriseAiContracts.SortOrder.NEWEST_FIRST,
            EnterpriseAiContracts.VersionPreference.CURRENT_VERSION,
            EnterpriseAiContracts.EvidenceGranularity.DOCUMENT,
            20),
        new Scenario(request(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            EnterpriseAiOperation.COMPARE,
            "Compare this document with the previous version for policy POL-12345",
            Map.of()),
            EnterpriseAiContracts.QueryScope.DOCUMENT_VERSION,
            EnterpriseAiContracts.SortOrder.RELEVANCE,
            EnterpriseAiContracts.VersionPreference.PREVIOUS_VERSION,
            EnterpriseAiContracts.EvidenceGranularity.DOCUMENT,
            10),
        new Scenario(request(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            EnterpriseAiOperation.VALIDATE,
            "Validate extracted fields for claim CLM-55555 on page 2",
            Map.of()),
            EnterpriseAiContracts.QueryScope.CUSTOMER,
            EnterpriseAiContracts.SortOrder.RELEVANCE,
            EnterpriseAiContracts.VersionPreference.CURRENT_VERSION,
            EnterpriseAiContracts.EvidenceGranularity.PAGE,
            20));

    for (Scenario scenario : scenarios) {
      EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(
          scenario.request(),
          intentDetectionService.detect(scenario.request()));

      assertThat(plan.scope()).isEqualTo(scenario.scope());
      assertThat(plan.sortOrder()).isEqualTo(scenario.sortOrder());
      assertThat(plan.versionPreference()).isEqualTo(scenario.versionPreference());
      assertThat(plan.requiredEvidenceGranularity()).isEqualTo(scenario.evidenceGranularity());
      assertThat(plan.resultLimit()).isEqualTo(scenario.resultLimit());
      assertThat(plan.businessReferenceFields().hasValues()).isTrue();
      assertThat(plan.retrievalModes()).isNotEmpty();
    }
  }

  @Test
  void plansShouldCarrySourceIdsAndConversationContextInputsForward() {
    UUID conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    UUID sourceId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        EnterpriseAiOperation.EXPLAIN,
        "Explain why review is required for policy number POL-10000",
        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
        Set.of(),
        conversationId,
        List.of(sourceId),
        Map.of("stream", true));

    EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(request, intentDetectionService.detect(request));

    assertThat(plan.sourceIds()).containsExactly(sourceId);
    assertThat(request.conversationId()).isEqualTo(conversationId);
    assertThat(plan.sourceTypes()).contains(EnterpriseAiContracts.SourceType.DOCUMENT);
  }

  private static EnterpriseAiContracts.EnterpriseAiRequest request(
      UUID clientId,
      EnterpriseAiOperation operation,
      String prompt,
      Map<String, Object> parameters) {
    return new EnterpriseAiContracts.EnterpriseAiRequest(
        clientId,
        operation,
        prompt,
        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
        Set.of(),
        null,
        List.of(),
        parameters);
  }

  private record Scenario(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryScope scope,
      EnterpriseAiContracts.SortOrder sortOrder,
      EnterpriseAiContracts.VersionPreference versionPreference,
      EnterpriseAiContracts.EvidenceGranularity evidenceGranularity,
      int resultLimit) {
  }
}
