package com.ikms.ai.provider;

import java.util.List;

public record LlmCompletionResponse(
    String content,
    String providerName,
    String modelName,
    int promptTokens,
    int completionTokens,
    int totalTokens,
    boolean fallbackUsed,
    boolean streamed,
    boolean timedOut,
    boolean cancelled,
    long providerLatencyMs,
    String restrictedContentNotice,
    List<String> streamEvents,
    List<String> warnings) {
}
