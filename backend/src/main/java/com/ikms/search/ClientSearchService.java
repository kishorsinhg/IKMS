package com.ikms.search;

import com.ikms.security.SecurityTrimService;
import com.ikms.security.GovernanceAccessService;
import com.ikms.security.domain.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientSearchService {

  private final LexicalSearchRetriever lexicalSearchRetriever;
  private final MetadataSearchRetriever metadataSearchRetriever;
  private final VectorSearchRetriever vectorSearchRetriever;
  private final RelationshipSearchRetriever relationshipSearchRetriever;
  private final EvidenceRankingService evidenceRankingService;
  private final SecurityTrimService securityTrimService;
  private final com.ikms.document.DocumentRepository documentRepository;
  private final GovernanceAccessService governanceAccessService;

  public ClientSearchService(
      LexicalSearchRetriever lexicalSearchRetriever,
      MetadataSearchRetriever metadataSearchRetriever,
      VectorSearchRetriever vectorSearchRetriever,
      RelationshipSearchRetriever relationshipSearchRetriever,
      EvidenceRankingService evidenceRankingService,
      SecurityTrimService securityTrimService,
      com.ikms.document.DocumentRepository documentRepository,
      GovernanceAccessService governanceAccessService) {
    this.lexicalSearchRetriever = lexicalSearchRetriever;
    this.metadataSearchRetriever = metadataSearchRetriever;
    this.vectorSearchRetriever = vectorSearchRetriever;
    this.relationshipSearchRetriever = relationshipSearchRetriever;
    this.evidenceRankingService = evidenceRankingService;
    this.securityTrimService = securityTrimService;
    this.documentRepository = documentRepository;
    this.governanceAccessService = governanceAccessService;
  }

  public List<SearchContracts.SearchResultResponse> search(UUID clientId, String query, Set<Permission> permissions) {
    return searchDetailed(clientId, query, permissions).results();
  }

  public SearchOutcome searchDetailed(UUID clientId, String query, Set<Permission> permissions) {
    return searchDetailed(SearchQueryContext.simple(clientId, query, permissions));
  }

  public SearchOutcome searchDetailed(UUID clientId, String query, Set<Permission> permissions, Map<String, String> actorAttributes) {
    return searchDetailed(SearchQueryContext.simple(clientId, query, permissions, actorAttributes));
  }

  public SearchOutcome searchDetailed(SearchQueryContext context) {
    String normalizedQuery = context.normalizedQuery();
    List<String> warnings = new ArrayList<>();
    List<SearchEvidenceCandidate> rawCandidates = new ArrayList<>();
    rawCandidates.addAll(lexicalSearchRetriever.retrieve(context));
    rawCandidates.addAll(metadataSearchRetriever.retrieve(context));
    var vectorExecution = vectorSearchRetriever.retrieveWithOutcome(context);
    if (vectorExecution.outcome().warning() != null && !vectorExecution.outcome().warning().isBlank()) {
      warnings.add(vectorExecution.outcome().warning());
    }
    rawCandidates.addAll(vectorExecution.candidates());
    rawCandidates.addAll(relationshipSearchRetriever.retrieve(context));

    Map<UUID, com.ikms.document.Document> documentCache = new HashMap<>();
    List<SearchContracts.SearchResultResponse> results = evidenceRankingService.rank(rawCandidates).stream()
        .filter(candidate -> isGovernanceAllowed(context, candidate, documentCache, warnings))
        .limit(context.resultLimit())
        .map(candidate -> new SearchContracts.SearchResultResponse(
            candidate.sourceType(),
            candidate.sourceId(),
            candidate.title(),
            securityTrimService.trimSearchResult(
                context.permissions(),
                candidate.excerpt() == null || candidate.excerpt().isBlank()
                    ? SearchSupport.excerpt(null, candidate.fallbackText(), candidate.title(), normalizedQuery)
                    : candidate.excerpt(),
                candidate.containsPii()),
            candidate.citation() == null || candidate.citation().isBlank() ? candidate.title() : candidate.citation(),
            candidate.pageNumber(),
            candidate.sourceSection(),
            candidate.retrievalPath(),
            SearchSupport.citationQuality(candidate.sourceType(), candidate.pageNumber(), candidate.sourceSection()),
            candidate.occurredAt()))
        .toList();
    if (results.stream().anyMatch(result -> "LOW".equals(result.citationQuality()))) {
      warnings.add("Some retrieved evidence has limited location metadata and may produce weaker citations.");
    }
    String retrievalMode = context.browsing()
        ? "BROWSE"
        : results.stream().anyMatch(result -> "HYBRID_VECTOR".equals(result.retrievalPath()) || "VECTOR_HYBRID".equals(result.retrievalPath()))
            ? "HYBRID_VECTOR"
            : results.stream().anyMatch(result -> "METADATA".equals(result.retrievalPath()) || "RELATIONSHIP_METADATA".equals(result.retrievalPath()))
                ? "METADATA_PLUS_VECTOR"
                : "KEYWORD_FALLBACK";
    return new SearchOutcome(results, retrievalMode, List.copyOf(warnings));
  }

  private boolean isGovernanceAllowed(
      SearchQueryContext context,
      SearchEvidenceCandidate candidate,
      Map<UUID, com.ikms.document.Document> documentCache,
      List<String> warnings) {
    if (!"DOCUMENT".equalsIgnoreCase(candidate.sourceType())
        && !"DOCUMENT_VERSION".equalsIgnoreCase(candidate.sourceType())) {
      return true;
    }
    com.ikms.document.Document document = documentCache.computeIfAbsent(
        candidate.sourceId(),
        sourceId -> documentRepository.findById(sourceId).orElse(null));
    GovernanceAccessService.GovernanceDecision decision = governanceAccessService.evaluate(
        context.permissions(),
        context.actorAttributes(),
        document,
        GovernanceAccessService.GovernanceAction.SEARCH);
    if (!decision.allowed()) {
      warnings.add("Restricted knowledge was excluded from search results by governance policy.");
      return false;
    }
    return true;
  }

  public record SearchOutcome(
      List<SearchContracts.SearchResultResponse> results,
      String retrievalMode,
      List<String> warnings) {
  }
}
