package com.ikms.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentConfidenceService {

  public BigDecimal overallConfidence(
      BigDecimal ocrConfidence,
      BigDecimal classificationConfidence,
      BigDecimal metadataConfidence,
      BigDecimal businessReferenceConfidence,
      BigDecimal validationConfidence,
      BigDecimal duplicateConfidence) {
    List<BigDecimal> values = new ArrayList<>();
    addIfPresent(values, ocrConfidence);
    addIfPresent(values, classificationConfidence);
    addIfPresent(values, metadataConfidence);
    addIfPresent(values, businessReferenceConfidence);
    addIfPresent(values, validationConfidence);
    addIfPresent(values, duplicateConfidence);
    if (values.isEmpty()) {
      return null;
    }
    BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
  }

  private static void addIfPresent(List<BigDecimal> target, BigDecimal value) {
    if (value != null) {
      target.add(value);
    }
  }
}
