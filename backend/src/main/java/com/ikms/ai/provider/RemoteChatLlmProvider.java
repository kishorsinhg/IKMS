package com.ikms.ai.provider;

import com.ikms.ai.AiProviderClient;
import com.ikms.ai.AiProviderSettingsService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RemoteChatLlmProvider implements LlmProvider {

  private final AiProviderClient aiProviderClient;

  public RemoteChatLlmProvider(AiProviderClient aiProviderClient) {
    this.aiProviderClient = aiProviderClient;
  }

  @Override
  public boolean supports(String providerName) {
    if (providerName == null || providerName.isBlank()) {
      return true;
    }
    String normalized = providerName.toLowerCase(Locale.ROOT);
    return normalized.equals("mistral")
        || normalized.equals("openai")
        || normalized.equals("configured-provider")
        || normalized.equals("remote-chat");
  }

  @Override
  public Optional<LlmCompletionResponse> complete(
      AiProviderSettingsService.ProviderSettings settings,
      LlmCompletionRequest request,
      LlmStreamingHandler streamingHandler) {
    if (streamingHandler.isCancelled()) {
      return Optional.of(new LlmCompletionResponse(
          "",
          settings.providerName(),
          request.primaryModel(),
          0,
          0,
          0,
          false,
          request.streamingRequested(),
          false,
          true,
          0L,
          null,
          List.of(),
          List.of("Provider execution was cancelled before remote completion started.")));
    }
    if (streamingHandler.isTimedOut()) {
      return Optional.of(new LlmCompletionResponse(
          "",
          settings.providerName(),
          request.primaryModel(),
          0,
          0,
          0,
          true,
          request.streamingRequested(),
          true,
          false,
          0L,
          null,
          List.of(),
          List.of("Provider execution timed out before remote completion started.")));
    }
    streamingHandler.onStart();
    long startedAt = System.currentTimeMillis();
    Optional<AiProviderClient.ChatCompletionResult> completion = aiProviderClient.completeChat(
        settings,
        request.messages().stream()
            .map(message -> new AiProviderClient.ChatMessage(message.role(), message.content()))
            .toList(),
        request.primaryModel(),
        request.maxTokens(),
        request.temperature());
    if (completion.isEmpty()) {
      streamingHandler.onError("Provider returned no completion.");
      return Optional.empty();
    }
    AiProviderClient.ChatCompletionResult result = completion.get();
    if (streamingHandler.isCancelled()) {
      streamingHandler.onError("Provider completion was cancelled.");
      return Optional.of(new LlmCompletionResponse(
          "",
          settings.providerName(),
          result.model(),
          result.promptTokens(),
          0,
          result.promptTokens(),
          false,
          request.streamingRequested(),
          false,
          true,
          System.currentTimeMillis() - startedAt,
          null,
          List.of("start", "error"),
          List.of("Provider completion was cancelled after provider response became available.")));
    }
    if (streamingHandler.isTimedOut()) {
      streamingHandler.onError("Provider completion exceeded timeout.");
      return Optional.of(new LlmCompletionResponse(
          "",
          settings.providerName(),
          result.model(),
          result.promptTokens(),
          0,
          result.promptTokens(),
          true,
          request.streamingRequested(),
          true,
          false,
          System.currentTimeMillis() - startedAt,
          null,
          List.of("start", "error"),
          List.of("Provider completion exceeded the configured timeout budget.")));
    }
    streamingHandler.onDelta(result.content());
    streamingHandler.onComplete(result.content());
    return Optional.of(new LlmCompletionResponse(
        result.content(),
        settings.providerName(),
        result.model(),
        result.promptTokens(),
        result.completionTokens(),
        result.totalTokens(),
        false,
        request.streamingRequested(),
        false,
        false,
        System.currentTimeMillis() - startedAt,
        null,
        List.of("start", "delta", "complete"),
        List.of()));
  }
}
