package com.ikms.ai;

import com.ikms.client.Client;
import com.ikms.client.ClientRepository;
import com.ikms.config.domain.DocumentType;
import com.ikms.config.domain.DocumentTypeRepository;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {

  private final DocumentTypeRepository documentTypeRepository;
  private final ClientRepository clientRepository;
  private final AiProviderSettingsService aiProviderSettingsService;

  public ClassificationService(
      DocumentTypeRepository documentTypeRepository,
      ClientRepository clientRepository,
      AiProviderSettingsService aiProviderSettingsService) {
    this.documentTypeRepository = documentTypeRepository;
    this.clientRepository = clientRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
  }

  public ClassificationResult classify(ClassificationRequest request) {
    var providerSettings = aiProviderSettingsService.current();
    String normalizedFilename = request.filename().toLowerCase(Locale.ROOT);
    String language = request.language() == null || request.language().isBlank() ? "en" : request.language();
    String suggestedTitle = normalizedFilename.replaceAll("\\.[^.]+$", "").replace('-', ' ').trim();
    String documentType = resolveDocumentType(normalizedFilename);
    UUID suggestedClientId = resolveSuggestedClientId(request.extractedText());

    BigDecimal clientMatchConfidence = request.clientId() != null
        ? new BigDecimal("0.9800")
        : suggestedClientId != null ? new BigDecimal("0.7800") : new BigDecimal("0.2500");
    BigDecimal classificationConfidence = normalizedFilename.endsWith(".pdf")
        ? new BigDecimal("0.9300")
        : new BigDecimal("0.9000");
    BigDecimal extractionConfidence = request.extractedText().isBlank()
        ? new BigDecimal("0.2000")
        : new BigDecimal("0.9100");

    return new ClassificationResult(
        suggestedClientId,
        suggestedTitle.isBlank() ? request.filename() : suggestedTitle,
        clientMatchConfidence,
        classificationConfidence,
        extractionConfidence,
        Map.of(
            "documentType", documentType,
            "language", language,
            "provider", providerSettings.providerName(),
            "model", providerSettings.modelName()));
  }

  private String resolveDocumentType(String normalizedFilename) {
    return documentTypeRepository.findAll().stream()
        .map(DocumentType::getName)
        .filter(name -> normalizedFilename.contains(name.toLowerCase(Locale.ROOT).replace(' ', '_'))
            || normalizedFilename.contains(name.toLowerCase(Locale.ROOT).replace(' ', '-'))
            || normalizedFilename.contains(name.toLowerCase(Locale.ROOT)))
        .findFirst()
        .orElseGet(() -> normalizedFilename.contains("policy") || normalizedFilename.contains("renewal")
            ? "Policy"
            : normalizedFilename.endsWith(".pdf") ? "PDF Document" : "DOCX Document");
  }

  private UUID resolveSuggestedClientId(String extractedText) {
    if (extractedText == null || extractedText.isBlank()) {
      return null;
    }
    String normalizedText = extractedText.toLowerCase(Locale.ROOT);
    for (Client client : clientRepository.findAll()) {
      if (client.getDisplayName() != null && normalizedText.contains(client.getDisplayName().toLowerCase(Locale.ROOT))) {
        return client.getId();
      }
      if (client.getClientId() != null && normalizedText.contains(client.getClientId().toLowerCase(Locale.ROOT))) {
        return client.getId();
      }
    }
    return null;
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
