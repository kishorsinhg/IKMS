package com.ikms.ai;

import com.ikms.client.Client;
import com.ikms.client.ClientRepository;
import com.ikms.config.domain.DocumentType;
import com.ikms.config.domain.DocumentTypeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {

  private final DocumentTypeRepository documentTypeRepository;
  private final ClientRepository clientRepository;
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;

  public ClassificationService(
      DocumentTypeRepository documentTypeRepository,
      ClientRepository clientRepository,
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient) {
    this.documentTypeRepository = documentTypeRepository;
    this.clientRepository = clientRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
  }

  public ClassificationResult classify(ClassificationRequest request) {
    var providerSettings = aiProviderSettingsService.current();
    String normalizedFilename = request.filename().toLowerCase(Locale.ROOT);
    String defaultLanguage = request.language() == null || request.language().isBlank() ? "en" : request.language();
    String suggestedTitle = normalizedFilename.replaceAll("\\.[^.]+$", "").replace('-', ' ').trim();
    String documentType = resolveDocumentType(normalizedFilename);
    UUID suggestedClientId = resolveSuggestedClientId(request.extractedText());

    var providerPrediction = aiProviderClient.classify(
        providerSettings,
        request.filename(),
        request.extractedText(),
        documentTypeRepository.findAll().stream().map(DocumentType::getName).toList(),
        clientRepository.findAll().stream()
            .map(client -> new AiProviderClient.ClientCandidate(client.getClientId(), client.getDisplayName()))
            .toList(),
        defaultLanguage);

    String language = providerPrediction.map(AiProviderClient.ClassificationPrediction::language)
        .filter(value -> value != null && !value.isBlank())
        .orElse(defaultLanguage);
    suggestedTitle = providerPrediction.map(AiProviderClient.ClassificationPrediction::title)
        .filter(value -> value != null && !value.isBlank())
        .orElse(suggestedTitle.isBlank() ? request.filename() : suggestedTitle);
    documentType = providerPrediction.map(AiProviderClient.ClassificationPrediction::documentType)
        .filter(value -> value != null && !value.isBlank())
        .orElse(documentType);
    suggestedClientId = providerPrediction.map(AiProviderClient.ClassificationPrediction::suggestedClientId)
        .map(this::parseUuid)
        .orElse(suggestedClientId);

    BigDecimal clientMatchConfidence = providerPrediction
        .map(AiProviderClient.ClassificationPrediction::clientMatchConfidence)
        .map(this::confidence)
        .orElse(request.clientId() != null
        ? new BigDecimal("0.9800")
        : suggestedClientId != null ? new BigDecimal("0.7800") : new BigDecimal("0.2500"));
    BigDecimal classificationConfidence = providerPrediction
        .map(AiProviderClient.ClassificationPrediction::classificationConfidence)
        .map(this::confidence)
        .orElse(normalizedFilename.endsWith(".pdf")
        ? new BigDecimal("0.9300")
        : new BigDecimal("0.9000"));
    BigDecimal extractionConfidence = providerPrediction
        .map(AiProviderClient.ClassificationPrediction::extractionConfidence)
        .map(this::confidence)
        .orElse(request.extractedText().isBlank()
        ? new BigDecimal("0.2000")
        : new BigDecimal("0.9100"));

    return new ClassificationResult(
        suggestedClientId,
        suggestedTitle,
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

  private UUID parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ignored) {
      return clientRepository.findAll().stream()
          .filter(client -> value.equalsIgnoreCase(client.getClientId()))
          .map(Client::getId)
          .findFirst()
          .orElse(null);
    }
  }

  private BigDecimal confidence(Double value) {
    if (value == null) {
      return null;
    }
    double normalized = Math.max(0d, Math.min(1d, value));
    return BigDecimal.valueOf(normalized).setScale(4, RoundingMode.HALF_UP);
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
