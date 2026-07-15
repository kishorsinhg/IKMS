package com.ikms.ai.orchestration;

import com.ikms.search.ClientSearchService;
import com.ikms.search.SearchQueryContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RetrievalCoordinator {

  private final ClientSearchService clientSearchService;

  public RetrievalCoordinator(ClientSearchService clientSearchService) {
    this.clientSearchService = clientSearchService;
  }

  public RetrievalResult retrieve(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryPlan plan) {
    ClientSearchService.SearchOutcome outcome = clientSearchService.searchDetailed(
        SearchQueryContext.enterprise(request, plan));

    List<EnterpriseAiContracts.RetrievedEvidence> evidence = outcome.results().stream()
        .limit(plan.maxEvidenceItems())
        .map(result -> new EnterpriseAiContracts.RetrievedEvidence(
            result.sourceType(),
            result.sourceId(),
            result.title(),
            result.excerpt(),
            result.citation(),
            result.pageNumber(),
            result.sourceSection(),
            result.retrievalPath(),
            result.citationQuality(),
            result.occurredAt()))
        .toList();

    return new RetrievalResult(evidence, outcome.retrievalMode(), outcome.warnings());
  }

  public record RetrievalResult(
      List<EnterpriseAiContracts.RetrievedEvidence> evidence,
      String retrievalMode,
      List<String> warnings) {
  }
}
