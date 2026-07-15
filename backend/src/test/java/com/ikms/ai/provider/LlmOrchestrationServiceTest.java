package com.ikms.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.ai.AiProviderSettingsService;
import com.ikms.ai.context.ContextAssembly;
import com.ikms.ai.context.ContextPromptMessage;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LlmOrchestrationServiceTest {

  @Test
  void completeShouldPreferLocalProviderWhenRequested() {
    AiProviderSettingsService settingsService = mock(AiProviderSettingsService.class);
    when(settingsService.current()).thenReturn(new AiProviderSettingsService.ProviderSettings(
        "remote-chat",
        "mistral-large",
        "mistral-embed",
        "http://localhost",
        "secret",
        "tesseract",
        true));

    LlmProvider localProvider = new FixedProvider("local-stub", "local-small", "Local response");
    LlmProvider remoteProvider = new FixedProvider("remote-chat", "mistral-large", "Remote response");
    LlmOrchestrationService service = new LlmOrchestrationService(settingsService, List.of(remoteProvider, localProvider));

    LlmCompletionResponse response = service.complete(
        EnterpriseAiOperation.ASK,
        contextAssembly(List.of()),
        "Fallback answer",
        Map.of("localModelPreferred", true));

    assertThat(response.content()).isEqualTo("Local response");
    assertThat(response.providerName()).isEqualTo("local-stub");
    assertThat(response.streamed()).isFalse();
  }

  @Test
  void completeShouldReturnTimedOutResponseWhenProviderExceedsBudget() {
    AiProviderSettingsService settingsService = mock(AiProviderSettingsService.class);
    when(settingsService.current()).thenReturn(new AiProviderSettingsService.ProviderSettings(
        "remote-chat",
        "mistral-large",
        "mistral-embed",
        "http://localhost",
        "secret",
        "tesseract",
        true));

    LlmProvider timeoutProvider = new TimeoutAwareProvider();
    LlmOrchestrationService service = new LlmOrchestrationService(settingsService, List.of(timeoutProvider));

    LlmCompletionResponse response = service.complete(
        EnterpriseAiOperation.SUMMARIZE,
        contextAssembly(List.of()),
        "Fallback answer",
        Map.of("timeoutMs", 1));

    assertThat(response.timedOut()).isTrue();
    assertThat(response.warnings()).anyMatch(warning -> warning.toLowerCase().contains("timeout"));
  }

  @Test
  void streamShouldCaptureEventsAndRestrictedNotice() {
    AiProviderSettingsService settingsService = mock(AiProviderSettingsService.class);
    when(settingsService.current()).thenReturn(new AiProviderSettingsService.ProviderSettings(
        "remote-chat",
        "mistral-large",
        "mistral-embed",
        "http://localhost",
        "secret",
        "tesseract",
        true));

    LlmProvider streamingProvider = new StreamingProvider();
    LlmOrchestrationService service = new LlmOrchestrationService(settingsService, List.of(streamingProvider));

    LlmCompletionResponse response = service.stream(
        EnterpriseAiOperation.EXPLAIN,
        contextAssembly(List.of("Restricted documents were excluded from provider-visible context.")),
        "Fallback answer",
        Map.of("stream", true));

    assertThat(response.streamed()).isTrue();
    assertThat(response.streamEvents()).containsExactly("start", "delta", "complete");
    assertThat(response.restrictedContentNotice()).isEqualTo("Restricted documents were excluded from provider-visible context.");
  }

  private static ContextAssembly contextAssembly(List<String> warnings) {
    return new ContextAssembly(
        "remote-chat",
        "mistral-large",
        "system",
        "user",
        List.of(
            new ContextPromptMessage("system", "system", 1),
            new ContextPromptMessage("user", "user", 1)),
        List.of(),
        List.of(),
        Map.of(),
        Map.of("reserveCompletion", 256, "total", 1024),
        warnings);
  }

  private record FixedProvider(String supportedName, String modelName, String content) implements LlmProvider {

    @Override
    public boolean supports(String providerName) {
      return supportedName.equals(providerName)
          || ("local-stub".equals(supportedName) && "local".equals(providerName));
    }

    @Override
    public Optional<LlmCompletionResponse> complete(
        AiProviderSettingsService.ProviderSettings settings,
        LlmCompletionRequest request,
        LlmStreamingHandler streamingHandler) {
      streamingHandler.onStart();
      streamingHandler.onDelta(content);
      streamingHandler.onComplete(content);
      return Optional.of(new LlmCompletionResponse(
          content,
          supportedName,
          modelName,
          8,
          12,
          20,
          false,
          request.streamingRequested(),
          false,
          false,
          5L,
          null,
          List.of("start", "delta", "complete"),
          List.of()));
    }
  }

  private static final class TimeoutAwareProvider implements LlmProvider {

    @Override
    public boolean supports(String providerName) {
      return "remote-chat".equals(providerName);
    }

    @Override
    public Optional<LlmCompletionResponse> complete(
        AiProviderSettingsService.ProviderSettings settings,
        LlmCompletionRequest request,
        LlmStreamingHandler streamingHandler) {
      streamingHandler.onStart();
      try {
        Thread.sleep(5L);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
      if (streamingHandler.isTimedOut()) {
        streamingHandler.onError("timeout");
        return Optional.of(new LlmCompletionResponse(
            "",
            "remote-chat",
            request.primaryModel(),
            0,
            0,
            0,
            true,
            request.streamingRequested(),
            true,
            false,
            5L,
            null,
            List.of("start", "error"),
            List.of("Provider execution exceeded the configured timeout budget.")));
      }
      return Optional.empty();
    }
  }

  private static final class StreamingProvider implements LlmProvider {

    @Override
    public boolean supports(String providerName) {
      return "remote-chat".equals(providerName);
    }

    @Override
    public Optional<LlmCompletionResponse> complete(
        AiProviderSettingsService.ProviderSettings settings,
        LlmCompletionRequest request,
        LlmStreamingHandler streamingHandler) {
      streamingHandler.onStart();
      streamingHandler.onDelta("partial");
      streamingHandler.onComplete("complete");
      return Optional.of(new LlmCompletionResponse(
          "complete",
          "remote-chat",
          request.primaryModel(),
          4,
          6,
          10,
          false,
          true,
          false,
          false,
          3L,
          null,
          List.of(),
          List.of()));
    }
  }
}
