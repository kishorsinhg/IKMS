package com.ikms.ai.orchestration;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IntentDetectionService {

  public EnterpriseAiContracts.DetectedIntent detect(EnterpriseAiContracts.EnterpriseAiRequest request) {
    String normalizedPrompt = request.prompt() == null ? "" : request.prompt().trim();
    String lowered = normalizedPrompt.toLowerCase(Locale.ROOT);
    boolean comparisonRequested = request.operation() == EnterpriseAiOperation.COMPARE
        || lowered.contains("compare")
        || lowered.contains("previous version");
    String reasoningMode = switch (request.operation()) {
      case SEARCH -> "BROWSE";
      case ASK -> "ANSWER";
      case SUMMARIZE -> "SUMMARY";
      case EXPLAIN -> "EXPLANATION";
      case COMPARE -> "COMPARISON";
      case EXTRACT -> "EXTRACTION";
      case VALIDATE -> "VALIDATION";
    };
    return new EnterpriseAiContracts.DetectedIntent(
        request.operation(),
        normalizedPrompt,
        reasoningMode,
        comparisonRequested);
  }
}
