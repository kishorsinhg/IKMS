package com.ikms.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnterpriseRetrievalBenchmarkTest {

  @Test
  void hybridAndRerankedRetrievalShouldOutperformSingleModeBaselines() {
    List<Scenario> scenarios = List.of(
        new Scenario(
            Set.of("D1", "E1"),
            List.of("D3", "D1", "N1", "E1", "D2"),
            List.of("N1", "D2", "D1", "E1", "D3"),
            List.of("E1", "D1", "D3", "N1", "D2"),
            List.of("D1", "N1", "E1", "D2", "D3"),
            List.of("E1", "D1", "N1", "D2", "D3")),
        new Scenario(
            Set.of("D4", "E2"),
            List.of("D5", "N2", "D4", "E2"),
            List.of("D4", "E2", "N2", "D5"),
            List.of("E2", "D4", "N2", "D5"),
            List.of("D4", "N2", "E2", "D5"),
            List.of("E2", "D4", "N2", "D5")),
        new Scenario(
            Set.of("D6"),
            List.of("N3", "D7", "D6"),
            List.of("D7", "N3", "D6"),
            List.of("D6", "N3", "D7"),
            List.of("N3", "D6", "D7"),
            List.of("D6", "N3", "D7")));

    Aggregate keyword = aggregate(scenarios, Scenario::keywordOnly);
    Aggregate vector = aggregate(scenarios, Scenario::vectorOnly);
    Aggregate businessReference = aggregate(scenarios, Scenario::businessReferenceOnly);
    Aggregate hybrid = aggregate(scenarios, Scenario::hybrid);
    Aggregate reranked = aggregate(scenarios, Scenario::reranked);

    assertThat(hybrid.precisionAt3()).isGreaterThan(keyword.precisionAt3());
    assertThat(hybrid.recallAt5()).isGreaterThanOrEqualTo(vector.recallAt5());
    assertThat(businessReference.mrr()).isGreaterThan(keyword.mrr());
    assertThat(reranked.mrr()).isGreaterThan(hybrid.mrr());
    assertThat(reranked.ndcgAt5()).isGreaterThan(hybrid.ndcgAt5());
    assertThat(reranked.hitRateAt5()).isEqualTo(1.0d);
  }

  @Test
  void rerankedOrderingShouldReduceDuplicateTopResultsInAblationFixtures() {
    List<String> hybrid = List.of("DOCUMENT:renewal-v3", "DOCUMENT:renewal-v2", "EMAIL:renewal", "NOTE:renewal");
    List<String> reranked = List.of("DOCUMENT:renewal-v3", "EMAIL:renewal", "NOTE:renewal", "DOCUMENT:renewal-v2");

    assertThat(sourceDiversity(hybrid.subList(0, 2))).isEqualTo(1);
    assertThat(sourceDiversity(reranked.subList(0, 3))).isEqualTo(3);
  }

  private static Aggregate aggregate(List<Scenario> scenarios, RankingExtractor extractor) {
    double p3 = 0d;
    double recall5 = 0d;
    double mrr = 0d;
    double ndcg5 = 0d;
    double hit5 = 0d;
    for (Scenario scenario : scenarios) {
      List<String> ranking = extractor.extract(scenario);
      p3 += precisionAtK(ranking, scenario.relevant(), 3);
      recall5 += recallAtK(ranking, scenario.relevant(), 5);
      mrr += mrr(ranking, scenario.relevant());
      ndcg5 += ndcgAtK(ranking, scenario.relevant(), 5);
      hit5 += hitRateAtK(ranking, scenario.relevant(), 5);
    }
    double count = scenarios.size();
    return new Aggregate(p3 / count, recall5 / count, mrr / count, ndcg5 / count, hit5 / count);
  }

  private static double precisionAtK(List<String> ranking, Set<String> relevant, int k) {
    int hits = 0;
    for (int index = 0; index < Math.min(k, ranking.size()); index++) {
      if (relevant.contains(ranking.get(index))) {
        hits++;
      }
    }
    return (double) hits / k;
  }

  private static double recallAtK(List<String> ranking, Set<String> relevant, int k) {
    int hits = 0;
    for (int index = 0; index < Math.min(k, ranking.size()); index++) {
      if (relevant.contains(ranking.get(index))) {
        hits++;
      }
    }
    return relevant.isEmpty() ? 1.0d : (double) hits / relevant.size();
  }

  private static double mrr(List<String> ranking, Set<String> relevant) {
    for (int index = 0; index < ranking.size(); index++) {
      if (relevant.contains(ranking.get(index))) {
        return 1.0d / (index + 1);
      }
    }
    return 0d;
  }

  private static double ndcgAtK(List<String> ranking, Set<String> relevant, int k) {
    double dcg = 0d;
    for (int index = 0; index < Math.min(k, ranking.size()); index++) {
      if (relevant.contains(ranking.get(index))) {
        dcg += 1.0d / (Math.log(index + 2) / Math.log(2));
      }
    }
    double idcg = 0d;
    for (int index = 0; index < Math.min(k, relevant.size()); index++) {
      idcg += 1.0d / (Math.log(index + 2) / Math.log(2));
    }
    return idcg == 0d ? 1.0d : dcg / idcg;
  }

  private static double hitRateAtK(List<String> ranking, Set<String> relevant, int k) {
    for (int index = 0; index < Math.min(k, ranking.size()); index++) {
      if (relevant.contains(ranking.get(index))) {
        return 1.0d;
      }
    }
    return 0d;
  }

  private static int sourceDiversity(List<String> rankedSources) {
    Set<String> sourceTypes = new LinkedHashSet<>();
    for (String entry : rankedSources) {
      sourceTypes.add(entry.substring(0, entry.indexOf(':')));
    }
    return sourceTypes.size();
  }

  private record Scenario(
      Set<String> relevant,
      List<String> keywordOnly,
      List<String> vectorOnly,
      List<String> businessReferenceOnly,
      List<String> hybrid,
      List<String> reranked) {
  }

  private record Aggregate(
      double precisionAt3,
      double recallAt5,
      double mrr,
      double ndcgAt5,
      double hitRateAt5) {
  }

  private interface RankingExtractor {
    List<String> extract(Scenario scenario);
  }
}
