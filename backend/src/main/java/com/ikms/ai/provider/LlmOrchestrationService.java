package com.ikms.ai.provider;

import com.ikms.ai.AiProviderSettingsService;
import com.ikms.ai.context.ContextAssembly;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LlmOrchestrationService {

  private static final int DEFAULT_MAX_RETRIES = 2;
  private static final long DEFAULT_TIMEOUT_MS = 15_000L;

  private final AiProviderSettingsService aiProviderSettingsService;
  private final List<LlmProvider> llmProviders;

  public LlmOrchestrationService(
      AiProviderSettingsService aiProviderSettingsService,
      List<LlmProvider> llmProviders) {
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.llmProviders = llmProviders.stream()
        .sorted(Comparator.comparing(provider -> provider.getClass().getSimpleName()))
        .toList();
  }

  public LlmCompletionResponse complete(
      EnterpriseAiOperation operation,
      ContextAssembly contextAssembly,
      String fallbackAnswer,
      Map<String, Object> parameters) {
    return execute(operation, contextAssembly, fallbackAnswer, parameters, false);
  }

  public LlmCompletionResponse stream(
      EnterpriseAiOperation operation,
      ContextAssembly contextAssembly,
      String fallbackAnswer,
      Map<String, Object> parameters) {
    return execute(operation, contextAssembly, fallbackAnswer, parameters, true);
  }

  private LlmCompletionResponse execute(
      EnterpriseAiOperation operation,
      ContextAssembly contextAssembly,
      String fallbackAnswer,
      Map<String, Object> parameters,
      boolean streamingRequested) {
    AiProviderSettingsService.ProviderSettings settings = aiProviderSettingsService.current();
    ExecutionOptions executionOptions = resolveExecutionOptions(settings, parameters, streamingRequested, contextAssembly);
    if (!isConfigured(settings) && !executionOptions.localModelPreferred()) {
      return fallbackResponse(settings, fallbackAnswer, "Provider settings are inactive or incomplete.", executionOptions, contextAssembly);
    }

    LlmProvider provider = selectProvider(executionOptions.providerName(), executionOptions.localModelPreferred())
        .orElse(null);
    if (provider == null) {
      return fallbackResponse(
          settings,
          fallbackAnswer,
          "No provider adapter is registered for " + executionOptions.providerName() + ".",
          executionOptions,
          contextAssembly);
    }

    String fallbackModel = resolveFallbackModel(settings, parameters);
    LlmCompletionRequest request = new LlmCompletionRequest(
        executionOptions.providerName(),
        executionOptions.primaryModel(),
        fallbackModel,
        contextAssembly.messages(),
        resolveMaxTokens(operation, contextAssembly),
        0.1d,
        executionOptions.streamingRequested(),
        executionOptions.timeoutMs(),
        executionOptions.localModelPreferred(),
        executionOptions.cancellationKey(),
        Map.of(
            "operation", operation.name(),
            "tokenBudget", contextAssembly.tokenBudget(),
            "metadata", contextAssembly.metadata(),
            "restrictedContentNotice", executionOptions.restrictedContentNotice() == null ? "" : executionOptions.restrictedContentNotice()));

    BufferingStreamingHandler streamingHandler = new BufferingStreamingHandler(executionOptions.timeoutMs(), executionOptions.cancellationRequested());
    Optional<LlmCompletionResponse> primary = tryComplete(provider, settings, request, streamingHandler);
    if (primary.isPresent()) {
      return enrich(primary.get(), contextAssembly, executionOptions, false, streamingHandler);
    }

    if (!fallbackModel.equals(executionOptions.primaryModel()) && !executionOptions.cancellationRequested() && !streamingHandler.isTimedOut()) {
      BufferingStreamingHandler fallbackStreamingHandler = new BufferingStreamingHandler(executionOptions.timeoutMs(), executionOptions.cancellationRequested());
      Optional<LlmCompletionResponse> secondary = tryComplete(
          provider,
          settings,
          new LlmCompletionRequest(
              request.providerName(),
              fallbackModel,
              fallbackModel,
              request.messages(),
              request.maxTokens(),
              request.temperature(),
              request.streamingRequested(),
              request.timeoutMs(),
              request.localModelAllowed(),
              request.cancellationKey(),
              request.metadata()),
          fallbackStreamingHandler);
      if (secondary.isPresent()) {
        return enrich(secondary.get(), contextAssembly, executionOptions, true, fallbackStreamingHandler);
      }
    }

    return fallbackResponse(
        settings,
        fallbackAnswer,
        streamingHandler.isTimedOut()
            ? "Provider execution exceeded the configured timeout."
            : executionOptions.cancellationRequested()
                ? "Provider execution was cancelled before completion."
                : "Provider execution failed after retries and fallback attempts.",
        executionOptions,
        contextAssembly);
  }

  private Optional<LlmCompletionResponse> tryComplete(
      LlmProvider provider,
      AiProviderSettingsService.ProviderSettings settings,
      LlmCompletionRequest request,
      LlmStreamingHandler streamingHandler) {
    Optional<LlmCompletionResponse> lastResponse = Optional.empty();
    for (int attempt = 0; attempt < DEFAULT_MAX_RETRIES; attempt++) {
      lastResponse = provider.complete(settings, request, streamingHandler);
      if (lastResponse.isPresent()) {
        return lastResponse;
      }
    }
    return lastResponse;
  }

  private Optional<LlmProvider> selectProvider(String providerName) {
    return selectProvider(providerName, false);
  }

  private Optional<LlmProvider> selectProvider(String providerName, boolean localModelPreferred) {
    if (localModelPreferred) {
      Optional<LlmProvider> localProvider = llmProviders.stream()
          .filter(provider -> provider.supports("local"))
          .findFirst();
      if (localProvider.isPresent()) {
        return localProvider;
      }
    }
    return llmProviders.stream()
        .filter(provider -> provider.supports(providerName))
        .findFirst();
  }

  private static boolean isConfigured(AiProviderSettingsService.ProviderSettings settings) {
    return settings != null
        && settings.active()
        && settings.apiBaseUrl() != null
        && !settings.apiBaseUrl().isBlank()
        && settings.apiKey() != null
        && !settings.apiKey().isBlank();
  }

  private static ExecutionOptions resolveExecutionOptions(
      AiProviderSettingsService.ProviderSettings settings,
      Map<String, Object> parameters,
      boolean streamingRequested,
      ContextAssembly contextAssembly) {
    String configuredProvider = settings == null || settings.providerName() == null || settings.providerName().isBlank()
        ? "UNCONFIGURED"
        : settings.providerName();
    String configuredModel = settings == null || settings.modelName() == null || settings.modelName().isBlank()
        ? "deterministic-fallback"
        : settings.modelName();
    String providerOverride = stringParameter(parameters, "providerName");
    String modelOverride = stringParameter(parameters, "modelName");
    boolean localPreferred = booleanParameter(parameters, "localModelPreferred")
        || booleanParameter(parameters, "preferLocalModel")
        || configuredProvider.toLowerCase(Locale.ROOT).startsWith("local");
    long timeoutMs = longParameter(parameters, "timeoutMs", DEFAULT_TIMEOUT_MS);
    boolean cancellationRequested = booleanParameter(parameters, "cancel")
        || booleanParameter(parameters, "cancelRequested");
    String cancellationKey = stringParameter(parameters, "cancellationKey");
    String restrictedContentNotice = contextAssembly.warnings().stream()
        .filter(warning -> warning.toLowerCase(Locale.ROOT).contains("restricted"))
        .findFirst()
        .orElse(null);
    return new ExecutionOptions(
        localPreferred ? "local-stub" : providerOverride == null ? configuredProvider : providerOverride,
        modelOverride == null ? configuredModel : modelOverride,
        Math.max(1L, timeoutMs),
        streamingRequested || booleanParameter(parameters, "stream"),
        localPreferred,
        cancellationRequested,
        cancellationKey,
        restrictedContentNotice);
  }

  private static String resolveFallbackModel(
      AiProviderSettingsService.ProviderSettings settings,
      Map<String, Object> parameters) {
    Object explicitFallback = parameters == null ? null : parameters.get("fallbackModel");
    if (explicitFallback instanceof String fallback && !fallback.isBlank()) {
      return fallback.trim();
    }
    String modelName = settings.modelName();
    String normalized = modelName == null ? "" : modelName.toLowerCase(Locale.ROOT);
    if (normalized.contains("small")) {
      return modelName;
    }
    return modelName == null || modelName.isBlank() ? "mistral-small" : "mistral-small";
  }

  private static int resolveMaxTokens(
      EnterpriseAiOperation operation,
      ContextAssembly contextAssembly) {
    int reserve = contextAssembly.tokenBudget().getOrDefault("reserveCompletion", 256);
    return switch (operation) {
      case COMPARE -> Math.max(reserve, 420);
      case SUMMARIZE, EXPLAIN -> Math.max(reserve, 320);
      case EXTRACT, VALIDATE -> Math.max(reserve, 280);
      case SEARCH, ASK -> Math.max(reserve, 240);
    };
  }

  private static LlmCompletionResponse fallbackResponse(
      AiProviderSettingsService.ProviderSettings settings,
      String fallbackAnswer,
      String warning,
      ExecutionOptions executionOptions,
      ContextAssembly contextAssembly) {
    String providerName = settings == null ? "UNCONFIGURED" : settings.providerName();
    String modelName = settings == null ? "deterministic-fallback" : settings.modelName();
    int completionTokens = fallbackAnswer == null ? 0 : Math.max(1, fallbackAnswer.length() / 4);
    return new LlmCompletionResponse(
        fallbackAnswer,
        executionOptions.providerName() == null ? providerName : executionOptions.providerName(),
        executionOptions.primaryModel() == null ? modelName : executionOptions.primaryModel(),
        0,
        completionTokens,
        completionTokens,
        true,
        executionOptions.streamingRequested(),
        warning.toLowerCase(Locale.ROOT).contains("timeout"),
        executionOptions.cancellationRequested(),
        0L,
        executionOptions.restrictedContentNotice(),
        List.of(executionOptions.streamingRequested() ? "start" : "complete", "complete"),
        appendWarning(contextAssembly.warnings(), warning));
  }

  private static List<String> appendWarning(List<String> warnings, String extraWarning) {
    if (warnings == null || warnings.isEmpty()) {
      return List.of(extraWarning);
    }
    return java.util.stream.Stream.concat(warnings.stream(), java.util.stream.Stream.of(extraWarning))
        .toList();
  }

  private static LlmCompletionResponse enrich(
      LlmCompletionResponse response,
      ContextAssembly contextAssembly,
      ExecutionOptions executionOptions,
      boolean fallbackUsed,
      BufferingStreamingHandler streamingHandler) {
    return new LlmCompletionResponse(
        response.content(),
        response.providerName(),
        response.modelName(),
        response.promptTokens(),
        response.completionTokens(),
        response.totalTokens(),
        fallbackUsed || response.fallbackUsed(),
        response.streamed() || executionOptions.streamingRequested(),
        response.timedOut() || streamingHandler.isTimedOut(),
        response.cancelled() || executionOptions.cancellationRequested(),
        response.providerLatencyMs(),
        executionOptions.restrictedContentNotice(),
        streamingHandler.events().isEmpty() ? response.streamEvents() : streamingHandler.events(),
        mergeWarnings(contextAssembly.warnings(), response.warnings(), fallbackUsed));
  }

  private static List<String> mergeWarnings(
      List<String> contextWarnings,
      List<String> responseWarnings,
      boolean fallbackUsed) {
    List<String> warnings = new ArrayList<>();
    if (contextWarnings != null) {
      warnings.addAll(contextWarnings);
    }
    if (responseWarnings != null) {
      warnings.addAll(responseWarnings);
    }
    if (fallbackUsed) {
      warnings.add("Primary model failed; fallback model was used.");
    }
    return warnings.stream().distinct().toList();
  }

  private static String stringParameter(Map<String, Object> parameters, String key) {
    if (parameters == null) {
      return null;
    }
    Object value = parameters.get(key);
    return value instanceof String string && !string.isBlank() ? string.trim() : null;
  }

  private static boolean booleanParameter(Map<String, Object> parameters, String key) {
    if (parameters == null) {
      return false;
    }
    Object value = parameters.get(key);
    return value instanceof Boolean bool ? bool : value instanceof String string && Boolean.parseBoolean(string);
  }

  private static long longParameter(Map<String, Object> parameters, String key, long defaultValue) {
    if (parameters == null) {
      return defaultValue;
    }
    Object value = parameters.get(key);
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String string) {
      try {
        return Long.parseLong(string.trim());
      } catch (NumberFormatException ignored) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  record ExecutionOptions(
      String providerName,
      String primaryModel,
      long timeoutMs,
      boolean streamingRequested,
      boolean localModelPreferred,
      boolean cancellationRequested,
      String cancellationKey,
      String restrictedContentNotice) {
  }

  static final class BufferingStreamingHandler implements LlmStreamingHandler {

    private final long startedAt = System.currentTimeMillis();
    private final long timeoutMs;
    private final boolean cancelled;
    private final List<String> events = new ArrayList<>();

    BufferingStreamingHandler(long timeoutMs, boolean cancelled) {
      this.timeoutMs = timeoutMs;
      this.cancelled = cancelled;
    }

    @Override
    public void onStart() {
      events.add("start");
    }

    @Override
    public void onDelta(String delta) {
      events.add("delta");
    }

    @Override
    public void onComplete(String content) {
      events.add("complete");
    }

    @Override
    public void onError(String message) {
      events.add("error");
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isTimedOut() {
      return System.currentTimeMillis() - startedAt > timeoutMs;
    }

    List<String> events() {
      return List.copyOf(events);
    }
  }
}
