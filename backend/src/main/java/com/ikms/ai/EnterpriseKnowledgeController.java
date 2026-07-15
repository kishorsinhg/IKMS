package com.ikms.ai;

import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import com.ikms.ai.orchestration.EnterpriseAiOrchestrationService;
import com.ikms.ai.orchestration.IntentDetectionService;
import com.ikms.ai.orchestration.QueryPlanningService;
import com.ikms.observability.RequestContextHolder;
import com.ikms.search.ClientSearchService;
import com.ikms.search.SearchQueryContext;
import com.ikms.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EnterpriseKnowledgeController {

  private final IntentDetectionService intentDetectionService;
  private final QueryPlanningService queryPlanningService;
  private final ClientSearchService clientSearchService;
  private final EnterpriseAiOrchestrationService enterpriseAiOrchestrationService;
  private final AiCitationRecordRepository aiCitationRecordRepository;
  private final AiConversationMessageRepository aiConversationMessageRepository;

  public EnterpriseKnowledgeController(
      IntentDetectionService intentDetectionService,
      QueryPlanningService queryPlanningService,
      ClientSearchService clientSearchService,
      EnterpriseAiOrchestrationService enterpriseAiOrchestrationService,
      AiCitationRecordRepository aiCitationRecordRepository,
      AiConversationMessageRepository aiConversationMessageRepository) {
    this.intentDetectionService = intentDetectionService;
    this.queryPlanningService = queryPlanningService;
    this.clientSearchService = clientSearchService;
    this.enterpriseAiOrchestrationService = enterpriseAiOrchestrationService;
    this.aiCitationRecordRepository = aiCitationRecordRepository;
    this.aiConversationMessageRepository = aiConversationMessageRepository;
  }

  @GetMapping("/search/knowledge")
  public EnterpriseKnowledgeContracts.KnowledgeSearchResponse searchKnowledge(
      @RequestParam(name = "query", defaultValue = "") String query,
      @RequestParam(name = "customerId", required = false) UUID customerId,
      @RequestParam(name = "policyNumber", required = false) String policyNumber,
      @RequestParam(name = "claimNumber", required = false) String claimNumber,
      @RequestParam(name = "insurer", required = false) String insurer,
      Authentication authentication) {
    try (RequestContextHolder.Scope ignored = RequestContextHolder.withGenerated(RequestContextHolder.SEARCH_REQUEST_ID)) {
      AppUserPrincipal principal = principal(authentication);
      Map<String, Object> parameters = parameterMap(policyNumber, claimNumber, insurer);
      EnterpriseAiContracts.EnterpriseAiRequest request = new EnterpriseAiContracts.EnterpriseAiRequest(
          customerId,
          EnterpriseAiOperation.SEARCH,
          query,
          principal.id(),
          principal.permissions(),
          null,
          List.of(),
          parameters);
      EnterpriseAiContracts.DetectedIntent intent = intentDetectionService.detect(request);
      EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(request, intent);
      ClientSearchService.SearchOutcome outcome = clientSearchService.searchDetailed(SearchQueryContext.enterprise(request, plan));
      return new EnterpriseKnowledgeContracts.KnowledgeSearchResponse(
          queryPlanResponse(plan),
          outcome.results().stream()
              .map(result -> new EnterpriseKnowledgeContracts.KnowledgeResultResponse(
                  result.sourceType(),
                  result.sourceId(),
                  result.title(),
                  result.excerpt(),
                  supportingAttributes(plan.businessReferenceFields())))
              .toList());
    }
  }

  @PostMapping("/ask")
  public EnterpriseKnowledgeContracts.GlobalAskResponse ask(
      @Valid @RequestBody AiContracts.GlobalAskRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    EnterpriseAiContracts.EnterpriseAiRequest aiRequest = new EnterpriseAiContracts.EnterpriseAiRequest(
        request.customerId(),
        EnterpriseAiOperation.ASK,
        request.question(),
        principal.id(),
        principal.permissions(),
        null,
        List.of(),
        request.parameters() == null ? Map.of() : request.parameters());
    EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(aiRequest, intentDetectionService.detect(aiRequest));
    EnterpriseAiContracts.EnterpriseAiResponse response = enterpriseAiOrchestrationService.orchestrate(
        request.customerId(),
        EnterpriseAiOperation.ASK,
        request.question(),
        principal.permissions(),
        principal.id(),
        null,
        List.of(),
        request.parameters() == null ? Map.of() : request.parameters());
    return new EnterpriseKnowledgeContracts.GlobalAskResponse(
        "Answered".equalsIgnoreCase(response.status()) || "Ready".equalsIgnoreCase(response.status()) ? "Answered" : response.status(),
        response.answer(),
        response.grounding().grounded() ? "GROUNDED" : "UNGROUNDED",
        response.citations().stream()
            .map(citation -> new EnterpriseKnowledgeContracts.AskCitationResponse(
                citation.sourceType(),
                citation.sourceId(),
                citation.title(),
                supportingAttributes(plan.businessReferenceFields())))
            .toList(),
        guardrailNotice(response.structuredPayload()),
        "audit-ask-" + response.interactionId());
  }

  @GetMapping("/ai/interactions/{interactionId}/evidence")
  public EnterpriseKnowledgeContracts.EvidenceExpansionResponse evidence(
      @PathVariable UUID interactionId) {
    return new EnterpriseKnowledgeContracts.EvidenceExpansionResponse(
        interactionId,
        aiCitationRecordRepository.findByInteractionIdOrderByCreatedAtAsc(interactionId).stream()
            .map(record -> new EnterpriseKnowledgeContracts.EvidenceRecord(
                record.getSourceType(),
                record.getSourceId(),
                record.getTitle(),
                supportingAttributesFromCitation(record)))
            .toList());
  }

  @PostMapping("/ai/conversations/{conversationId}/continue")
  public EnterpriseKnowledgeContracts.ConversationContinuationResponse continueConversation(
      @PathVariable UUID conversationId,
      @Valid @RequestBody AiContracts.ConversationContinueRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    EnterpriseAiContracts.EnterpriseAiRequest continuationRequest = new EnterpriseAiContracts.EnterpriseAiRequest(
        null,
        EnterpriseAiOperation.ASK,
        request.prompt(),
        principal.id(),
        principal.permissions(),
        conversationId,
        request.sourceIds() == null ? List.of() : request.sourceIds(),
        request.parameters() == null ? Map.of() : request.parameters());
    EnterpriseAiContracts.QueryPlan plan = queryPlanningService.plan(
        continuationRequest,
        intentDetectionService.detect(continuationRequest));
    return new EnterpriseKnowledgeContracts.ConversationContinuationResponse(
        conversationId,
        Math.toIntExact(aiConversationMessageRepository.countByConversationId(conversationId)),
        queryPlanResponse(plan));
  }

  @PostMapping("/ai/stream")
  public EnterpriseKnowledgeContracts.StreamResponse stream(
      @Valid @RequestBody AiContracts.StreamRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    Map<String, Object> parameters = new java.util.LinkedHashMap<>(request.parameters() == null ? Map.of() : request.parameters());
    parameters.put("stream", true);
    EnterpriseAiContracts.EnterpriseAiResponse response = enterpriseAiOrchestrationService.orchestrate(
        request.customerId(),
        EnterpriseAiOperation.ASK,
        request.prompt(),
        principal.permissions(),
        principal.id(),
        request.conversationId(),
        request.sourceIds() == null ? List.of() : request.sourceIds(),
        parameters);
    @SuppressWarnings("unchecked")
    List<String> events = (List<String>) response.structuredPayload().getOrDefault("streamEvents", List.of("start", "complete"));
    return new EnterpriseKnowledgeContracts.StreamResponse(
        !"Failed".equalsIgnoreCase(response.status()) && !"InsufficientEvidence".equalsIgnoreCase(response.status()),
        response.status(),
        events.stream().map(event -> new EnterpriseKnowledgeContracts.StreamEventResponse(event, event.equals("delta") ? response.answer() : "")).toList(),
        response.warnings());
  }

  private static EnterpriseKnowledgeContracts.KnowledgeQueryPlanResponse queryPlanResponse(
      EnterpriseAiContracts.QueryPlan plan) {
    return new EnterpriseKnowledgeContracts.KnowledgeQueryPlanResponse(
        plan.scope().name(),
        plan.normalizedPrompt(),
        supportingAttributes(plan.businessReferenceFields()),
        plan.versionPreference().name(),
        plan.requiredEvidenceGranularity().name());
  }

  private static Map<String, String> supportingAttributes(EnterpriseAiContracts.BusinessReferenceFields fields) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("policyNumber", valueOrEmpty(fields.policyNumber()));
    attributes.put("claimNumber", valueOrEmpty(fields.claimNumber()));
    attributes.put("insurer", valueOrEmpty(fields.insurer()));
    return Map.copyOf(attributes);
  }

  private static Map<String, String> supportingAttributesFromCitation(AiCitationRecord record) {
    Map<String, String> attributes = new LinkedHashMap<>();
    String excerpt = record.getExcerpt() == null ? "" : record.getExcerpt();
    if (excerpt.contains("POL-")) {
      attributes.put("policyNumber", excerpt.replaceAll(".*(POL-[A-Z0-9-]+).*", "$1"));
    } else {
      attributes.put("policyNumber", "");
    }
    if (excerpt.contains("CLM-")) {
      attributes.put("claimNumber", excerpt.replaceAll(".*(CLM-[A-Z0-9-]+).*", "$1"));
    } else {
      attributes.put("claimNumber", "");
    }
    attributes.put("insurer", "");
    return Map.copyOf(attributes);
  }

  @SuppressWarnings("unchecked")
  private static String guardrailNotice(Map<String, Object> payload) {
    Object guardrails = payload.get("guardrails");
    if (guardrails instanceof Map<?, ?> guardrailMap) {
      Object notice = guardrailMap.get("restrictedContentNotice");
      return notice == null ? "" : String.valueOf(notice);
    }
    return "";
  }

  private static Map<String, Object> parameterMap(String policyNumber, String claimNumber, String insurer) {
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (policyNumber != null && !policyNumber.isBlank()) {
      parameters.put("policyNumber", policyNumber);
    }
    if (claimNumber != null && !claimNumber.isBlank()) {
      parameters.put("claimNumber", claimNumber);
    }
    if (insurer != null && !insurer.isBlank()) {
      parameters.put("insurer", insurer);
    }
    return Map.copyOf(parameters);
  }

  private static String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
