package com.ikms.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiProviderClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public AiProviderClient(ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().build();
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

  public boolean configured(AiProviderSettingsService.ProviderSettings settings) {
    return settings != null
        && settings.active()
        && settings.apiBaseUrl() != null
        && !settings.apiBaseUrl().isBlank()
        && settings.apiKey() != null
        && !settings.apiKey().isBlank();
  }

  private static String normalizeBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private static String truncate(String value, int maxLength) {
    String safe = value == null ? "" : value.trim();
    return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
  }

  public record ClientCandidate(String clientId, String displayName) {
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
}
