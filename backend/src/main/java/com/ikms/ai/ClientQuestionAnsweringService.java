package com.ikms.ai;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.governance.GovernancePolicyService;
import com.ikms.observability.RequestContextHolder;
import com.ikms.search.SearchContracts;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientQuestionAnsweringService {

  private static final List<String> PROHIBITED_TERMS = List.of(
      "underwrite", "approve claim", "deny claim", "fraud", "policy decision", "coverage decision");

  private final RagContextService ragContextService;
  private final AiInteractionRepository aiInteractionRepository;
  private final AuditService auditService;
  private final PromptInjectionDetectionService promptInjectionDetectionService;
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;
  private final GovernancePolicyService governancePolicyService;

  public ClientQuestionAnsweringService(
      RagContextService ragContextService,
      AiInteractionRepository aiInteractionRepository,
      AuditService auditService,
      PromptInjectionDetectionService promptInjectionDetectionService,
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient,
      GovernancePolicyService governancePolicyService) {
    this.ragContextService = ragContextService;
    this.aiInteractionRepository = aiInteractionRepository;
    this.auditService = auditService;
    this.promptInjectionDetectionService = promptInjectionDetectionService;
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
    this.governancePolicyService = governancePolicyService;
  }

  public AiContracts.AskClientResponse ask(UUID clientId, String question, Set<Permission> permissions, UUID actorUserId) {
    return ask(clientId, question, permissions, actorUserId, java.util.Map.of());
  }

  public AiContracts.AskClientResponse ask(
      UUID clientId,
      String question,
      Set<Permission> permissions,
      UUID actorUserId,
      java.util.Map<String, String> actorAttributes) {
    String normalizedQuestion = question == null ? "" : question.trim();
    if (normalizedQuestion.isBlank()) {
      throw new IllegalArgumentException("Question is required.");
    }

    if (isRefused(normalizedQuestion)) {
      return saveInteraction(clientId, normalizedQuestion, "Refused", "I cannot make claim, policy, underwriting, or fraud decisions.", List.of(), "GUARDRAIL", List.of(), actorUserId);
    }

    var contextOutcome = actorAttributes == null || actorAttributes.isEmpty()
        ? ragContextService.buildContextDetailed(clientId, normalizedQuestion, permissions)
        : ragContextService.buildContextDetailed(clientId, normalizedQuestion, permissions, actorAttributes);
    List<SearchContracts.SearchResultResponse> safeContext = contextOutcome.results().stream()
        .filter(result -> allowContext(clientId, result, actorUserId))
        .toList();
    List<String> warnings = new java.util.ArrayList<>(contextOutcome.warnings());
    if (safeContext.isEmpty()) {
      return saveInteraction(
          clientId,
          normalizedQuestion,
          "NoEvidence",
          "No authorized evidence was found for this client question after safety and permission filtering.",
          List.of(),
          contextOutcome.retrievalMode(),
          warnings,
          actorUserId);
    }

    boolean conflictingEvidence = detectConflict(safeContext);
    List<AiContracts.SourceCitation> citations = safeContext.stream()
        .map(result -> new AiContracts.SourceCitation(
            result.sourceType(),
            result.sourceId(),
            result.title(),
            result.excerpt(),
            result.pageNumber(),
            result.sourceSection()))
        .toList();
    String answer = buildProviderAnswer(normalizedQuestion, safeContext, conflictingEvidence)
        .orElseGet(() -> buildFallbackAnswer(safeContext, conflictingEvidence));
    if (safeContext.stream().anyMatch(result -> "LOW".equals(result.citationQuality()))) {
      warnings.add("Some cited document evidence is missing ideal page or section provenance.");
    }
    return saveInteraction(clientId, normalizedQuestion, "Answered", answer, citations, contextOutcome.retrievalMode(), warnings, actorUserId);
  }

  private AiContracts.AskClientResponse saveInteraction(
      UUID clientId,
      String question,
      String status,
      String answer,
      List<AiContracts.SourceCitation> citations,
      String retrievalMode,
      List<String> warnings,
      UUID actorUserId) {
    AiInteraction interaction = new AiInteraction();
    interaction.setClientId(clientId);
    interaction.setQuestion(question);
    interaction.setStatus(status);
    interaction.setAnswer(answer);
    interaction.setCitedSources(citations.stream().map(citation -> citation.title() + " [" + citation.sourceType() + "]").reduce((a, b) -> a + "; " + b).orElse(""));
    AiInteraction saved = aiInteractionRepository.save(interaction);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.AI_INTERACTION_ID, saved.getId().toString())) {
      auditService.write(new AuditEvent(
          Instant.now(),
          "AI",
          "CLIENT_QUESTION_ASKED",
          "Answered".equals(status) ? AuditOutcome.SUCCESS : AuditOutcome.DENIED,
          actorUserId,
          clientId,
          "AiInteraction",
          saved.getId().toString(),
          false,
          java.util.Map.of(
              "status", status,
              "retrievalMode", retrievalMode == null ? "" : retrievalMode,
              "warnings", warnings == null || warnings.isEmpty() ? "" : String.join(" | ", warnings))));

      return new AiContracts.AskClientResponse(saved.getId(), status, answer, citations, retrievalMode, warnings == null ? List.of() : List.copyOf(warnings), saved.getCreatedAt());
    }
  }

  private static boolean isRefused(String question) {
    String normalized = question.toLowerCase(Locale.ROOT);
    return PROHIBITED_TERMS.stream().anyMatch(normalized::contains)
        || (normalized.contains("approve") && normalized.contains("claim"))
        || (normalized.contains("deny") && normalized.contains("claim"))
        || normalized.contains("underwrite")
        || normalized.contains("fraud");
  }

  private boolean allowContext(UUID clientId, SearchContracts.SearchResultResponse result, UUID actorUserId) {
    var detection = promptInjectionDetectionService.inspect(result.title() + " " + result.excerpt());
    if (!detection.detected()) {
      return true;
    }

    auditService.write(new AuditEvent(
        Instant.now(),
        "AI",
        "PROMPT_INJECTION_BLOCKED",
        AuditOutcome.DENIED,
        actorUserId,
        clientId,
        result.sourceType(),
        result.sourceId().toString(),
        false,
        java.util.Map.of("marker", detection.marker() == null ? "" : detection.marker())));
    return false;
  }

  private Optional<String> buildProviderAnswer(
      String question,
      List<SearchContracts.SearchResultResponse> context,
      boolean conflictingEvidence) {
    var providerSettings = aiProviderSettingsService.current();
    if (!governancePolicyService.isApprovedModel(providerSettings.providerName(), providerSettings.modelName())) {
      return Optional.empty();
    }
    return aiProviderClient.answerWithEvidence(
        providerSettings,
        question,
        context.stream()
            .map(result -> new AiProviderClient.EvidenceSnippet(
                result.sourceType(),
                result.title(),
                formatLocation(result),
                result.excerpt()))
            .toList(),
        conflictingEvidence)
        .map(answer -> conflictingEvidence && !mentionsConflict(answer)
            ? answer + " Conflicting evidence requires manual review."
            : answer);
  }

  private static String buildFallbackAnswer(List<SearchContracts.SearchResultResponse> context, boolean conflictingEvidence) {
    String base = context.stream()
        .map(result -> result.title() + ": " + result.excerpt())
        .reduce((left, right) -> left + " | " + right)
        .orElse("No evidence found.");
    return conflictingEvidence
        ? base + " Source excerpts conflict on key facts and require manual review."
        : base;
  }

  private static boolean detectConflict(List<SearchContracts.SearchResultResponse> context) {
    List<String> excerpts = context.stream()
        .map(result -> (result.title() + " " + result.excerpt()).toLowerCase(Locale.ROOT))
        .toList();
    return hasOpposingSignals(excerpts, List.of("active", "approved", "covered", "valid"), List.of("inactive", "denied", "not covered", "expired"))
        || hasOpposingSignals(excerpts, List.of("renewal due", "outstanding"), List.of("paid", "completed", "settled"));
  }

  private static boolean hasOpposingSignals(List<String> excerpts, List<String> leftSignals, List<String> rightSignals) {
    boolean leftFound = excerpts.stream().anyMatch(text -> leftSignals.stream().anyMatch(text::contains));
    boolean rightFound = excerpts.stream().anyMatch(text -> rightSignals.stream().anyMatch(text::contains));
    return leftFound && rightFound;
  }

  private static String formatLocation(SearchContracts.SearchResultResponse result) {
    if (result.pageNumber() != null) {
      return "Page " + result.pageNumber();
    }
    if (result.sourceSection() != null && !result.sourceSection().isBlank()) {
      return result.sourceSection();
    }
    return result.sourceType();
  }

  private static boolean mentionsConflict(String answer) {
    String normalized = answer.toLowerCase(Locale.ROOT);
    return normalized.contains("conflict")
        || normalized.contains("manual review")
        || normalized.contains("conflicting");
  }
}
