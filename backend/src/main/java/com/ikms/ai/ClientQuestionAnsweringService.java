package com.ikms.ai;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.search.SearchContracts;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

  public ClientQuestionAnsweringService(
      RagContextService ragContextService,
      AiInteractionRepository aiInteractionRepository,
      AuditService auditService,
      PromptInjectionDetectionService promptInjectionDetectionService) {
    this.ragContextService = ragContextService;
    this.aiInteractionRepository = aiInteractionRepository;
    this.auditService = auditService;
    this.promptInjectionDetectionService = promptInjectionDetectionService;
  }

  public AiContracts.AskClientResponse ask(UUID clientId, String question, Set<Permission> permissions, UUID actorUserId) {
    String normalizedQuestion = question == null ? "" : question.trim();
    if (normalizedQuestion.isBlank()) {
      throw new IllegalArgumentException("Question is required.");
    }

    if (isRefused(normalizedQuestion)) {
      return saveInteraction(clientId, normalizedQuestion, "Refused", "I cannot make claim, policy, underwriting, or fraud decisions.", List.of(), actorUserId);
    }

    List<SearchContracts.SearchResultResponse> context = ragContextService.buildContext(clientId, normalizedQuestion, permissions);
    List<SearchContracts.SearchResultResponse> safeContext = context.stream()
        .filter(result -> allowContext(clientId, result, actorUserId))
        .toList();
    if (safeContext.isEmpty()) {
      return saveInteraction(
          clientId,
          normalizedQuestion,
          "NoEvidence",
          "No authorized evidence was found for this client question after safety and permission filtering.",
          List.of(),
          actorUserId);
    }

    String answer = buildAnswer(safeContext);
    List<AiContracts.SourceCitation> citations = safeContext.stream()
        .map(result -> new AiContracts.SourceCitation(
            result.sourceType(),
            result.sourceId(),
            result.title(),
            result.excerpt(),
            result.pageNumber(),
            result.sourceSection()))
        .toList();
    return saveInteraction(clientId, normalizedQuestion, "Answered", answer, citations, actorUserId);
  }

  private AiContracts.AskClientResponse saveInteraction(
      UUID clientId,
      String question,
      String status,
      String answer,
      List<AiContracts.SourceCitation> citations,
      UUID actorUserId) {
    AiInteraction interaction = new AiInteraction();
    interaction.setClientId(clientId);
    interaction.setQuestion(question);
    interaction.setStatus(status);
    interaction.setAnswer(answer);
    interaction.setCitedSources(citations.stream().map(citation -> citation.title() + " [" + citation.sourceType() + "]").reduce((a, b) -> a + "; " + b).orElse(""));
    AiInteraction saved = aiInteractionRepository.save(interaction);

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
        java.util.Map.of("status", status)));

    return new AiContracts.AskClientResponse(saved.getId(), status, answer, citations, saved.getCreatedAt());
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

  private static String buildAnswer(List<SearchContracts.SearchResultResponse> context) {
    String base = context.stream()
        .map(result -> result.title() + ": " + result.excerpt())
        .reduce((left, right) -> left + " | " + right)
        .orElse("No evidence found.");
    return detectConflict(context)
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
}
