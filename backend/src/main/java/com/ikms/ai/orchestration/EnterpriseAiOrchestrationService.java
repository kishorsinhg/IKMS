package com.ikms.ai.orchestration;

import com.ikms.ai.AiContracts;
import com.ikms.ai.AiConversation;
import com.ikms.ai.AiConversationMessage;
import com.ikms.ai.AiConversationMessageRepository;
import com.ikms.ai.AiConversationRepository;
import com.ikms.ai.AiInteraction;
import com.ikms.ai.AiInteractionRepository;
import com.ikms.ai.AiOrchestrationMetric;
import com.ikms.ai.AiOrchestrationMetricRepository;
import com.ikms.ai.CitationAuditService;
import com.ikms.ai.ClientQuestionAnsweringService;
import com.ikms.ai.EnterpriseGuardrailService;
import com.ikms.ai.context.ContextAssembly;
import com.ikms.ai.context.ContextBuilderService;
import com.ikms.governance.GovernancePolicyService;
import com.ikms.observability.RequestContextHolder;
import com.ikms.ai.provider.LlmCompletionResponse;
import com.ikms.ai.provider.LlmOrchestrationService;
import com.ikms.security.ActorAttributeContext;
import com.ikms.security.domain.Permission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnterpriseAiOrchestrationService {

  private final IntentDetectionService intentDetectionService;
  private final QueryPlanningService queryPlanningService;
  private final RetrievalCoordinator retrievalCoordinator;
  private final CitationBuilderService citationBuilderService;
  private final GroundingValidationService groundingValidationService;
  private final ClientQuestionAnsweringService clientQuestionAnsweringService;
  private final ContextBuilderService contextBuilderService;
  private final LlmOrchestrationService llmOrchestrationService;
  private final EnterpriseGuardrailService enterpriseGuardrailService;
  private final CitationAuditService citationAuditService;
  private final GovernancePolicyService governancePolicyService;
  private final AiConversationRepository aiConversationRepository;
  private final AiConversationMessageRepository aiConversationMessageRepository;
  private final AiInteractionRepository aiInteractionRepository;
  private final AiOrchestrationMetricRepository aiOrchestrationMetricRepository;

  public EnterpriseAiOrchestrationService(
      IntentDetectionService intentDetectionService,
      QueryPlanningService queryPlanningService,
      RetrievalCoordinator retrievalCoordinator,
      CitationBuilderService citationBuilderService,
      GroundingValidationService groundingValidationService,
      ClientQuestionAnsweringService clientQuestionAnsweringService,
      ContextBuilderService contextBuilderService,
      LlmOrchestrationService llmOrchestrationService,
      EnterpriseGuardrailService enterpriseGuardrailService,
      CitationAuditService citationAuditService,
      GovernancePolicyService governancePolicyService,
      AiConversationRepository aiConversationRepository,
      AiConversationMessageRepository aiConversationMessageRepository,
      AiInteractionRepository aiInteractionRepository,
      AiOrchestrationMetricRepository aiOrchestrationMetricRepository) {
    this.intentDetectionService = intentDetectionService;
    this.queryPlanningService = queryPlanningService;
    this.retrievalCoordinator = retrievalCoordinator;
    this.citationBuilderService = citationBuilderService;
    this.groundingValidationService = groundingValidationService;
    this.clientQuestionAnsweringService = clientQuestionAnsweringService;
    this.contextBuilderService = contextBuilderService;
    this.llmOrchestrationService = llmOrchestrationService;
    this.enterpriseGuardrailService = enterpriseGuardrailService;
    this.citationAuditService = citationAuditService;
    this.governancePolicyService = governancePolicyService;
    this.aiConversationRepository = aiConversationRepository;
    this.aiConversationMessageRepository = aiConversationMessageRepository;
    this.aiInteractionRepository = aiInteractionRepository;
    this.aiOrchestrationMetricRepository = aiOrchestrationMetricRepository;
  }

  public EnterpriseAiContracts.EnterpriseAiResponse orchestrate(
      UUID clientId,
      EnterpriseAiOperation operation,
      String prompt,
      Set<Permission> permissions,
      UUID actorUserId,
      UUID conversationId,
      List<UUID> sourceIds,
      Map<String, Object> parameters) {
    try (RequestContextHolder.Scope ignored = RequestContextHolder.withGenerated(RequestContextHolder.RETRIEVAL_ID)) {
      long startedAt = System.currentTimeMillis();
      EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
          clientId,
          operation,
          prompt,
          actorUserId,
          permissions,
          conversationId,
          sourceIds == null ? List.of() : List.copyOf(sourceIds),
          parameters == null ? Map.of() : Map.copyOf(parameters));

      AiConversation conversation = resolveConversation(request);
      EnterpriseAiContracts.DetectedIntent intent = intentDetectionService.detect(request);
      EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(request, intent);
      RetrievalCoordinator.RetrievalResult retrieval = retrievalCoordinator.retrieve(request, plan);
      ContextAssembly contextAssembly = contextBuilderService.build(request, plan, retrieval, conversation.getId());
      EnterpriseGuardrailService.GuardrailOutcome guardrailOutcome = enterpriseGuardrailService.apply(
          request,
          contextAssembly,
          retrieval.evidence());
      RetrievalCoordinator.RetrievalResult guardedRetrieval = new RetrievalCoordinator.RetrievalResult(
          guardrailOutcome.evidence(),
          retrieval.retrievalMode(),
          guardrailOutcome.warnings());

      if (operation == EnterpriseAiOperation.ASK) {
        return orchestrateAsk(request, conversation, guardedRetrieval, guardrailOutcome.contextAssembly(), guardrailOutcome, startedAt);
      }

      List<EnterpriseAiContracts.CitationReference> citations = citationBuilderService.buildCitations(guardedRetrieval.evidence());
      List<EnterpriseAiContracts.EvidenceReference> evidenceReferences = citationBuilderService.buildEvidenceReferences(citations);
      List<EnterpriseAiContracts.SourceReference> sourceReferences = citationBuilderService.buildSourceReferences(citations);
      EnterpriseAiContracts.GroundingValidation grounding = groundingValidationService.validate(citations, guardedRetrieval.warnings());
      AiInteraction interaction = createInteraction(request, conversation.getId(), guardedRetrieval.retrievalMode(), grounding, startedAt);
      try (RequestContextHolder.Scope interactionScope = RequestContextHolder.with(RequestContextHolder.AI_INTERACTION_ID, interaction.getId().toString())) {
        String deterministicAnswer = buildDeterministicAnswer(operation, guardedRetrieval.evidence());
        boolean approvedModel = governancePolicyService.isApprovedModel(
            guardrailOutcome.contextAssembly().providerName(),
            guardrailOutcome.contextAssembly().modelName());
        LlmCompletionResponse completion = approvedModel
            ? llmOrchestrationService.complete(
                operation,
                guardrailOutcome.contextAssembly(),
                deterministicAnswer,
                request.parameters())
            : new LlmCompletionResponse(
                deterministicAnswer,
                guardrailOutcome.contextAssembly().providerName(),
                guardrailOutcome.contextAssembly().modelName(),
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                0L,
                null,
                List.of("model-not-approved"),
                List.of("The configured AI model is not in the approved model registry; deterministic fallback was used."));
        String finalAnswer = guardrailOutcome.insufficientEvidence() || !grounding.grounded()
            ? "Insufficient evidence to answer."
            : completion.content();
        String finalStatus = guardrailOutcome.insufficientEvidence()
            ? "InsufficientEvidence"
            : completion.fallbackUsed()
                ? "Fallback"
                : grounding.grounded()
                    ? "Ready"
                    : "NeedsReview";
        interaction.setAnswer(finalAnswer);
        interaction.setStatus(finalStatus);
        interaction.setProviderName(completion.providerName());
        interaction.setModelName(completion.modelName());
        interaction.setPromptTokens(completion.promptTokens());
        interaction.setCompletionTokens(completion.completionTokens());
        interaction.setTotalTokens(completion.totalTokens());
        interaction.setFallbackUsed(completion.fallbackUsed());
        interaction.setTotalLatencyMs(System.currentTimeMillis() - startedAt);
        interaction.setWarningSummary(String.join(" | ", mergeWarnings(grounding.warnings(), completion.warnings())));
        interaction.setCitedSources(citations.stream()
            .map(citation -> citation.title() + " [" + citation.sourceType() + "]")
            .reduce((left, right) -> left + "; " + right)
            .orElse(""));
        aiInteractionRepository.save(interaction);
        persistMessage(
            conversation.getId(),
            "user",
            guardrailOutcome.contextAssembly().userPrompt(),
            guardrailOutcome.contextAssembly().providerName(),
            guardrailOutcome.contextAssembly().modelName());
        persistMessage(
            conversation.getId(),
            "assistant",
            finalAnswer,
            completion.providerName(),
            completion.modelName());
        citationAuditService.record(
            interaction.getId(),
            conversation.getId(),
            request.clientId(),
            guardedRetrieval.retrievalMode(),
            citations,
            guardedRetrieval.evidence(),
            guardrailOutcome);
        saveMetric(interaction, conversation.getId(), operation, guardedRetrieval, grounding, completion, startedAt);

        return new EnterpriseAiContracts.EnterpriseAiResponse(
            operation,
            conversation.getId(),
            interaction.getId(),
            finalStatus,
            finalAnswer,
            citations,
            evidenceReferences,
            sourceReferences,
            mergeWarnings(grounding.warnings(), completion.warnings()),
            new EnterpriseAiContracts.MetricsSnapshot(
                System.currentTimeMillis() - startedAt,
                guardedRetrieval.evidence().size(),
                guardedRetrieval.retrievalMode(),
                completion.fallbackUsed()),
            grounding,
            buildStructuredPayload(operation, request, plan, guardedRetrieval.evidence(), guardrailOutcome.contextAssembly(), guardrailOutcome));
      }
    }
  }

  private EnterpriseAiContracts.EnterpriseAiResponse orchestrateAsk(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      AiConversation conversation,
      RetrievalCoordinator.RetrievalResult retrieval,
      ContextAssembly contextAssembly,
      EnterpriseGuardrailService.GuardrailOutcome guardrailOutcome,
      long startedAt) {
    AiContracts.AskClientResponse answer = clientQuestionAnsweringService.ask(
        request.clientId(),
        request.prompt(),
        request.permissions(),
        request.actorUserId(),
        ActorAttributeContext.fromParameters(request.parameters()).asMap());
    aiInteractionRepository.findById(answer.interactionId()).ifPresent(interaction -> {
      interaction.setConversationId(conversation.getId());
      interaction.setOperationType(request.operation().name());
      interaction.setRetrievalMode(answer.retrievalMode());
      interaction.setWarningSummary(String.join(" | ", answer.warnings()));
      interaction.setTotalLatencyMs(System.currentTimeMillis() - startedAt);
      interaction.setCitationCoverage(answer.citations().isEmpty() ? 0d : 1d);
      interaction.setGroundingScore(answer.citations().isEmpty() ? 0d : 0.98d);
      aiInteractionRepository.save(interaction);
      LlmCompletionResponse completion = new LlmCompletionResponse(
          answer.answer(),
          contextAssembly.providerName(),
          contextAssembly.modelName(),
          0,
          estimateTokens(answer.answer()),
          estimateTokens(answer.answer()),
          false,
          false,
          false,
          false,
          0L,
          null,
          List.of("complete"),
          List.copyOf(answer.warnings()));
      saveMetric(interaction, conversation.getId(), request.operation(), retrieval, groundingValidationService.validate(
          answer.citations().stream()
              .map(citation -> new EnterpriseAiContracts.CitationReference(
                  citation.sourceType(),
                  citation.sourceId(),
                  citation.title(),
                  citation.excerpt(),
                  citation.pageNumber(),
                  null,
                  citation.sourceSection(),
                  citation.pageNumber() != null || citation.sourceSection() != null ? "HIGH" : "LOW",
                  citation.pageNumber() != null
                      ? "document:" + citation.sourceId() + ":page:" + citation.pageNumber()
                      : "metadata:" + citation.sourceType() + ":" + citation.sourceId() + ":" + citation.sourceSection(),
                  answer.retrievalMode()))
              .toList(),
          answer.warnings()), completion, startedAt);
    });
    persistMessage(conversation.getId(), "user", contextAssembly.userPrompt(), contextAssembly.providerName(), contextAssembly.modelName());
    persistMessage(conversation.getId(), "assistant", answer.answer(), contextAssembly.providerName(), contextAssembly.modelName());

    List<EnterpriseAiContracts.CitationReference> citations = answer.citations().stream()
        .map(citation -> new EnterpriseAiContracts.CitationReference(
            citation.sourceType(),
            citation.sourceId(),
            citation.title(),
            citation.excerpt(),
            citation.pageNumber(),
            null,
            citation.sourceSection(),
            citation.pageNumber() != null || citation.sourceSection() != null ? "HIGH" : "LOW",
            citation.pageNumber() != null
                ? "document:" + citation.sourceId() + ":page:" + citation.pageNumber()
                : "metadata:" + citation.sourceType() + ":" + citation.sourceId() + ":" + citation.sourceSection(),
            answer.retrievalMode()))
        .toList();
    List<EnterpriseAiContracts.EvidenceReference> evidenceReferences = citationBuilderService.buildEvidenceReferences(citations);
    List<EnterpriseAiContracts.SourceReference> sourceReferences = citationBuilderService.buildSourceReferences(citations);
    EnterpriseAiContracts.GroundingValidation grounding = groundingValidationService.validate(citations, answer.warnings());
    citationAuditService.record(
        answer.interactionId(),
        conversation.getId(),
        request.clientId(),
        retrieval.retrievalMode(),
        citations,
        retrieval.evidence(),
        guardrailOutcome);

    return new EnterpriseAiContracts.EnterpriseAiResponse(
        request.operation(),
        conversation.getId(),
        answer.interactionId(),
        answer.status(),
        answer.answer(),
        citations,
        evidenceReferences,
        sourceReferences,
        grounding.warnings(),
        new EnterpriseAiContracts.MetricsSnapshot(
            System.currentTimeMillis() - startedAt,
            retrieval.evidence().size(),
            answer.retrievalMode(),
            false),
        grounding,
        Map.of(
            "createdAt", answer.createdAt(),
            "context", summarizeContextAssembly(contextAssembly),
            "guardrails", summarizeGuardrails(guardrailOutcome)));
  }

  private AiConversation resolveConversation(EnterpriseAiContracts.EnterpriseAiRequest request) {
    if (request.conversationId() != null) {
      return aiConversationRepository.findById(request.conversationId()).orElseGet(() -> createConversation(request));
    }
    return createConversation(request);
  }

  private AiConversation createConversation(EnterpriseAiContracts.EnterpriseAiRequest request) {
    AiConversation conversation = new AiConversation();
    conversation.setClientId(request.clientId());
    conversation.setActorUserId(request.actorUserId());
    conversation.setScopeType("CLIENT");
    conversation.setScopeId(request.clientId());
    conversation.setOperationType(request.operation().name());
    conversation.setTitle(request.prompt());
    return aiConversationRepository.save(conversation);
  }

  private AiInteraction createInteraction(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      UUID conversationId,
      String retrievalMode,
      EnterpriseAiContracts.GroundingValidation grounding,
      long startedAt) {
    AiInteraction interaction = new AiInteraction();
    interaction.setClientId(request.clientId());
    interaction.setQuestion(request.prompt());
    interaction.setAnswer(null);
    interaction.setStatus("Ready");
    interaction.setOperationType(request.operation().name());
    interaction.setConversationId(conversationId);
    interaction.setRetrievalMode(retrievalMode);
    interaction.setGroundingScore(grounding.groundingScore());
    interaction.setCitationCoverage(grounding.citationCoverage());
    interaction.setTotalLatencyMs(System.currentTimeMillis() - startedAt);
    interaction.setWarningSummary(String.join(" | ", grounding.warnings()));
    return aiInteractionRepository.save(interaction);
  }

  private static String buildDeterministicAnswer(
      EnterpriseAiOperation operation,
      List<EnterpriseAiContracts.RetrievedEvidence> evidence) {
    String summary = evidence.stream()
        .map(item -> item.title() + ": " + item.excerpt())
        .limit(3)
        .reduce((left, right) -> left + " | " + right)
        .orElse("No evidence was retrieved.");
    return switch (operation) {
      case SUMMARIZE -> "Summary prepared from retrieved evidence: " + summary;
      case EXPLAIN -> "Explanation prepared from retrieved evidence: " + summary;
      case COMPARE -> "Comparison scaffold prepared from retrieved evidence: " + summary;
      case EXTRACT -> "Extraction scaffold prepared from retrieved evidence: " + summary;
      case VALIDATE -> "Validation scaffold prepared from retrieved evidence: " + summary;
      case SEARCH -> "Search scaffold prepared from retrieved evidence: " + summary;
      case ASK -> summary;
    };
  }

  private static Map<String, Object> buildStructuredPayload(
      EnterpriseAiOperation operation,
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryPlan queryPlan,
      List<EnterpriseAiContracts.RetrievedEvidence> evidence,
      ContextAssembly contextAssembly,
      EnterpriseGuardrailService.GuardrailOutcome guardrailOutcome) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("operation", operation.name());
    payload.put("clientId", request.clientId());
    payload.put("sourceIds", request.sourceIds());
    payload.put("evidenceCount", evidence.size());
    payload.put("queryPlan", summarizeQueryPlan(queryPlan));
    payload.put("context", summarizeContextAssembly(contextAssembly));
    payload.put("guardrails", summarizeGuardrails(guardrailOutcome));
    if (operation == EnterpriseAiOperation.EXTRACT || operation == EnterpriseAiOperation.VALIDATE) {
      payload.put("fields", Map.of(
          "title", Map.of("value", evidence.isEmpty() ? "" : evidence.getFirst().title(), "status", evidence.isEmpty() ? "MISSING" : "READ_ONLY"),
          "excerpt", Map.of("value", evidence.isEmpty() ? "" : evidence.getFirst().excerpt(), "status", evidence.isEmpty() ? "MISSING" : "NEEDS_REVIEW")));
    }
    return Map.copyOf(payload);
  }

  private void persistMessage(
      UUID conversationId,
      String role,
      String content,
      String providerName,
      String modelName) {
    AiConversationMessage message = new AiConversationMessage();
    message.setConversationId(conversationId);
    message.setRole(role);
    message.setContent(content);
    message.setProviderName(providerName);
    message.setModelName(modelName);
    int estimatedTokens = estimateTokens(content);
    if ("assistant".equals(role)) {
      message.setCompletionTokens(estimatedTokens);
    } else {
      message.setPromptTokens(estimatedTokens);
    }
    message.setTotalTokens(estimatedTokens);
    aiConversationMessageRepository.save(message);
  }

  private static Map<String, Object> summarizeContextAssembly(ContextAssembly contextAssembly) {
    return Map.of(
        "provider", contextAssembly.providerName(),
        "model", contextAssembly.modelName(),
        "messageCount", contextAssembly.messages().size(),
        "historyCount", contextAssembly.conversationHistory().size(),
        "evidenceCount", contextAssembly.evidence().size(),
        "metadata", contextAssembly.metadata(),
        "tokenBudget", contextAssembly.tokenBudget());
  }

  private static int estimateTokens(String content) {
    if (content == null || content.isBlank()) {
      return 0;
    }
    return Math.max(1, content.length() / 4);
  }

  private static Map<String, Object> summarizeGuardrails(EnterpriseGuardrailService.GuardrailOutcome guardrailOutcome) {
    return Map.of(
        "piiMasked", guardrailOutcome.piiMasked(),
        "permissionTrimmed", guardrailOutcome.permissionTrimmed(),
        "promptInjectionDetected", guardrailOutcome.promptInjectionDetected(),
        "tokenLimitApplied", guardrailOutcome.tokenLimitApplied(),
        "restrictedContentNotice", valueOrEmpty(guardrailOutcome.restrictedContentNotice()),
        "insufficientEvidence", guardrailOutcome.insufficientEvidence());
  }

  private static Map<String, Object> summarizeQueryPlan(EnterpriseAiContracts.QueryPlan queryPlan) {
    return Map.of(
        "scope", queryPlan.scope().name(),
        "retrievalModes", queryPlan.retrievalModes(),
        "sourceTypes", queryPlan.sourceTypes().stream().map(Enum::name).toList(),
        "businessReferenceFields", Map.of(
            "policyNumber", valueOrEmpty(queryPlan.businessReferenceFields().policyNumber()),
            "claimNumber", valueOrEmpty(queryPlan.businessReferenceFields().claimNumber()),
            "insurer", valueOrEmpty(queryPlan.businessReferenceFields().insurer()),
            "policyType", valueOrEmpty(queryPlan.businessReferenceFields().policyType()),
            "effectiveDate", valueOrEmpty(queryPlan.businessReferenceFields().effectiveDate()),
            "expiryDate", valueOrEmpty(queryPlan.businessReferenceFields().expiryDate()),
            "renewalDate", valueOrEmpty(queryPlan.businessReferenceFields().renewalDate()),
            "brokerReference", valueOrEmpty(queryPlan.businessReferenceFields().brokerReference()),
            "externalReference", valueOrEmpty(queryPlan.businessReferenceFields().externalReference())),
        "sortOrder", queryPlan.sortOrder().name(),
        "versionPreference", queryPlan.versionPreference().name(),
        "requiredEvidenceGranularity", queryPlan.requiredEvidenceGranularity().name());
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private void saveMetric(
      AiInteraction interaction,
      UUID conversationId,
      EnterpriseAiOperation operation,
      RetrievalCoordinator.RetrievalResult retrieval,
      EnterpriseAiContracts.GroundingValidation grounding,
      LlmCompletionResponse completion,
      long startedAt) {
    AiOrchestrationMetric metric = new AiOrchestrationMetric();
    metric.setInteractionId(interaction.getId());
    metric.setConversationId(conversationId);
    metric.setOperationType(operation.name());
    metric.setTotalLatencyMs(System.currentTimeMillis() - startedAt);
    metric.setProviderLatencyMs(completion.providerLatencyMs());
    metric.setGroundingScore(grounding.groundingScore());
    metric.setCitationCoverage(grounding.citationCoverage());
    metric.setEvidenceCount(retrieval.evidence().size());
    metric.setWarningCount(mergeWarnings(grounding.warnings(), completion.warnings()).size());
    metric.setFallbackUsed(completion.fallbackUsed());
    metric.setProviderName(completion.providerName());
    metric.setModelName(completion.modelName());
    aiOrchestrationMetricRepository.save(metric);
  }

  private static List<String> mergeWarnings(List<String> left, List<String> right) {
    return java.util.stream.Stream.concat(
            left == null ? java.util.stream.Stream.empty() : left.stream(),
            right == null ? java.util.stream.Stream.empty() : right.stream())
        .distinct()
        .toList();
  }
}
