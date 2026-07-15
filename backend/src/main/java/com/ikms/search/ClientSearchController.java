package com.ikms.search;

import com.ikms.ai.AiContracts;
import com.ikms.ai.ClientQuestionAnsweringService;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import com.ikms.ai.orchestration.EnterpriseAiOrchestrationService;
import com.ikms.observability.RequestContextHolder;
import com.ikms.security.ActorAttributeContext;
import com.ikms.security.AppUserPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/clients/{clientId}")
public class ClientSearchController {

  private final ClientSearchService clientSearchService;
  private final ClientQuestionAnsweringService clientQuestionAnsweringService;
  private final EnterpriseAiOrchestrationService enterpriseAiOrchestrationService;

  public ClientSearchController(
      ClientSearchService clientSearchService,
      ClientQuestionAnsweringService clientQuestionAnsweringService,
      EnterpriseAiOrchestrationService enterpriseAiOrchestrationService) {
    this.clientSearchService = clientSearchService;
    this.clientQuestionAnsweringService = clientQuestionAnsweringService;
    this.enterpriseAiOrchestrationService = enterpriseAiOrchestrationService;
  }

  @GetMapping("/search")
  public List<SearchContracts.SearchResultResponse> search(
      @PathVariable UUID clientId,
      @RequestParam(name = "query", defaultValue = "") String query,
      Authentication authentication) {
    try (RequestContextHolder.Scope ignored = RequestContextHolder.withGenerated(RequestContextHolder.SEARCH_REQUEST_ID)) {
      AppUserPrincipal principal = principal(authentication);
      return clientSearchService.searchDetailed(
          clientId,
          query,
          principal.permissions(),
          ActorAttributeContext.from(principal).asMap()).results();
    }
  }

  @PostMapping("/ask")
  public AiContracts.AskClientResponse ask(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.AskClientRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    return clientQuestionAnsweringService.ask(
        clientId,
        request.question(),
        principal.permissions(),
        principal.id(),
        ActorAttributeContext.from(principal).asMap());
  }

  @PostMapping("/summarize")
  public EnterpriseAiContracts.EnterpriseAiResponse summarize(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.EnterprisePromptRequest request,
      Authentication authentication) {
    return orchestrate(clientId, EnterpriseAiOperation.SUMMARIZE, request, authentication);
  }

  @PostMapping("/explain")
  public EnterpriseAiContracts.EnterpriseAiResponse explain(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.EnterprisePromptRequest request,
      Authentication authentication) {
    return orchestrate(clientId, EnterpriseAiOperation.EXPLAIN, request, authentication);
  }

  @PostMapping("/compare")
  public EnterpriseAiContracts.EnterpriseAiResponse compare(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.CompareRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    return enterpriseAiOrchestrationService.orchestrate(
        clientId,
        EnterpriseAiOperation.COMPARE,
        request.prompt(),
        principal.permissions(),
        principal.id(),
        request.conversationId(),
        request.sourceIds(),
        enrichParameters(request.parameters(), principal));
  }

  @PostMapping("/extract")
  public EnterpriseAiContracts.EnterpriseAiResponse extract(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.EnterprisePromptRequest request,
      Authentication authentication) {
    return orchestrate(clientId, EnterpriseAiOperation.EXTRACT, request, authentication);
  }

  @PostMapping("/validate")
  public EnterpriseAiContracts.EnterpriseAiResponse validate(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.EnterprisePromptRequest request,
      Authentication authentication) {
    return orchestrate(clientId, EnterpriseAiOperation.VALIDATE, request, authentication);
  }

  private EnterpriseAiContracts.EnterpriseAiResponse orchestrate(
      UUID clientId,
      EnterpriseAiOperation operation,
      AiContracts.EnterprisePromptRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    return enterpriseAiOrchestrationService.orchestrate(
        clientId,
        operation,
        request.prompt(),
        principal.permissions(),
        principal.id(),
        request.conversationId(),
        request.sourceIds(),
        enrichParameters(request.parameters(), principal));
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }

  private Map<String, Object> enrichParameters(Map<String, Object> parameters, AppUserPrincipal principal) {
    Map<String, Object> merged = new java.util.LinkedHashMap<>();
    if (parameters != null) {
      merged.putAll(parameters);
    }
    merged.putAll(ActorAttributeContext.from(principal).asParameterMap());
    return Map.copyOf(merged);
  }
}
