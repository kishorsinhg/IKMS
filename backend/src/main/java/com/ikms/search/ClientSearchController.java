package com.ikms.search;

import com.ikms.ai.AiContracts;
import com.ikms.ai.ClientQuestionAnsweringService;
import com.ikms.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
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

  public ClientSearchController(
      ClientSearchService clientSearchService,
      ClientQuestionAnsweringService clientQuestionAnsweringService) {
    this.clientSearchService = clientSearchService;
    this.clientQuestionAnsweringService = clientQuestionAnsweringService;
  }

  @GetMapping("/search")
  public List<SearchContracts.SearchResultResponse> search(
      @PathVariable UUID clientId,
      @RequestParam(name = "query", defaultValue = "") String query,
      Authentication authentication) {
    return clientSearchService.search(clientId, query, principal(authentication).permissions());
  }

  @PostMapping("/ask")
  public AiContracts.AskClientResponse ask(
      @PathVariable UUID clientId,
      @Valid @RequestBody AiContracts.AskClientRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = principal(authentication);
    return clientQuestionAnsweringService.ask(clientId, request.question(), principal.permissions(), principal.id());
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
