package com.ikms.ai.provider;

import com.ikms.ai.AiProviderSettingsService;
import java.util.Optional;

public interface LlmProvider {

  boolean supports(String providerName);

  Optional<LlmCompletionResponse> complete(
      AiProviderSettingsService.ProviderSettings settings,
      LlmCompletionRequest request,
      LlmStreamingHandler streamingHandler);
}
