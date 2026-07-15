package com.ikms.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikms.document.DocumentRepository;
import com.ikms.security.GovernanceAccessService;
import com.ikms.security.SecurityTrimService;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientSearchServiceOptimizationTest {

  @Test
  void searchShouldExecuteVectorRetrievalOncePerRequest() {
    LexicalSearchRetriever lexical = mock(LexicalSearchRetriever.class);
    MetadataSearchRetriever metadata = mock(MetadataSearchRetriever.class);
    VectorSearchRetriever vector = mock(VectorSearchRetriever.class);
    RelationshipSearchRetriever relationship = mock(RelationshipSearchRetriever.class);
    EvidenceRankingService rankingService = new EvidenceRankingService();
    SecurityTrimService trimService = mock(SecurityTrimService.class);
    DocumentRepository documentRepository = mock(DocumentRepository.class);
    GovernanceAccessService governanceAccessService = new GovernanceAccessService();
    when(lexical.retrieve(any())).thenReturn(List.of());
    when(metadata.retrieve(any())).thenReturn(List.of());
    when(relationship.retrieve(any())).thenReturn(List.of());
    when(trimService.trimSearchResult(any(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(1));

    SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("DOCUMENT", UUID.randomUUID());
    candidate.setTitle("Renewal Schedule");
    candidate.setExcerpt("Renewal due next month.");
    candidate.setCitation("Renewal Schedule");
    candidate.setPageNumber(2);
    candidate.setSourceSection("document-version");
    candidate.setOccurredAt(Instant.parse("2026-07-15T00:00:00Z"));
    candidate.addVectorScore(2.1d);
    candidate.retrievalSignals().add("VECTOR");

    when(vector.retrieveWithOutcome(any())).thenReturn(new VectorSearchRetriever.VectorRetrievalExecution(
        List.of(candidate),
        new VectorSearchRetriever.VectorRetrievalOutcome(List.of(), "HYBRID_VECTOR", null)));

    ClientSearchService service = new ClientSearchService(
        lexical,
        metadata,
        vector,
        relationship,
        rankingService,
        trimService,
        documentRepository,
        governanceAccessService);
    ClientSearchService.SearchOutcome outcome = service.searchDetailed(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "renewal",
        Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE));

    verify(vector).retrieveWithOutcome(any());
    verify(vector, never()).retrieve(any());
    assertThat(outcome.results()).hasSize(1);
    assertThat(outcome.retrievalMode()).isEqualTo("HYBRID_VECTOR");
  }
}
