package com.ikms.ai.orchestration;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GroundingValidationService {

  public EnterpriseAiContracts.GroundingValidation validate(
      List<EnterpriseAiContracts.CitationReference> citations,
      List<String> warnings) {
    List<String> mergedWarnings = new ArrayList<>(warnings == null ? List.of() : warnings);
    if (mergedWarnings.stream().anyMatch(warning -> warning.toLowerCase(java.util.Locale.ROOT).contains("insufficient evidence"))) {
      return new EnterpriseAiContracts.GroundingValidation(
          false,
          0d,
          0d,
          List.copyOf(mergedWarnings));
    }

    boolean grounded = !citations.isEmpty();
    long fullyLocated = citations.stream()
        .filter(citation -> citation.pageNumber() != null || (citation.section() != null && !citation.section().isBlank()))
        .count();
    double citationCoverage = citations.isEmpty() ? 0d : fullyLocated / (double) citations.size();
    boolean hasLowConfidence = citations.stream().anyMatch(citation -> "LOW".equalsIgnoreCase(citation.confidence()));
    boolean hasUnknownConfidence = citations.stream().anyMatch(citation -> "UNKNOWN".equalsIgnoreCase(citation.confidence()));
    double groundingScore = !grounded
        ? 0d
        : hasLowConfidence
            ? 0.75d
            : hasUnknownConfidence || citationCoverage < 1d
                ? 0.85d
                : 0.98d;
    if (!grounded) {
      mergedWarnings.add("No grounded evidence was available for this orchestration response.");
    } else if (citationCoverage < 1d) {
      mergedWarnings.add("Citation coverage is partial; some evidence lacks precise page or section provenance.");
    }
    if (hasLowConfidence) {
      mergedWarnings.add("One or more citations rely on low-confidence evidence and require manual review.");
    }
    return new EnterpriseAiContracts.GroundingValidation(
        grounded && citationCoverage >= 0.5d,
        groundingScore,
        citationCoverage,
        List.copyOf(mergedWarnings));
  }
}
