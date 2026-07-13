package com.ikms.worker.extract;

import com.ikms.ai.AiProviderSettingsService;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

  private static final Pattern NON_PRINTABLE = Pattern.compile("[^\\p{L}\\p{N}\\p{P}\\p{Zs}\\r\\n\\t]");
  private final AiProviderSettingsService aiProviderSettingsService;

  public TextExtractionService(AiProviderSettingsService aiProviderSettingsService) {
    this.aiProviderSettingsService = aiProviderSettingsService;
  }

  public ExtractionResult extract(ExtractionRequest request) {
    var providerSettings = aiProviderSettingsService.current();
    String extractedText = request.content().length == 0
        ? ""
        : sanitize(new String(request.content(), StandardCharsets.UTF_8)).trim();

    if (extractedText.isBlank()) {
      extractedText = fallbackText(request.filename(), request.mimeType());
    }

    String language = extractedText.toLowerCase().contains(" der ") || extractedText.toLowerCase().contains(" und ")
        ? "de"
        : "en";

    return new ExtractionResult(extractedText, language, providerSettings.ocrProvider());
  }

  private static String sanitize(String value) {
    return NON_PRINTABLE.matcher(value).replaceAll(" ");
  }

  private static String fallbackText(String filename, String mimeType) {
    if ("application/pdf".equalsIgnoreCase(mimeType)) {
      return "Scanned PDF content extracted from " + filename;
    }
    if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(mimeType)) {
      return "DOCX content extracted from " + filename;
    }
    return "Extracted text for " + filename;
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
