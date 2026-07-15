package com.ikms.governance;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {

  private final GovernancePolicyService governancePolicyService;

  public GovernanceController(GovernancePolicyService governancePolicyService) {
    this.governancePolicyService = governancePolicyService;
  }

  @GetMapping("/classification")
  public GovernanceContracts.ClassificationPolicyResponse getClassificationPolicy() {
    return governancePolicyService.getClassificationPolicy();
  }

  @PostMapping("/classification")
  public GovernanceContracts.ClassificationPolicyResponse saveClassificationPolicy(
      @Valid @RequestBody GovernanceContracts.ClassificationPolicyRequest request,
      Authentication authentication) {
    return governancePolicyService.saveClassificationPolicy(request, principal(authentication).id());
  }

  @GetMapping("/retention")
  public GovernanceContracts.RetentionPolicyResponse getRetentionPolicy() {
    return governancePolicyService.getRetentionPolicy();
  }

  @PostMapping("/retention")
  public GovernanceContracts.RetentionPolicyResponse saveRetentionPolicy(
      @Valid @RequestBody GovernanceContracts.RetentionPolicyRequest request,
      Authentication authentication) {
    return governancePolicyService.saveRetentionPolicy(request, principal(authentication).id());
  }

  @GetMapping("/legal-holds")
  public List<GovernanceContracts.LegalHoldResponse> listLegalHolds() {
    return governancePolicyService.listLegalHolds();
  }

  @PostMapping("/legal-holds")
  public GovernanceContracts.LegalHoldResponse createLegalHold(
      @Valid @RequestBody GovernanceContracts.LegalHoldRequest request,
      Authentication authentication) {
    return governancePolicyService.createLegalHold(request, principal(authentication).id());
  }

  @PostMapping("/reclassify/{documentId}")
  public GovernanceContracts.LegalHoldResponse reclassifyDocument(
      @PathVariable UUID documentId,
      @Valid @RequestBody GovernanceContracts.ReclassifyRequest request,
      Authentication authentication) {
    return governancePolicyService.reclassifyDocument(documentId, request, principal(authentication).id());
  }

  @GetMapping("/ai")
  public GovernanceContracts.AiGovernancePolicyResponse getAiGovernancePolicy() {
    return governancePolicyService.getAiGovernancePolicy();
  }

  @PostMapping("/ai")
  public GovernanceContracts.AiGovernancePolicyResponse saveAiGovernancePolicy(
      @Valid @RequestBody GovernanceContracts.AiGovernancePolicyRequest request,
      Authentication authentication) {
    return governancePolicyService.saveAiGovernancePolicy(request, principal(authentication).id());
  }

  @GetMapping("/security")
  public GovernanceContracts.SecurityPolicyResponse getSecurityPolicy() {
    return governancePolicyService.getSecurityPolicy();
  }

  @PostMapping("/security")
  public GovernanceContracts.SecurityPolicyResponse saveSecurityPolicy(
      @Valid @RequestBody GovernanceContracts.SecurityPolicyRequest request,
      Authentication authentication) {
    return governancePolicyService.saveSecurityPolicy(request, principal(authentication).id());
  }

  @GetMapping("/reports/compliance")
  public GovernanceContracts.ComplianceReportResponse getComplianceReport() {
    return governancePolicyService.buildComplianceReport();
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
