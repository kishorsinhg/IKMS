package com.ikms.review;

import com.ikms.config.domain.ReviewSetting;
import com.ikms.config.domain.ReviewSettingRepository;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewRoutingService {

  private final ReviewSettingRepository reviewSettingRepository;

  public ReviewRoutingService(ReviewSettingRepository reviewSettingRepository) {
    this.reviewSettingRepository = reviewSettingRepository;
  }

  public ReviewDecision documentDecision(
      UUID clientId,
      UUID itemId,
      BigDecimal clientMatchConfidence,
      BigDecimal classificationConfidence,
      BigDecimal extractionConfidence) {
    ReviewSetting setting = currentSetting();
    String mode = normalizeMode(setting.getMode());
    if (clientId == null) {
      return new ReviewDecision(true, ReviewQueueReason.UNLINKED);
    }

    boolean belowThreshold = below(clientMatchConfidence, setting.getLowConfidenceThreshold())
        || below(classificationConfidence, setting.getLowConfidenceThreshold())
        || below(extractionConfidence, setting.getLowConfidenceThreshold());
    ReviewQueueReason confidenceReason = confidenceReason(
        clientMatchConfidence,
        classificationConfidence,
        extractionConfidence,
        setting.getLowConfidenceThreshold());

    return switch (mode) {
      case "full", "manual" -> new ReviewDecision(true, confidenceReason);
      case "sampling" -> new ReviewDecision(belowThreshold || sampled(itemId), confidenceReason);
      case "straight-through", "straight_through", "stp" -> new ReviewDecision(belowThreshold, confidenceReason);
      default -> new ReviewDecision(belowThreshold, confidenceReason);
    };
  }

  public ReviewDecision emailDecision(UUID clientId, UUID itemId, boolean attachmentNeedsReview) {
    ReviewSetting setting = currentSetting();
    String mode = normalizeMode(setting.getMode());
    if (clientId == null) {
      return new ReviewDecision(true, ReviewQueueReason.UNLINKED);
    }

    return switch (mode) {
      case "full", "manual" -> new ReviewDecision(true, ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE);
      case "sampling" -> new ReviewDecision(attachmentNeedsReview || sampled(itemId), ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE);
      case "straight-through", "straight_through", "stp" -> new ReviewDecision(attachmentNeedsReview, ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE);
      default -> new ReviewDecision(attachmentNeedsReview, ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE);
    };
  }

  private ReviewSetting currentSetting() {
    return reviewSettingRepository.findAll().stream().findFirst().orElseGet(this::defaultSetting);
  }

  private ReviewSetting defaultSetting() {
    ReviewSetting setting = new ReviewSetting();
    setting.setMode("confidence");
    setting.setLowConfidenceThreshold(0.75d);
    return setting;
  }

  private boolean below(BigDecimal value, double threshold) {
    return value == null || value.doubleValue() < threshold;
  }

  private boolean sampled(UUID itemId) {
    return Math.floorMod(itemId.hashCode(), 5) == 0;
  }

  private String normalizeMode(String mode) {
    return mode == null ? "confidence" : mode.trim().toLowerCase(Locale.ROOT);
  }

  private ReviewQueueReason confidenceReason(
      BigDecimal clientMatchConfidence,
      BigDecimal classificationConfidence,
      BigDecimal extractionConfidence,
      double threshold) {
    if (below(clientMatchConfidence, threshold)) {
      return ReviewQueueReason.LOW_CLIENT_CONFIDENCE;
    }
    if (below(classificationConfidence, threshold)) {
      return ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE;
    }
    if (below(extractionConfidence, threshold)) {
      return ReviewQueueReason.LOW_EXTRACTION_CONFIDENCE;
    }
    return ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE;
  }

  public record ReviewDecision(boolean requiresReview, ReviewQueueReason reason) {
  }
}
