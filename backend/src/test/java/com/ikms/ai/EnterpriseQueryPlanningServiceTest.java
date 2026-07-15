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

class EnterpriseQueryPlanningServiceTest {

  private final IntentDetectionService intentDetectionService = new IntentDetectionService();
  private final QueryPlanningService queryPlanningService =
      new QueryPlanningService(new BusinessReferenceExtractionService());

  @Test
  void planShouldExtractBusinessReferenceFieldsFromPrompt() {
    EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        EnterpriseAiOperation.ASK,
        "Show correspondence for claim number CLM-9988 from insurer Northwind Mutual",
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        Set.of(),
        null,
        List.of(),
        Map.of());

    EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(request, intentDetectionService.detect(request));

    assertThat(plan.scope()).isEqualTo(EnterpriseAiContracts.QueryScope.CUSTOMER);
    assertThat(plan.businessReferenceFields().claimNumber()).isEqualTo("CLM-9988");
    assertThat(plan.businessReferenceFields().insurer()).contains("Northwind Mutual");
    assertThat(plan.sourceTypes()).contains(EnterpriseAiContracts.SourceType.EMAIL, EnterpriseAiContracts.SourceType.DOCUMENT);
    assertThat(plan.sortOrder()).isEqualTo(EnterpriseAiContracts.SortOrder.RELEVANCE);
  }

  @Test
  void planShouldPreferPreviousVersionForComparisonRequests() {
    EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        EnterpriseAiOperation.COMPARE,
        "Compare this document with the previous version for policy POL-12345",
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        Set.of(),
        null,
        List.of(UUID.fromString("22222222-2222-2222-2222-222222222222")),
        Map.of());

    EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(request, intentDetectionService.detect(request));

    assertThat(plan.scope()).isEqualTo(EnterpriseAiContracts.QueryScope.DOCUMENT_VERSION);
    assertThat(plan.versionPreference()).isEqualTo(EnterpriseAiContracts.VersionPreference.PREVIOUS_VERSION);
    assertThat(plan.requiredEvidenceGranularity()).isEqualTo(EnterpriseAiContracts.EvidenceGranularity.DOCUMENT);
    assertThat(plan.businessReferenceFields().policyNumber()).isEqualTo("POL-12345");
    assertThat(plan.retrievalModes()).contains("VERSION", "RELATIONSHIP");
  }

  @Test
  void planShouldHonorStructuredParameterOverridesAndDateRange() {
    EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
        null,
        EnterpriseAiOperation.SEARCH,
        "Find the latest insurer correspondence",
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        Set.of(),
        null,
        List.of(),
        Map.of(
            "policyNumber", "POL-22222",
            "claimNumber", "CLM-77777",
            "dateFrom", "2026-01-01",
            "dateTo", "2026-07-15",
            "insurer", "Blue Tide"));

    EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(request, intentDetectionService.detect(request));

    assertThat(plan.scope()).isEqualTo(EnterpriseAiContracts.QueryScope.GLOBAL);
    assertThat(plan.businessReferenceFields().policyNumber()).isEqualTo("POL-22222");
    assertThat(plan.businessReferenceFields().claimNumber()).isEqualTo("CLM-77777");
    assertThat(plan.businessReferenceFields().insurer()).isEqualTo("Blue Tide");
    assertThat(plan.dateRange().from()).isEqualTo("2026-01-01");
    assertThat(plan.dateRange().to()).isEqualTo("2026-07-15");
    assertThat(plan.sortOrder()).isEqualTo(EnterpriseAiContracts.SortOrder.NEWEST_FIRST);
    assertThat(plan.resultLimit()).isEqualTo(20);
  }
}
