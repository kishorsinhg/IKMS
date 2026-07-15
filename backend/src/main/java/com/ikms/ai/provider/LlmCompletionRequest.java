package com.ikms.ai.provider;

import com.ikms.ai.context.ContextPromptMessage;
import java.util.List;
import java.util.Map;

public record LlmCompletionRequest(
    String providerName,
    String primaryModel,
    String fallbackModel,
    List<ContextPromptMessage> messages,
    int maxTokens,
    double temperature,
    boolean streamingRequested,
    long timeoutMs,
    boolean localModelAllowed,
    String cancellationKey,
    Map<String, Object> metadata) {
}
