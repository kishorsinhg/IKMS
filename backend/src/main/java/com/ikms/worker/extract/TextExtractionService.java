package com.ikms.worker.extract;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

  public ExtractionResult extract(ExtractionRequest request) {
    String extractedText = request.content().length == 0
        ? ""
        : new String(request.content(), StandardCharsets.UTF_8).trim();

    if (extractedText.isBlank()) {
      extractedText = "Extracted placeholder text for " + request.filename();
    }

    String language = extractedText.toLowerCase().contains(" der ") || extractedText.toLowerCase().contains(" und ")
        ? "de"
        : "en";

    return new ExtractionResult(extractedText, language, request.provider());
  }

  public record ExtractionRequest(
      String filename,
      String mimeType,
      byte[] content,
      String provider) {
  }

  public record ExtractionResult(
      String extractedText,
      String language,
      String provider) {
  }
}
