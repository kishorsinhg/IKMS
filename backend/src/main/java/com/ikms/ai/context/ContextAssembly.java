package com.ikms.ai.context;

import java.util.List;
import java.util.Map;

public record ContextAssembly(
    String providerName,
    String modelName,
    String systemPrompt,
    String userPrompt,
    List<ContextPromptMessage> messages,
    List<ContextEvidenceItem> evidence,
    List<ContextPromptMessage> conversationHistory,
    Map<String, Object> metadata,
    Map<String, Integer> tokenBudget,
    List<String> warnings) {
}
