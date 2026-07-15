package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnterpriseRetrievalEvaluationFixtureTest {

  @Test
  void businessReferenceFixtureShouldScoreAsAccurateWhenCustomerScopedEvidenceMatches() {
    RetrievalEvaluationFixture fixture = new RetrievalEvaluationFixture(
        "Find correspondence for claim CLM-9988",
        "CLM-9988",
        List.of("DOCUMENT", "EMAIL"),
        0.92d,
        0.95d,
        0.0d);

    assertThat(fixture.retrievalPrecision()).isGreaterThanOrEqualTo(0.90d);
    assertThat(fixture.citationAccuracy()).isGreaterThanOrEqualTo(0.90d);
    assertThat(fixture.permissionLeakageRate()).isZero();
  }

  @Test
  void permissionLeakageFixtureShouldFailEvaluationThresholds() {
    RetrievalEvaluationFixture fixture = new RetrievalEvaluationFixture(
        "Show restricted claim documents",
        "CLM-9988",
        List.of("DOCUMENT"),
        0.60d,
        0.70d,
        0.15d);

    assertThat(fixture.retrievalPrecision()).isLessThan(0.85d);
    assertThat(fixture.permissionLeakageRate()).isGreaterThan(0d);
  }

  private record RetrievalEvaluationFixture(
      String query,
      String businessReference,
      List<String> expectedSourceTypes,
      double retrievalPrecision,
      double citationAccuracy,
      double permissionLeakageRate) {
  }
}
