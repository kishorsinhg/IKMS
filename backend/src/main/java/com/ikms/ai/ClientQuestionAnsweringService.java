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

  public ClientQuestionAnsweringService(
      RagContextService ragContextService,
      AiInteractionRepository aiInteractionRepository,
      AuditService auditService) {
    this.ragContextService = ragContextService;
    this.aiInteractionRepository = aiInteractionRepository;
    this.auditService = auditService;
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
    if (context.isEmpty()) {
      return saveInteraction(clientId, normalizedQuestion, "NoEvidence", "No authorized evidence was found for this client question.", List.of(), actorUserId);
    }

    String answer = buildAnswer(context);
    List<AiContracts.SourceCitation> citations = context.stream()
        .map(result -> new AiContracts.SourceCitation(result.sourceType(), result.sourceId(), result.title(), result.excerpt()))
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

  private static String buildAnswer(List<SearchContracts.SearchResultResponse> context) {
    String base = context.stream()
        .map(result -> result.title() + ": " + result.excerpt())
        .reduce((left, right) -> left + " | " + right)
        .orElse("No evidence found.");

    long distinctCitations = context.stream().map(SearchContracts.SearchResultResponse::citation).distinct().count();
    if (distinctCitations > 1) {
      return base + " Conflicting or complementary sources are included in the citations.";
    }
    return base;
  }
}
