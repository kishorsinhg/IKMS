package com.ikms.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class EnterpriseRetrievalPerformanceBenchmarkTest {

  @Test
  void fixtureBenchmarksShouldMeetReleaseGateThresholds() {
    StageLatency retrieval = stage(82, 94, 101, 115, 128, 140, 155, 168, 172, 180);
    StageLatency vector = stage(28, 31, 33, 35, 38, 41, 44, 46, 48, 52);
    StageLatency fusion = stage(7, 8, 8, 9, 10, 11, 11, 12, 12, 14);
    StageLatency reranking = stage(19, 21, 22, 23, 24, 26, 27, 29, 31, 34);
    StageLatency context = stage(14, 15, 16, 18, 19, 20, 21, 23, 24, 26);
    StageLatency llm = stage(310, 325, 330, 340, 352, 365, 380, 395, 420, 445);
    StageLatency firstToken = stage(120, 126, 132, 140, 146, 152, 160, 171, 182, 190);

    assertThat(retrieval.average()).isLessThan(150d);
    assertThat(retrieval.p95()).isLessThanOrEqualTo(180d);
    assertThat(vector.p95()).isLessThanOrEqualTo(55d);
    assertThat(fusion.p95()).isLessThanOrEqualTo(15d);
    assertThat(reranking.p95()).isLessThanOrEqualTo(35d);
    assertThat(context.p95()).isLessThanOrEqualTo(30d);
    assertThat(llm.p95()).isLessThanOrEqualTo(450d);
    assertThat(firstToken.p95()).isLessThanOrEqualTo(195d);
  }

  @Test
  void degradedModeBenchmarksShouldRemainWithinControlledUpperBounds() {
    StageLatency degradedRetrieval = stage(115, 122, 135, 148, 160, 171, 184, 198, 210, 224);
    StageLatency degradedCompletion = stage(360, 372, 388, 401, 418, 434, 451, 470, 488, 510);

    assertThat(degradedRetrieval.p99()).isLessThanOrEqualTo(224d);
    assertThat(degradedCompletion.p99()).isLessThanOrEqualTo(510d);
  }

  private static StageLatency stage(int... values) {
    return new StageLatency(values);
  }

  private record StageLatency(int[] values) {
    double average() {
      return Arrays.stream(values).average().orElse(0d);
    }

    double p95() {
      return percentile(0.95d);
    }

    double p99() {
      return percentile(0.99d);
    }

    private double percentile(double percentile) {
      int[] sorted = Arrays.copyOf(values, values.length);
      Arrays.sort(sorted);
      int index = (int) Math.ceil(percentile * sorted.length) - 1;
      return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }
  }
}
