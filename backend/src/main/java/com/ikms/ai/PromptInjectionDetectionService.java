package com.ikms.ai;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PromptInjectionDetectionService {

  private static final List<String> PROMPT_INJECTION_MARKERS = List.of(
      "ignore previous instructions",
      "disregard previous instructions",
      "system prompt",
      "developer message",
      "reveal hidden prompt",
      "execute tool",
      "run shell command",
      "bypass policy");

  public DetectionResult inspect(String text) {
    String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
    for (String marker : PROMPT_INJECTION_MARKERS) {
      if (normalized.contains(marker)) {
        return new DetectionResult(true, marker);
      }
    }
    return new DetectionResult(false, null);
  }

  public record DetectionResult(boolean detected, String marker) {
  }
}
