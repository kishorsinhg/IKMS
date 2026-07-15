package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.GroundingValidationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnterpriseAiEvaluationTest {

  private final GroundingValidationService groundingValidationService = new GroundingValidationService();

  @Test
  void groundingScoreShouldStayHighWhenAllCitationsHaveStrongProvenance() {
    var result = groundingValidationService.validate(List.of(
        citation("HIGH", 4, "document-version"),
        citation("HIGH", 6, "coverage-summary")),
        List.of());

    assertThat(result.grounded()).isTrue();
    assertThat(result.groundingScore()).isEqualTo(0.98d);
    assertThat(result.citationCoverage()).isEqualTo(1.0d);
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void groundingScoreShouldDropWhenLowConfidenceEvidenceIsPresent() {
    var result = groundingValidationService.validate(List.of(
        citation("HIGH", 4, "document-version"),
        citation("LOW", null, "freeform-note")),
        List.of("Provider fallback was used."));

    assertThat(result.grounded()).isTrue();
    assertThat(result.groundingScore()).isEqualTo(0.75d);
    assertThat(result.citationCoverage()).isEqualTo(1.0d);
    assertThat(result.warnings()).contains("Provider fallback was used.");
    assertThat(result.warnings()).contains("One or more citations rely on low-confidence evidence and require manual review.");
  }

  @Test
  void groundingShouldFailWhenInsufficientEvidenceWarningIsPresent() {
    var result = groundingValidationService.validate(
        List.of(citation("HIGH", 4, "document-version")),
        List.of("Insufficient evidence to answer."));

    assertThat(result.grounded()).isFalse();
    assertThat(result.groundingScore()).isEqualTo(0d);
    assertThat(result.citationCoverage()).isEqualTo(0d);
  }

  @Test
  void evaluationSnapshotShouldFlagLatencyAndCitationCoverageThresholds() {
    EvaluationSnapshot healthy = new EvaluationSnapshot(840L, 0.98d, 1.0d, 0.93d, false);
    EvaluationSnapshot degraded = new EvaluationSnapshot(2480L, 0.72d, 0.50d, 0.61d, true);

    assertThat(healthy.withinLatencyBudget()).isTrue();
    assertThat(healthy.hasGroundingCoverage()).isTrue();
    assertThat(healthy.answerQualityAcceptable()).isTrue();

    assertThat(degraded.withinLatencyBudget()).isFalse();
    assertThat(degraded.hasGroundingCoverage()).isFalse();
    assertThat(degraded.answerQualityAcceptable()).isFalse();
    assertThat(degraded.requiresManualReview()).isTrue();
  }

  private static EnterpriseAiContracts.CitationReference citation(
      String confidence,
      Integer pageNumber,
      String section) {
    return new EnterpriseAiContracts.CitationReference(
        "DOCUMENT",
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        "Policy Schedule",
        "renewal due next month",
        pageNumber,
        0,
        section,
        confidence,
        pageNumber == null
            ? "metadata:DOCUMENT:22222222-2222-2222-2222-222222222222:" + section
            : "document:22222222-2222-2222-2222-222222222222:page:" + pageNumber,
        "HYBRID_VECTOR");
  }

  private record EvaluationSnapshot(
      long totalLatencyMs,
      double groundingScore,
      double citationCoverage,
      double answerQualityScore,
      boolean fallbackUsed) {

    private static final long LATENCY_BUDGET_MS = 1500L;
    private static final double MIN_GROUNDING_SCORE = 0.85d;
    private static final double MIN_CITATION_COVERAGE = 0.90d;
    private static final double MIN_ANSWER_QUALITY_SCORE = 0.80d;

    boolean withinLatencyBudget() {
      return totalLatencyMs <= LATENCY_BUDGET_MS;
    }

    boolean hasGroundingCoverage() {
      return groundingScore >= MIN_GROUNDING_SCORE && citationCoverage >= MIN_CITATION_COVERAGE;
    }

    boolean answerQualityAcceptable() {
      return answerQualityScore >= MIN_ANSWER_QUALITY_SCORE && !fallbackUsed;
    }

    boolean requiresManualReview() {
      return !withinLatencyBudget() || !hasGroundingCoverage() || !answerQualityAcceptable();
    }
  }
}
