package com.ikms.ai.context;

import com.ikms.ai.AiConversationMessage;
import com.ikms.ai.AiConversationMessageRepository;
import com.ikms.ai.AiProviderSettingsService;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.RetrievalCoordinator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ContextBuilderService {

  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiConversationMessageRepository aiConversationMessageRepository;
  private final TokenBudgetManager tokenBudgetManager;

  public ContextBuilderService(
      AiProviderSettingsService aiProviderSettingsService,
      AiConversationMessageRepository aiConversationMessageRepository,
      TokenBudgetManager tokenBudgetManager) {
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiConversationMessageRepository = aiConversationMessageRepository;
    this.tokenBudgetManager = tokenBudgetManager;
  }

  public ContextAssembly build(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryPlan plan,
      RetrievalCoordinator.RetrievalResult retrieval,
      UUID conversationId) {
    AiProviderSettingsService.ProviderSettings providerSettings = aiProviderSettingsService.current();
    Map<String, Object> metadata = buildMetadata(request, plan, retrieval, providerSettings);
    List<ContextPromptMessage> conversationHistory = loadConversationHistory(conversationId);
    TokenBudgetManager.BudgetAllocation budget = tokenBudgetManager.allocate(
        plan,
        request.prompt(),
        conversationHistory.stream().map(ContextPromptMessage::content).toList(),
        retrieval.evidence(),
        metadata);

    String systemPrompt = buildSystemPrompt(request, plan, providerSettings);
    List<ContextPromptMessage> trimmedConversation = trimConversation(conversationHistory, budget.conversationBudget());
    List<ContextEvidenceItem> trimmedEvidence = trimEvidence(retrieval.evidence(), budget.evidenceBudget());
    String userPrompt = buildUserPrompt(request, trimmedEvidence, metadata);

    List<ContextPromptMessage> messages = new ArrayList<>();
    messages.add(new ContextPromptMessage("system", systemPrompt, tokenBudgetManager.estimateTokens(systemPrompt)));
    messages.addAll(trimmedConversation);
    messages.add(new ContextPromptMessage("user", userPrompt, tokenBudgetManager.estimateTokens(userPrompt)));

    return new ContextAssembly(
        providerSettings.providerName(),
        providerSettings.modelName(),
        systemPrompt,
        userPrompt,
        List.copyOf(messages),
        List.copyOf(trimmedEvidence),
        List.copyOf(trimmedConversation),
        Map.copyOf(metadata),
        Map.of(
            "total", budget.totalBudget(),
            "system", budget.systemBudget(),
            "user", budget.userBudget(),
            "conversation", budget.conversationBudget(),
            "evidence", budget.evidenceBudget(),
            "metadata", budget.metadataBudget(),
            "reserveCompletion", budget.reserveCompletionBudget()),
        retrieval.warnings());
  }

  private Map<String, Object> buildMetadata(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryPlan plan,
      RetrievalCoordinator.RetrievalResult retrieval,
      AiProviderSettingsService.ProviderSettings providerSettings) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("clientId", request.clientId());
    metadata.put("operation", request.operation().name());
    metadata.put("scope", plan.scope().name());
    metadata.put("reasoningMode", plan.reasoningMode());
    metadata.put("retrievalMode", retrieval.retrievalMode());
    metadata.put("retrievalModes", plan.retrievalModes());
    metadata.put("sourceTypes", plan.sourceTypes().stream().map(Enum::name).toList());
    metadata.put("documentTypes", plan.documentTypes());
    metadata.put("sortOrder", plan.sortOrder().name());
    metadata.put("versionPreference", plan.versionPreference().name());
    metadata.put("requiredEvidenceGranularity", plan.requiredEvidenceGranularity().name());
    metadata.put("businessReferenceFields", Map.of(
        "policyNumber", plan.businessReferenceFields().policyNumber() == null ? "" : plan.businessReferenceFields().policyNumber(),
        "claimNumber", plan.businessReferenceFields().claimNumber() == null ? "" : plan.businessReferenceFields().claimNumber(),
        "insurer", plan.businessReferenceFields().insurer() == null ? "" : plan.businessReferenceFields().insurer(),
        "policyType", plan.businessReferenceFields().policyType() == null ? "" : plan.businessReferenceFields().policyType(),
        "effectiveDate", plan.businessReferenceFields().effectiveDate() == null ? "" : plan.businessReferenceFields().effectiveDate(),
        "expiryDate", plan.businessReferenceFields().expiryDate() == null ? "" : plan.businessReferenceFields().expiryDate(),
        "renewalDate", plan.businessReferenceFields().renewalDate() == null ? "" : plan.businessReferenceFields().renewalDate(),
        "brokerReference", plan.businessReferenceFields().brokerReference() == null ? "" : plan.businessReferenceFields().brokerReference(),
        "externalReference", plan.businessReferenceFields().externalReference() == null ? "" : plan.businessReferenceFields().externalReference()));
    metadata.put("dateRange", Map.of(
        "from", plan.dateRange().from() == null ? "" : plan.dateRange().from(),
        "to", plan.dateRange().to() == null ? "" : plan.dateRange().to()));
    metadata.put("sourceIds", request.sourceIds());
    metadata.put("parameterKeys", request.parameters().keySet());
    metadata.put("providerExecution", Map.of(
        "streamingRequested", booleanParameter(request.parameters(), "stream"),
        "timeoutMs", longParameter(request.parameters(), "timeoutMs", 15_000L),
        "localModelPreferred", booleanParameter(request.parameters(), "localModelPreferred")
            || booleanParameter(request.parameters(), "preferLocalModel"),
        "cancellationRequested", booleanParameter(request.parameters(), "cancel")
            || booleanParameter(request.parameters(), "cancelRequested")));
    metadata.put("provider", providerSettings.providerName());
    metadata.put("model", providerSettings.modelName());
    metadata.put("evidenceCount", retrieval.evidence().size());
    metadata.put("restrictedContentNotice", retrieval.warnings().stream()
        .filter(warning -> warning.toLowerCase(Locale.ROOT).contains("restricted"))
        .findFirst()
        .orElse(""));
    return metadata;
  }

  private List<ContextPromptMessage> loadConversationHistory(UUID conversationId) {
    if (conversationId == null) {
      return List.of();
    }
    return aiConversationMessageRepository.findTop12ByConversationIdOrderByCreatedAtDesc(conversationId).stream()
        .sorted(Comparator.comparing(AiConversationMessage::getCreatedAt))
        .map(message -> new ContextPromptMessage(
            normalizeRole(message.getRole()),
            message.getContent() == null ? "" : message.getContent(),
            message.getTotalTokens() != null
                ? message.getTotalTokens()
                : tokenBudgetManager.estimateTokens(message.getContent())))
        .toList();
  }

  private List<ContextPromptMessage> trimConversation(
      List<ContextPromptMessage> history,
      int tokenBudget) {
    if (history.isEmpty()) {
      return List.of();
    }
    List<ContextPromptMessage> selected = new ArrayList<>();
    int used = 0;
    for (int index = history.size() - 1; index >= 0; index--) {
      ContextPromptMessage message = history.get(index);
      if (!selected.isEmpty() && used + message.estimatedTokens() > tokenBudget) {
        break;
      }
      selected.addFirst(message);
      used += message.estimatedTokens();
    }
    return List.copyOf(selected);
  }

  private List<ContextEvidenceItem> trimEvidence(
      List<EnterpriseAiContracts.RetrievedEvidence> evidence,
      int tokenBudget) {
    if (evidence.isEmpty()) {
      return List.of();
    }
    List<ContextEvidenceItem> selected = new ArrayList<>();
    Set<String> seenEvidenceKeys = new HashSet<>();
    int used = 0;
    for (EnterpriseAiContracts.RetrievedEvidence item : evidence) {
      if (!seenEvidenceKeys.add(evidenceKey(item))) {
        continue;
      }
      int estimatedTokens = tokenBudgetManager.estimateTokens(item.title() + "\n" + item.excerpt());
      if (!selected.isEmpty() && used + estimatedTokens > tokenBudget) {
        break;
      }
      selected.add(new ContextEvidenceItem(
          item.sourceType(),
          item.sourceId(),
          item.title(),
          item.excerpt(),
          item.sourceSection(),
          item.pageNumber(),
          item.retrievalPath(),
          item.citationQuality(),
          item.occurredAt(),
          estimatedTokens));
      used += estimatedTokens;
    }
    return List.copyOf(selected);
  }

  private String buildSystemPrompt(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryPlan plan,
      AiProviderSettingsService.ProviderSettings providerSettings) {
    String restrictedNotice = request.parameters() == null
        ? ""
        : booleanParameter(request.parameters(), "restrictedContentPresent")
            ? "\nRestricted documents or redacted content were excluded from provider-visible context."
            : "";
    return """
        You are the IKMS enterprise knowledge orchestration layer.
        Operation: %s
        Reasoning mode: %s
        Provider target: %s / %s
        Answer only from authorized evidence and surfaced metadata.
        Do not invent facts, cite unsupported conclusions, or bypass permission boundaries.
        If evidence is incomplete, conflicting, or missing, state that clearly and recommend manual review.
        %s
        """.formatted(
        request.operation().name(),
        plan.reasoningMode(),
        providerSettings.providerName(),
        providerSettings.modelName(),
        restrictedNotice);
  }

  private String buildUserPrompt(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      List<ContextEvidenceItem> evidence,
      Map<String, Object> metadata) {
    String evidenceBlock = evidence.stream()
        .map(item -> """
            [%s] %s
            Location: %s
            Retrieval: %s
            Excerpt: %s
            """.formatted(
            item.sourceType(),
            item.title(),
            formatLocation(item),
            item.retrievalPath(),
            item.excerpt()))
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("No evidence retrieved.");

    return """
        User request:
        %s

        Workspace context:
        %s

        Retrieved evidence:
        %s
        """.formatted(
        request.prompt() == null ? "" : request.prompt().trim(),
        metadata,
        evidenceBlock);
  }

  private static String formatLocation(ContextEvidenceItem item) {
    if (item.pageNumber() != null) {
      return "Page " + item.pageNumber();
    }
    if (item.section() != null && !item.section().isBlank()) {
      return item.section();
    }
    return item.sourceType();
  }

  private static String normalizeRole(String role) {
    if (role == null || role.isBlank()) {
      return "assistant";
    }
    String normalized = role.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "system", "user", "assistant" -> normalized;
      default -> "assistant";
    };
  }

  private static String evidenceKey(EnterpriseAiContracts.RetrievedEvidence item) {
    return String.join("|",
        item.sourceType() == null ? "" : item.sourceType(),
        item.sourceId() == null ? "" : item.sourceId().toString(),
        item.pageNumber() == null ? "" : item.pageNumber().toString(),
        item.sourceSection() == null ? "" : item.sourceSection().trim().toLowerCase(Locale.ROOT),
        item.excerpt() == null ? "" : item.excerpt().trim().toLowerCase(Locale.ROOT));
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
}
