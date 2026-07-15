package com.ikms.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Service;

@Service
class EvidenceRankingService {

  private static final double RRF_K = 60d;

  List<SearchEvidenceCandidate> rank(List<SearchEvidenceCandidate> candidates) {
    Map<String, SearchEvidenceCandidate> merged = new LinkedHashMap<>();
    for (SearchEvidenceCandidate candidate : candidates) {
      merged.merge(candidate.key(), candidate, this::merge);
    }
    List<SearchEvidenceCandidate> fused = new ArrayList<>(merged.values());
    Map<String, Double> fusedScores = buildFusionScores(fused);
    List<SearchEvidenceCandidate> prelim = fused.stream()
        .sorted(Comparator
            .comparing((SearchEvidenceCandidate candidate) -> fusedScores.getOrDefault(candidate.key(), 0d), Comparator.reverseOrder())
            .thenComparing(SearchEvidenceCandidate::finalScore, Comparator.reverseOrder()))
        .toList();

    List<SearchEvidenceCandidate> reranked = new ArrayList<>();
    Map<String, Integer> sourceTypeCounts = new HashMap<>();
    Map<String, Integer> titleCounts = new HashMap<>();
    List<SearchEvidenceCandidate> remaining = new ArrayList<>(prelim);
    while (!remaining.isEmpty()) {
      SearchEvidenceCandidate next = remaining.stream()
          .max(Comparator.comparing(candidate -> rerankScore(candidate, fusedScores, sourceTypeCounts, titleCounts)))
          .orElseThrow();
      reranked.add(next);
      sourceTypeCounts.merge(next.sourceType(), 1, Integer::sum);
      titleCounts.merge(normalizedTitle(next), 1, Integer::sum);
      remaining.remove(next);
    }
    return reranked;
  }

  private SearchEvidenceCandidate merge(SearchEvidenceCandidate left, SearchEvidenceCandidate right) {
    left.addLexicalScore(right.lexicalScore());
    left.addVectorScore(right.vectorScore());
    left.addMetadataScore(right.metadataScore());
    left.addRelationshipScore(right.relationshipScore());
    left.retrievalSignals().addAll(right.retrievalSignals());
    if ((left.excerpt() == null || left.excerpt().isBlank()) && right.excerpt() != null) {
      left.setExcerpt(right.excerpt());
    }
    if ((left.fallbackText() == null || left.fallbackText().isBlank()) && right.fallbackText() != null) {
      left.setFallbackText(right.fallbackText());
    }
    if ((left.citation() == null || left.citation().isBlank()) && right.citation() != null) {
      left.setCitation(right.citation());
    }
    if (left.pageNumber() == null && right.pageNumber() != null) {
      left.setPageNumber(right.pageNumber());
    }
    if ((left.sourceSection() == null || left.sourceSection().isBlank()) && right.sourceSection() != null) {
      left.setSourceSection(right.sourceSection());
    }
    if (left.occurredAt() == null || (right.occurredAt() != null && right.occurredAt().isAfter(left.occurredAt()))) {
      left.setOccurredAt(right.occurredAt());
    }
    left.setContainsPii(left.containsPii() || right.containsPii());
    return left;
  }

  private Map<String, Double> buildFusionScores(List<SearchEvidenceCandidate> candidates) {
    Map<String, Double> scores = new HashMap<>();
    addReciprocalRank(scores, candidates, SearchEvidenceCandidate::lexicalScore);
    addReciprocalRank(scores, candidates, SearchEvidenceCandidate::vectorScore);
    addReciprocalRank(scores, candidates, SearchEvidenceCandidate::metadataScore);
    addReciprocalRank(scores, candidates, SearchEvidenceCandidate::relationshipScore);
    for (SearchEvidenceCandidate candidate : candidates) {
      double coverageBonus = candidate.retrievalSignals().size() * 0.05d;
      double freshnessBonus = freshnessBonus(candidate);
      scores.merge(candidate.key(), coverageBonus + freshnessBonus, Double::sum);
    }
    return scores;
  }

  private void addReciprocalRank(
      Map<String, Double> scores,
      List<SearchEvidenceCandidate> candidates,
      ToDoubleFunction<SearchEvidenceCandidate> signal) {
    List<SearchEvidenceCandidate> ordered = candidates.stream()
        .filter(candidate -> signal.applyAsDouble(candidate) > 0d)
        .sorted(Comparator
            .comparingDouble((SearchEvidenceCandidate candidate) -> signal.applyAsDouble(candidate))
            .reversed()
            .thenComparing(SearchEvidenceCandidate::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    int rank = 1;
    for (SearchEvidenceCandidate candidate : ordered) {
      scores.merge(candidate.key(), 1d / (RRF_K + rank++), Double::sum);
    }
  }

  private double rerankScore(
      SearchEvidenceCandidate candidate,
      Map<String, Double> fusedScores,
      Map<String, Integer> sourceTypeCounts,
      Map<String, Integer> titleCounts) {
    double base = fusedScores.getOrDefault(candidate.key(), 0d) + candidate.finalScore();
    double sourceDiversityPenalty = sourceTypeCounts.getOrDefault(candidate.sourceType(), 0) * 0.20d;
    double titlePenalty = titleCounts.getOrDefault(normalizedTitle(candidate), 0) * 0.10d;
    double chunkLineageBonus = candidate.pageNumber() != null || (candidate.sourceSection() != null && !candidate.sourceSection().isBlank())
        ? 0.08d
        : 0d;
    return base + chunkLineageBonus - sourceDiversityPenalty - titlePenalty;
  }

  private static double freshnessBonus(SearchEvidenceCandidate candidate) {
    if (candidate.occurredAt() == null) {
      return 0d;
    }
    long ageMillis = Math.max(0L, java.time.Instant.now().toEpochMilli() - candidate.occurredAt().toEpochMilli());
    double ageDays = ageMillis / 86_400_000d;
    return Math.max(0d, 0.20d - Math.min(0.20d, ageDays * 0.01d));
  }

  private static String normalizedTitle(SearchEvidenceCandidate candidate) {
    return candidate.title() == null ? "" : candidate.title().trim().toLowerCase(java.util.Locale.ROOT);
  }
}
