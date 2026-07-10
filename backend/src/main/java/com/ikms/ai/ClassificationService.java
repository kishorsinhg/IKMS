package com.ikms.ai;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {

  public ClassificationResult classify(ClassificationRequest request) {
    String normalizedFilename = request.filename().toLowerCase(Locale.ROOT);
    String language = request.language() == null || request.language().isBlank() ? "en" : request.language();
    String suggestedTitle = normalizedFilename.replaceAll("\\.[^.]+$", "").replace('-', ' ').trim();
    String documentType = normalizedFilename.contains("policy") || normalizedFilename.contains("renewal")
        ? "POLICY_DOCUMENT"
        : normalizedFilename.endsWith(".pdf")
            ? "PDF_DOCUMENT"
            : "DOCX_DOCUMENT";

    BigDecimal clientMatchConfidence = request.clientId() == null ? new BigDecimal("0.2500") : new BigDecimal("0.9800");
    BigDecimal classificationConfidence = normalizedFilename.endsWith(".pdf")
        ? new BigDecimal("0.9300")
        : new BigDecimal("0.9000");
    BigDecimal extractionConfidence = request.extractedText().isBlank()
        ? new BigDecimal("0.2000")
        : new BigDecimal("0.9100");

    return new ClassificationResult(
        null,
        suggestedTitle.isBlank() ? request.filename() : suggestedTitle,
        clientMatchConfidence,
        classificationConfidence,
        extractionConfidence,
        Map.of("documentType", documentType, "language", language));
  }

  public record ClassificationRequest(
      UUID clientId,
      String filename,
      String mimeType,
      String extractedText,
      String language) {
  }

  public record ClassificationResult(
      UUID suggestedClientId,
      String suggestedTitle,
      BigDecimal clientMatchConfidence,
      BigDecimal classificationConfidence,
      BigDecimal extractionConfidence,
      Map<String, String> metadata) {
  }
}
