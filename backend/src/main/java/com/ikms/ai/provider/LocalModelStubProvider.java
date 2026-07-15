package com.ikms.ai.provider;

import com.ikms.ai.AiProviderSettingsService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LocalModelStubProvider implements LlmProvider {

  @Override
  public boolean supports(String providerName) {
    return providerName != null && providerName.toLowerCase(Locale.ROOT).startsWith("local");
  }

  @Override
  public Optional<LlmCompletionResponse> complete(
      AiProviderSettingsService.ProviderSettings settings,
      LlmCompletionRequest request,
      LlmStreamingHandler streamingHandler) {
    if (streamingHandler.isCancelled()) {
      return Optional.of(new LlmCompletionResponse(
          "",
          request.providerName(),
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
          List.of("Provider execution was cancelled before local model generation started.")));
    }
    if (streamingHandler.isTimedOut()) {
      return Optional.of(new LlmCompletionResponse(
          "",
          request.providerName(),
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
          List.of("Local model execution timed out before content generation completed.")));
    }
    streamingHandler.onStart();
    String content = "Local model adapter is not configured for this environment.";
    streamingHandler.onDelta(content);
    streamingHandler.onComplete(content);
    return Optional.of(new LlmCompletionResponse(
        content,
        request.providerName(),
        request.primaryModel(),
        0,
        Math.max(1, content.length() / 4),
        Math.max(1, content.length() / 4),
        true,
        request.streamingRequested(),
        false,
        false,
        0L,
        null,
        List.of("start", "delta", "complete"),
        List.of("Local model provider is a placeholder adapter and requires environment-specific enablement.")));
  }
}
