package com.ikms.client;

import com.ikms.security.AppUserPrincipal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeSourceController {

  private final ClientKnowledgeService clientKnowledgeService;

  public KnowledgeSourceController(ClientKnowledgeService clientKnowledgeService) {
    this.clientKnowledgeService = clientKnowledgeService;
  }

  @GetMapping("/sources/{sourceType}/{sourceId}/related")
  public ClientContracts.RelatedKnowledgeResponse relatedKnowledgeForSource(
      @PathVariable String sourceType,
      @PathVariable UUID sourceId,
      @RequestParam(name = "limit", required = false) Integer limit,
      Authentication authentication) {
    AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
    return clientKnowledgeService.relatedForSource(sourceType, sourceId, limit == null ? 12 : limit, principal.id(), principal.permissions());
  }
}
