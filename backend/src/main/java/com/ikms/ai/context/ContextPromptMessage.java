package com.ikms.ai.context;

public record ContextPromptMessage(
    String role,
    String content,
    int estimatedTokens) {
}
