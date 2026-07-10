package com.ikms.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class V1SlaValidationTest {

  private V1SlaPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new V1SlaPolicy();
  }

  @Test
  void clientProfileOpenShouldMeetTenSecondTarget() {
    assertThat(policy.meetsClientProfileOpenSla(Duration.ofSeconds(8))).isTrue();
    assertThat(policy.meetsClientProfileOpenSla(Duration.ofSeconds(11))).isFalse();
  }

  @Test
  void intakeRoutingShouldMeetNinetyFivePercentTarget() {
    assertThat(policy.meetsRoutingSuccessRate(95, 100)).isTrue();
    assertThat(policy.meetsRoutingSuccessRate(94, 100)).isFalse();
  }
}
