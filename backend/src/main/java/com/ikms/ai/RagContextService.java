package com.ikms.ai;

import com.ikms.search.ClientSearchService;
import com.ikms.search.SearchContracts;
import com.ikms.security.domain.Permission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RagContextService {

  private final ClientSearchService clientSearchService;

  public RagContextService(ClientSearchService clientSearchService) {
    this.clientSearchService = clientSearchService;
  }

  public List<SearchContracts.SearchResultResponse> buildContext(UUID clientId, String question, Set<Permission> permissions) {
    return buildContextDetailed(clientId, question, permissions).results();
  }

  public ClientSearchService.SearchOutcome buildContextDetailed(UUID clientId, String question, Set<Permission> permissions) {
    return buildContextDetailed(clientId, question, permissions, Map.of());
  }

  public ClientSearchService.SearchOutcome buildContextDetailed(
      UUID clientId,
      String question,
      Set<Permission> permissions,
      Map<String, String> actorAttributes) {
    ClientSearchService.SearchOutcome outcome = clientSearchService.searchDetailed(clientId, question, permissions, actorAttributes);
    return new ClientSearchService.SearchOutcome(
        outcome.results().stream()
        .limit(5)
        .toList(),
        outcome.retrievalMode(),
        outcome.warnings());
  }
}
