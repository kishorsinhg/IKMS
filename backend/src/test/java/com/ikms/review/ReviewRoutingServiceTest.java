package com.ikms.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.config.domain.ReviewSetting;
import com.ikms.config.domain.ReviewSettingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewRoutingServiceTest {

  @Test
  void confidenceModeRoutesLowConfidenceDocumentsToReview() {
    ReviewSettingRepository repository = mock(ReviewSettingRepository.class);
    ReviewSetting setting = new ReviewSetting();
    setting.setMode("confidence");
    setting.setLowConfidenceThreshold(0.75d);
    when(repository.findAll()).thenReturn(List.of(setting));

    ReviewRoutingService service = new ReviewRoutingService(repository);
    var decision = service.documentDecision(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        new BigDecimal("0.60"),
        new BigDecimal("0.90"),
        new BigDecimal("0.92"));

    assertThat(decision.requiresReview()).isTrue();
    assertThat(decision.reason()).isEqualTo(ReviewQueueReason.LOW_CLIENT_CONFIDENCE);
  }

  @Test
  void fullModeRoutesLinkedEmailsToReview() {
    ReviewSettingRepository repository = mock(ReviewSettingRepository.class);
    ReviewSetting setting = new ReviewSetting();
    setting.setMode("full");
    setting.setLowConfidenceThreshold(0.75d);
    when(repository.findAll()).thenReturn(List.of(setting));

    ReviewRoutingService service = new ReviewRoutingService(repository);
    var decision = service.emailDecision(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        false);

    assertThat(decision.requiresReview()).isTrue();
  }
}
