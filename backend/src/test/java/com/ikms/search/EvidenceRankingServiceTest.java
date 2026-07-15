package com.ikms.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceRankingServiceTest {

  private final EvidenceRankingService rankingService = new EvidenceRankingService();

  @Test
  void rankShouldFavorHybridEvidenceCoverage() {
    SearchEvidenceCandidate hybrid = candidate("DOCUMENT", "Renewal Schedule", Instant.parse("2026-07-14T10:00:00Z"));
    hybrid.addLexicalScore(2.5d);
    hybrid.addVectorScore(2.0d);
    hybrid.retrievalSignals().add("LEXICAL");
    hybrid.retrievalSignals().add("VECTOR");

    SearchEvidenceCandidate lexicalOnly = candidate("DOCUMENT", "Carrier Letter", Instant.parse("2026-07-15T10:00:00Z"));
    lexicalOnly.addLexicalScore(3.4d);
    lexicalOnly.retrievalSignals().add("LEXICAL");

    List<SearchEvidenceCandidate> ranked = rankingService.rank(List.of(lexicalOnly, hybrid));

    assertThat(ranked.getFirst().title()).isEqualTo("Renewal Schedule");
  }

  @Test
  void rankShouldPromoteSourceDiversityAheadOfNearDuplicateDocuments() {
    SearchEvidenceCandidate firstDocument = candidate("DOCUMENT", "Policy Schedule", Instant.parse("2026-07-15T10:00:00Z"));
    firstDocument.addVectorScore(2.0d);
    firstDocument.retrievalSignals().add("VECTOR");

    SearchEvidenceCandidate secondDocument = candidate("DOCUMENT", "Policy Schedule", Instant.parse("2026-07-14T10:00:00Z"));
    secondDocument.addVectorScore(1.9d);
    secondDocument.retrievalSignals().add("VECTOR");

    SearchEvidenceCandidate email = candidate("EMAIL", "Insurer Correspondence", Instant.parse("2026-07-13T10:00:00Z"));
    email.addVectorScore(1.8d);
    email.addMetadataScore(1.2d);
    email.retrievalSignals().add("VECTOR");
    email.retrievalSignals().add("METADATA");

    List<SearchEvidenceCandidate> ranked = rankingService.rank(List.of(firstDocument, secondDocument, email));

    assertThat(ranked.subList(0, 2).stream().map(SearchEvidenceCandidate::sourceType).distinct())
        .containsExactlyInAnyOrder("DOCUMENT", "EMAIL");
  }

  private static SearchEvidenceCandidate candidate(String sourceType, String title, Instant occurredAt) {
    SearchEvidenceCandidate candidate = new SearchEvidenceCandidate(sourceType, UUID.randomUUID());
    candidate.setTitle(title);
    candidate.setExcerpt(title);
    candidate.setCitation(title);
    candidate.setOccurredAt(occurredAt);
    return candidate;
  }
}
