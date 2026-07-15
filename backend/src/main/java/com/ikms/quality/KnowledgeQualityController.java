package com.ikms.quality;

import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-quality")
public class KnowledgeQualityController {

  private final KnowledgeQualityService knowledgeQualityService;

  public KnowledgeQualityController(KnowledgeQualityService knowledgeQualityService) {
    this.knowledgeQualityService = knowledgeQualityService;
  }

  @GetMapping("/customers")
  public KnowledgeQualityContracts.KnowledgeQualityCustomerListResponse customers(
      @RequestParam(name = "query", required = false) String query,
      @RequestParam(name = "refresh", required = false, defaultValue = "false") boolean refresh,
      Authentication authentication) {
    requireSteward(authentication);
    return knowledgeQualityService.listCustomers(query, refresh);
  }

  @GetMapping("/customer/{clientId}")
  public KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse customer(
      @PathVariable UUID clientId,
      @RequestParam(name = "refresh", required = false, defaultValue = "false") boolean refresh,
      Authentication authentication) {
    requireSteward(authentication);
    return knowledgeQualityService.customer(clientId, refresh);
  }

  @GetMapping("/issues")
  public KnowledgeQualityContracts.KnowledgeQualityIssueQueueResponse issues(
      @RequestParam(name = "clientId", required = false) UUID clientId,
      Authentication authentication) {
    requireSteward(authentication);
    return knowledgeQualityService.issues(clientId);
  }

  @PostMapping("/revalidate")
  public KnowledgeQualityContracts.KnowledgeQualityActionResultResponse revalidate(
      @Valid @RequestBody KnowledgeQualityContracts.QualityRevalidateRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = requireSteward(authentication);
    return knowledgeQualityService.revalidate(request, principal.id());
  }

  @PostMapping("/reindex")
  public KnowledgeQualityContracts.KnowledgeQualityActionResultResponse reindex(
      @Valid @RequestBody KnowledgeQualityContracts.QualityReindexRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = requireSteward(authentication);
    return knowledgeQualityService.reindex(request, principal.id());
  }

  @PostMapping("/bulk-correct")
  public KnowledgeQualityContracts.KnowledgeQualityActionResultResponse bulkCorrect(
      @Valid @RequestBody KnowledgeQualityContracts.BulkQualityCorrectionRequest request,
      Authentication authentication) {
    AppUserPrincipal principal = requireSteward(authentication);
    return knowledgeQualityService.bulkCorrect(request, principal.id());
  }

  private AppUserPrincipal requireSteward(Authentication authentication) {
    AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
    if (!principal.permissions().contains(Permission.MANAGE_CONFIGURATION)) {
      throw new AccessDeniedException("Knowledge Quality workspace requires MANAGE_CONFIGURATION.");
    }
    return principal;
  }
}
