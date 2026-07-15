package com.ikms.document;

import org.springframework.stereotype.Service;

@Service
public class LanguageDetectionService {

  public String detect(String extractedText, String extractedLanguage) {
    if (extractedLanguage != null && !extractedLanguage.isBlank()) {
      return extractedLanguage;
    }
    String safeText = extractedText == null ? "" : extractedText.toLowerCase();
    if (safeText.contains(" der ") || safeText.contains(" und ")) {
      return "de";
    }
    return "en";
  }
}
