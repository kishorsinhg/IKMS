package com.ikms.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiProviderClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  @Autowired
  public AiProviderClient(ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(5000);
    requestFactory.setReadTimeout(10000);
    this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    this.objectMapper = objectMapper;
  }

  AiProviderClient(ObjectMapper objectMapper, RestClient restClient) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
  }

  public Optional<ClassificationPrediction> classify(
      AiProviderSettingsService.ProviderSettings settings,
      String filename,
      String extractedText,
      List<String> documentTypes,
      List<ClientCandidate> clients,
      String language) {
    if (!configured(settings)) {
      return Optional.empty();
    }

    String prompt = """
        Return strict JSON with keys: title, documentType, language, suggestedClientId, clientMatchConfidence, classificationConfidence, extractionConfidence.
        Filename: %s
        Language hint: %s
        Supported document types: %s
        Candidate clients: %s
        Content:
        %s
        """.formatted(
        filename,
        language,
        String.join(", ", documentTypes),
        clients.stream().map(candidate -> candidate.clientId() + ":" + candidate.displayName()).reduce((a, b) -> a + ", " + b).orElse("none"),
        truncate(extractedText, 6000));

    try {
      ChatCompletionResponse response = restClient.post()
          .uri(normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(headers -> headers.setBearerAuth(settings.apiKey()))
          .body(Map.of(
              "model", settings.modelName(),
              "temperature", 0,
              "response_format", Map.of("type", "json_object"),
              "messages", List.of(
                  Map.of("role", "system", "content", "You classify insurance broker knowledge documents."),
                  Map.of("role", "user", "content", prompt))))
          .retrieve()
          .body(ChatCompletionResponse.class);
      if (response == null || response.choices == null || response.choices.isEmpty()) {
        return Optional.empty();
      }
      String content = response.choices.getFirst().message == null ? null : response.choices.getFirst().message.content;
      if (content == null || content.isBlank()) {
        return Optional.empty();
      }
      ClassificationPrediction prediction = objectMapper.readValue(content, ClassificationPrediction.class);
      return Optional.of(prediction);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  public Optional<List<List<Double>>> embed(
      AiProviderSettingsService.ProviderSettings settings,
      List<String> inputs) {
    if (!configured(settings) || inputs == null || inputs.isEmpty()) {
      return Optional.empty();
    }

    try {
      EmbeddingsResponse response = restClient.post()
          .uri(normalizeBaseUrl(settings.apiBaseUrl()) + "/embeddings")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(headers -> headers.setBearerAuth(settings.apiKey()))
          .body(Map.of(
              "model", settings.embeddingModelName(),
              "input", inputs))
          .retrieve()
          .body(EmbeddingsResponse.class);
      if (response == null || response.data == null || response.data.isEmpty()) {
        return Optional.empty();
      }
      List<List<Double>> vectors = new ArrayList<>();
      for (EmbeddingData item : response.data) {
        vectors.add(item.embedding == null ? List.of() : item.embedding);
      }
      return Optional.of(vectors);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  public Optional<OcrResult> ocr(
      AiProviderSettingsService.ProviderSettings settings,
      String mimeType,
      byte[] content) {
    if (!configured(settings) || content == null || content.length == 0) {
      return Optional.empty();
    }

    String safeMimeType = mimeType == null || mimeType.isBlank() ? "application/pdf" : mimeType;
    String documentUrl = "data:" + safeMimeType + ";base64," + java.util.Base64.getEncoder().encodeToString(content);

    try {
      OcrResponse response = restClient.post()
          .uri(normalizeBaseUrl(settings.apiBaseUrl()) + "/ocr")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(headers -> headers.setBearerAuth(settings.apiKey()))
          .body(Map.of(
              "model", settings.ocrProvider(),
              "document", Map.of(
                  "type", "document_url",
                  "document_url", documentUrl),
              "confidence_scores_granularity", "page"))
          .retrieve()
          .body(OcrResponse.class);
      if (response == null || response.pages == null || response.pages.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new OcrResult(
          response.model,
          response.pages.stream()
              .map(page -> new OcrPage(
                  page.index == null ? null : page.index + 1,
                  page.markdown,
                  page.confidenceScores == null ? null : page.confidenceScores.averagePageConfidenceScore))
              .toList()));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  public Optional<String> answerWithEvidence(
      AiProviderSettingsService.ProviderSettings settings,
      String question,
      List<EvidenceSnippet> evidence,
      boolean conflictingEvidence) {
    if (!configured(settings) || question == null || question.isBlank() || evidence == null || evidence.isEmpty()) {
      return Optional.empty();
    }

    String evidenceText = evidence.stream()
        .map(snippet -> """
            Source: %s
            Type: %s
            Location: %s
            Excerpt: %s
            """.formatted(
            snippet.title(),
            snippet.sourceType(),
            snippet.location(),
            truncate(snippet.excerpt(), 1000)))
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");

    String systemPrompt = conflictingEvidence
        ? "Answer only from the provided client evidence. Be concise, mention that the evidence conflicts where applicable, and recommend manual review for the conflict. Do not invent facts."
        : "Answer only from the provided client evidence. Be concise and do not invent facts. If the evidence is limited, say so.";

    try {
      ChatCompletionResponse response = restClient.post()
          .uri(normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(headers -> headers.setBearerAuth(settings.apiKey()))
          .body(Map.of(
              "model", settings.modelName(),
              "temperature", 0,
              "max_tokens", 220,
              "messages", List.of(
                  Map.of("role", "system", "content", systemPrompt),
                  Map.of("role", "user", "content", """
                      Question:
                      %s

                      Evidence:
                      %s
                      """.formatted(question.trim(), evidenceText)))))
          .retrieve()
          .body(ChatCompletionResponse.class);
      if (response == null || response.choices == null || response.choices.isEmpty()) {
        return Optional.empty();
      }
      String content = response.choices.getFirst().message == null ? null : response.choices.getFirst().message.content;
      if (content == null || content.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(content.trim());
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  public boolean configured(AiProviderSettingsService.ProviderSettings settings) {
    return settings != null
        && settings.active()
        && settings.apiBaseUrl() != null
        && !settings.apiBaseUrl().isBlank()
        && settings.apiKey() != null
        && !settings.apiKey().isBlank();
  }

  public ProviderValidationResult validate(AiProviderSettingsService.ProviderSettings settings) {
    if (!configured(settings)) {
      return new ProviderValidationResult(
          false,
          false,
          false,
          supportsOcrProvider(settings == null ? null : settings.ocrProvider()),
          "INVALID_CONFIGURATION",
          "API base URL and API key must be configured before validation.");
    }

    ValidationAttempt chatAttempt = validateChatModel(settings);
    ValidationAttempt embeddingAttempt = validateEmbeddingModel(settings);
    boolean ocrProviderSupported = supportsOcrProvider(settings.ocrProvider());
    boolean valid = chatAttempt.ok() && embeddingAttempt.ok() && ocrProviderSupported;
    String status = valid ? "READY" : "DEGRADED";
    String message = valid
        ? "Chat model, embedding model, and OCR provider are reachable."
        : joinMessages(chatAttempt.message(), embeddingAttempt.message(), ocrProviderSupported ? null : "OCR provider is not supported.");
    return new ProviderValidationResult(
        valid,
        chatAttempt.ok(),
        embeddingAttempt.ok(),
        ocrProviderSupported,
        status,
        message);
  }

  private static String normalizeBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private static String truncate(String value, int maxLength) {
    String safe = value == null ? "" : value.trim();
    return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
  }

  private ValidationAttempt validateChatModel(AiProviderSettingsService.ProviderSettings settings) {
    try {
      ChatCompletionResponse response = restClient.post()
          .uri(normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(headers -> headers.setBearerAuth(settings.apiKey()))
          .body(Map.of(
              "model", settings.modelName(),
              "temperature", 0,
              "max_tokens", 8,
              "messages", List.of(
                  Map.of("role", "system", "content", "Respond with the word OK."),
                  Map.of("role", "user", "content", "Validation probe"))))
          .retrieve()
          .body(ChatCompletionResponse.class);
      boolean reachable = response != null && response.choices != null && !response.choices.isEmpty();
      return new ValidationAttempt(reachable, reachable ? null : "Chat model did not return a completion.");
    } catch (Exception exception) {
      return new ValidationAttempt(false, "Chat model validation failed: " + summarizeException(exception));
    }
  }

  private ValidationAttempt validateEmbeddingModel(AiProviderSettingsService.ProviderSettings settings) {
    try {
      var result = embed(settings, List.of("IKMS validation probe"));
      boolean reachable = result.isPresent()
          && !result.get().isEmpty()
          && result.get().getFirst() != null
          && !result.get().getFirst().isEmpty();
      return new ValidationAttempt(reachable, reachable ? null : "Embedding model did not return a vector.");
    } catch (Exception exception) {
      return new ValidationAttempt(false, "Embedding model validation failed: " + summarizeException(exception));
    }
  }

  private static boolean supportsOcrProvider(String ocrProvider) {
    if (ocrProvider == null) {
      return false;
    }
    String normalized = ocrProvider.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("tesseract")
        || normalized.equals("configured-provider")
        || normalized.equals("mistral-ocr")
        || normalized.startsWith("mistral-ocr");
  }

  private static String joinMessages(String... messages) {
    return java.util.Arrays.stream(messages)
        .filter(message -> message != null && !message.isBlank())
        .reduce((left, right) -> left + " " + right)
        .orElse("Validation failed.");
  }

  private static String summarizeException(Exception exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof SocketTimeoutException) {
      return "request timed out.";
    }
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return "request failed.";
    }
    return truncate(message.replace('\n', ' '), 180);
  }

  public record ClientCandidate(String clientId, String displayName) {
  }

  public record EvidenceSnippet(
      String sourceType,
      String title,
      String location,
      String excerpt) {
  }

  public record OcrResult(
      String model,
      List<OcrPage> pages) {
  }

  public record OcrPage(
      Integer pageNumber,
      String markdown,
      Double averageConfidenceScore) {
  }

  public record ProviderValidationResult(
      boolean valid,
      boolean chatModelReachable,
      boolean embeddingModelReachable,
      boolean ocrProviderSupported,
      String status,
      String message) {
  }

  private record ValidationAttempt(boolean ok, String message) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ClassificationPrediction(
      String title,
      String documentType,
      String language,
      String suggestedClientId,
      Double clientMatchConfidence,
      Double classificationConfidence,
      Double extractionConfidence) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ChatCompletionResponse {
    public List<Choice> choices;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Choice {
    public Message message;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Message {
    public String content;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class EmbeddingsResponse {
    public List<EmbeddingData> data;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class EmbeddingData {
    public List<Double> embedding;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class OcrResponse {
    public List<OcrResponsePage> pages;
    public String model;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class OcrResponsePage {
    public Integer index;
    public String markdown;

    @JsonProperty("confidence_scores")
    public OcrConfidenceScores confidenceScores;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class OcrConfidenceScores {
    @JsonProperty("average_page_confidence_score")
    public Double averagePageConfidenceScore;
  }
}
