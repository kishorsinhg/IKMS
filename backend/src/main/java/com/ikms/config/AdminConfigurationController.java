package com.ikms.config;

import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.AppUserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminConfigurationController {

  private final AppUserRepository appUserRepository;
  private final KnowledgeConfigurationService knowledgeConfigurationService;
  private final IntakeAiConfigurationService intakeAiConfigurationService;

  public AdminConfigurationController(
      AppUserRepository appUserRepository,
      KnowledgeConfigurationService knowledgeConfigurationService,
      IntakeAiConfigurationService intakeAiConfigurationService) {
    this.appUserRepository = appUserRepository;
    this.knowledgeConfigurationService = knowledgeConfigurationService;
    this.intakeAiConfigurationService = intakeAiConfigurationService;
  }

  @GetMapping("/users")
  public List<AdminConfigurationContracts.AdminUserResponse> listUsers() {
    return appUserRepository.findAll().stream()
        .map(user -> new AdminConfigurationContracts.AdminUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getStatus(), user.getRoles()))
        .toList();
  }

  @GetMapping("/document-types")
  public List<AdminConfigurationContracts.DocumentTypeResponse> listDocumentTypes() {
    return knowledgeConfigurationService.listDocumentTypes();
  }

  @PostMapping("/document-types")
  public AdminConfigurationContracts.DocumentTypeResponse createDocumentType(@Valid @RequestBody AdminConfigurationContracts.DocumentTypeRequest request, Authentication authentication) {
    return knowledgeConfigurationService.createDocumentType(request, principal(authentication).id());
  }

  @PatchMapping("/document-types/{id}")
  public AdminConfigurationContracts.DocumentTypeResponse updateDocumentType(@PathVariable UUID id, @Valid @RequestBody AdminConfigurationContracts.DocumentTypeRequest request, Authentication authentication) {
    return knowledgeConfigurationService.updateDocumentType(id, request, principal(authentication).id());
  }

  @GetMapping("/metadata-fields")
  public List<AdminConfigurationContracts.MetadataFieldResponse> listMetadataFields() {
    return knowledgeConfigurationService.listMetadataFields();
  }

  @PostMapping("/metadata-fields")
  public AdminConfigurationContracts.MetadataFieldResponse createMetadataField(@Valid @RequestBody AdminConfigurationContracts.MetadataFieldRequest request, Authentication authentication) {
    return knowledgeConfigurationService.createMetadataField(request, principal(authentication).id());
  }

  @PatchMapping("/metadata-fields/{id}")
  public AdminConfigurationContracts.MetadataFieldResponse updateMetadataField(@PathVariable UUID id, @Valid @RequestBody AdminConfigurationContracts.MetadataFieldRequest request, Authentication authentication) {
    return knowledgeConfigurationService.updateMetadataField(id, request, principal(authentication).id());
  }

  @GetMapping("/intake/shared-folders")
  public List<AdminConfigurationContracts.SharedFolderResponse> listSharedFolders() {
    return intakeAiConfigurationService.listSharedFolders();
  }

  @PostMapping("/intake/shared-folders")
  public AdminConfigurationContracts.SharedFolderResponse createSharedFolder(@Valid @RequestBody AdminConfigurationContracts.SharedFolderRequest request, Authentication authentication) {
    return intakeAiConfigurationService.createSharedFolder(request, principal(authentication).id());
  }

  @PatchMapping("/intake/shared-folders/{id}")
  public AdminConfigurationContracts.SharedFolderResponse updateSharedFolder(@PathVariable UUID id, @Valid @RequestBody AdminConfigurationContracts.SharedFolderRequest request, Authentication authentication) {
    return intakeAiConfigurationService.updateSharedFolder(id, request, principal(authentication).id());
  }

  @GetMapping("/intake/mailboxes")
  public List<AdminConfigurationContracts.MailboxConfigResponse> listMailboxes() {
    return intakeAiConfigurationService.listMailboxes();
  }

  @PostMapping("/intake/mailboxes")
  public AdminConfigurationContracts.MailboxConfigResponse createMailbox(@Valid @RequestBody AdminConfigurationContracts.MailboxConfigRequest request, Authentication authentication) {
    return intakeAiConfigurationService.createMailbox(request, principal(authentication).id());
  }

  @PatchMapping("/intake/mailboxes/{id}")
  public AdminConfigurationContracts.MailboxConfigResponse updateMailbox(@PathVariable UUID id, @Valid @RequestBody AdminConfigurationContracts.MailboxConfigRequest request, Authentication authentication) {
    return intakeAiConfigurationService.updateMailbox(id, request, principal(authentication).id());
  }

  @GetMapping("/review-settings")
  public AdminConfigurationContracts.ReviewSettingResponse getReviewSetting() {
    return intakeAiConfigurationService.getReviewSetting();
  }

  @PostMapping("/review-settings")
  public AdminConfigurationContracts.ReviewSettingResponse createReviewSetting(@Valid @RequestBody AdminConfigurationContracts.ReviewSettingRequest request, Authentication authentication) {
    return intakeAiConfigurationService.upsertReviewSetting(request, principal(authentication).id());
  }

  @PatchMapping("/review-settings")
  public AdminConfigurationContracts.ReviewSettingResponse updateReviewSetting(@Valid @RequestBody AdminConfigurationContracts.ReviewSettingRequest request, Authentication authentication) {
    return intakeAiConfigurationService.upsertReviewSetting(request, principal(authentication).id());
  }

  @GetMapping("/ai-settings")
  public AdminConfigurationContracts.AiProviderSettingResponse getAiSettings() {
    return intakeAiConfigurationService.getAiProviderSetting();
  }

  @PostMapping("/ai-settings")
  public AdminConfigurationContracts.AiProviderSettingResponse createAiSettings(@Valid @RequestBody AdminConfigurationContracts.AiProviderSettingRequest request, Authentication authentication) {
    return intakeAiConfigurationService.upsertAiProviderSetting(request, principal(authentication).id());
  }

  @PatchMapping("/ai-settings")
  public AdminConfigurationContracts.AiProviderSettingResponse updateAiSettings(@Valid @RequestBody AdminConfigurationContracts.AiProviderSettingRequest request, Authentication authentication) {
    return intakeAiConfigurationService.upsertAiProviderSetting(request, principal(authentication).id());
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
