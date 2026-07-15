package com.ikms.ai;

import com.ikms.ai.context.ContextAssembly;
import com.ikms.ai.context.ContextEvidenceItem;
import com.ikms.ai.context.ContextPromptMessage;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.security.PiiMaskingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EnterpriseGuardrailService {

  private final PromptInjectionDetectionService promptInjectionDetectionService;
  private final PiiMaskingService piiMaskingService;

  public EnterpriseGuardrailService(
      PromptInjectionDetectionService promptInjectionDetectionService,
      PiiMaskingService piiMaskingService) {
    this.promptInjectionDetectionService = promptInjectionDetectionService;
    this.piiMaskingService = piiMaskingService;
  }

  public GuardrailOutcome apply(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      ContextAssembly contextAssembly,
      List<EnterpriseAiContracts.RetrievedEvidence> evidence) {
    List<String> warnings = new ArrayList<>(contextAssembly.warnings());
    List<EnterpriseAiContracts.RetrievedEvidence> safeEvidence = new ArrayList<>();
    List<ContextEvidenceItem> safeContextEvidence = new ArrayList<>();
    boolean promptInjectionDetected = false;
    boolean piiMasked = false;
    boolean permissionTrimmed = false;
    boolean restrictedContentDetected = warnings.stream()
        .anyMatch(warning -> warning.toLowerCase(java.util.Locale.ROOT).contains("restricted"));

    for (int index = 0; index < evidence.size(); index++) {
      EnterpriseAiContracts.RetrievedEvidence item = evidence.get(index);
      String combined = (item.title() == null ? "" : item.title()) + "\n" + (item.excerpt() == null ? "" : item.excerpt());
      if (containsRestrictedMarker(combined)) {
        restrictedContentDetected = true;
        permissionTrimmed = true;
        warnings.add("Restricted documents were excluded from retrieval and prompt context.");
        continue;
      }
      PromptInjectionDetectionService.DetectionResult detection = promptInjectionDetectionService.inspect(combined);
      if (detection.detected()) {
        promptInjectionDetected = true;
        warnings.add("Prompt injection marker removed from retrieved evidence: " + detection.marker() + ".");
        continue;
      }

      String maskedTitle = piiMaskingService.trimFreeText(item.title(), request.permissions());
      String maskedExcerpt = piiMaskingService.trimFreeText(item.excerpt(), request.permissions());
      piiMasked = piiMasked
          || !Objects.equals(maskedTitle, item.title())
          || !Objects.equals(maskedExcerpt, item.excerpt());
      permissionTrimmed = permissionTrimmed
          || !Objects.equals(maskedTitle, item.title())
          || !Objects.equals(maskedExcerpt, item.excerpt());
      EnterpriseAiContracts.RetrievedEvidence safeItem = new EnterpriseAiContracts.RetrievedEvidence(
          item.sourceType(),
          item.sourceId(),
          maskedTitle,
          maskedExcerpt,
          item.citation(),
          item.pageNumber(),
          item.sourceSection(),
          item.retrievalPath(),
          item.citationQuality(),
          item.occurredAt());
      safeEvidence.add(safeItem);
      if (index < contextAssembly.evidence().size()) {
        ContextEvidenceItem originalContextItem = contextAssembly.evidence().get(index);
        safeContextEvidence.add(new ContextEvidenceItem(
            safeItem.sourceType(),
            safeItem.sourceId(),
            safeItem.title(),
            safeItem.excerpt(),
            safeItem.sourceSection(),
            safeItem.pageNumber(),
            safeItem.retrievalPath(),
            safeItem.citationQuality(),
            safeItem.occurredAt(),
            originalContextItem.estimatedTokens()));
      }
    }

    String maskedUserPrompt = piiMaskingService.trimFreeText(contextAssembly.userPrompt(), request.permissions());
    piiMasked = piiMasked || !Objects.equals(maskedUserPrompt, contextAssembly.userPrompt());
    permissionTrimmed = permissionTrimmed || !Objects.equals(maskedUserPrompt, contextAssembly.userPrompt());
    List<ContextPromptMessage> safeMessages = new ArrayList<>();
    safeMessages.add(new ContextPromptMessage("system", contextAssembly.systemPrompt(), estimateTokens(contextAssembly.systemPrompt())));
    safeMessages.addAll(contextAssembly.conversationHistory());
    safeMessages.add(new ContextPromptMessage("user", maskedUserPrompt, estimateTokens(maskedUserPrompt)));

    boolean tokenLimitApplied = safeMessages.stream().mapToInt(ContextPromptMessage::estimatedTokens).sum()
        > contextAssembly.tokenBudget().getOrDefault("total", Integer.MAX_VALUE);
    if (piiMasked) {
      warnings.add("PII masking was applied to provider-visible context.");
    }
    if (tokenLimitApplied) {
      warnings.add("Context payload exceeded the token budget and requires additional truncation before provider execution.");
    }
    if (safeEvidence.isEmpty() && !evidence.isEmpty()) {
      warnings.add("All retrieved evidence was removed by guardrail enforcement; manual review is required.");
    }
    boolean insufficientEvidence = safeEvidence.isEmpty();
    if (insufficientEvidence) {
      warnings.add("Insufficient evidence to answer.");
    }

    ContextAssembly guardedContext = new ContextAssembly(
        contextAssembly.providerName(),
        contextAssembly.modelName(),
        contextAssembly.systemPrompt(),
        maskedUserPrompt,
        List.copyOf(safeMessages),
        List.copyOf(safeContextEvidence),
        contextAssembly.conversationHistory(),
        contextAssembly.metadata(),
        contextAssembly.tokenBudget(),
        List.copyOf(warnings));

    return new GuardrailOutcome(
        guardedContext,
        List.copyOf(safeEvidence),
        List.copyOf(warnings),
        piiMasked,
        permissionTrimmed,
        promptInjectionDetected,
        tokenLimitApplied,
        restrictedContentDetected
            ? "Restricted documents were excluded from retrieval and prompt context."
            : null,
        insufficientEvidence);
  }

  private static int estimateTokens(String content) {
    if (content == null || content.isBlank()) {
      return 0;
    }
    return Math.max(1, content.length() / 4);
  }

  public record GuardrailOutcome(
      ContextAssembly contextAssembly,
      List<EnterpriseAiContracts.RetrievedEvidence> evidence,
      List<String> warnings,
      boolean piiMasked,
      boolean permissionTrimmed,
      boolean promptInjectionDetected,
      boolean tokenLimitApplied,
      String restrictedContentNotice,
      boolean insufficientEvidence) {
  }

  private static boolean containsRestrictedMarker(String value) {
    String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("restricted")
        || normalized.contains("confidential")
        || normalized.contains("redacted only");
  }
}
