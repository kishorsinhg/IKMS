package com.ikms.config;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.config.domain.AiProviderSetting;
import com.ikms.config.domain.AiProviderSettingRepository;
import com.ikms.config.domain.MailboxConfig;
import com.ikms.config.domain.MailboxConfigRepository;
import com.ikms.config.domain.ReviewSetting;
import com.ikms.config.domain.ReviewSettingRepository;
import com.ikms.config.domain.SharedFolderConfig;
import com.ikms.config.domain.SharedFolderConfigRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IntakeAiConfigurationService {

  private final SharedFolderConfigRepository sharedFolderConfigRepository;
  private final MailboxConfigRepository mailboxConfigRepository;
  private final ReviewSettingRepository reviewSettingRepository;
  private final AiProviderSettingRepository aiProviderSettingRepository;
  private final AuditService auditService;

  public IntakeAiConfigurationService(
      SharedFolderConfigRepository sharedFolderConfigRepository,
      MailboxConfigRepository mailboxConfigRepository,
      ReviewSettingRepository reviewSettingRepository,
      AiProviderSettingRepository aiProviderSettingRepository,
      AuditService auditService) {
    this.sharedFolderConfigRepository = sharedFolderConfigRepository;
    this.mailboxConfigRepository = mailboxConfigRepository;
    this.reviewSettingRepository = reviewSettingRepository;
    this.aiProviderSettingRepository = aiProviderSettingRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<AdminConfigurationContracts.SharedFolderResponse> listSharedFolders() {
    return sharedFolderConfigRepository.findAll().stream()
        .map(item -> new AdminConfigurationContracts.SharedFolderResponse(item.getId(), item.getPath(), item.isActive(), item.getCreatedAt()))
        .toList();
  }

  public AdminConfigurationContracts.SharedFolderResponse createSharedFolder(AdminConfigurationContracts.SharedFolderRequest request, UUID actorUserId) {
    SharedFolderConfig item = new SharedFolderConfig();
    item.setPath(request.path().trim());
    item.setActive(request.active());
    SharedFolderConfig saved = sharedFolderConfigRepository.save(item);
    audit("SHARED_FOLDER_CREATED", actorUserId, saved.getId().toString(), Map.of("path", saved.getPath()));
    return new AdminConfigurationContracts.SharedFolderResponse(saved.getId(), saved.getPath(), saved.isActive(), saved.getCreatedAt());
  }

  public AdminConfigurationContracts.SharedFolderResponse updateSharedFolder(UUID id, AdminConfigurationContracts.SharedFolderRequest request, UUID actorUserId) {
    SharedFolderConfig item = sharedFolderConfigRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shared folder not found: " + id));
    item.setPath(request.path().trim());
    item.setActive(request.active());
    SharedFolderConfig saved = sharedFolderConfigRepository.save(item);
    audit("SHARED_FOLDER_UPDATED", actorUserId, saved.getId().toString(), Map.of("path", saved.getPath()));
    return new AdminConfigurationContracts.SharedFolderResponse(saved.getId(), saved.getPath(), saved.isActive(), saved.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public List<AdminConfigurationContracts.MailboxConfigResponse> listMailboxes() {
    return mailboxConfigRepository.findAll().stream()
        .map(item -> new AdminConfigurationContracts.MailboxConfigResponse(item.getId(), item.getName(), item.getHost(), item.getUsername(), item.isActive(), item.getCreatedAt()))
        .toList();
  }

  public AdminConfigurationContracts.MailboxConfigResponse createMailbox(AdminConfigurationContracts.MailboxConfigRequest request, UUID actorUserId) {
    MailboxConfig item = new MailboxConfig();
    apply(item, request);
    MailboxConfig saved = mailboxConfigRepository.save(item);
    audit("MAILBOX_CREATED", actorUserId, saved.getId().toString(), Map.of("name", saved.getName()));
    return toResponse(saved);
  }

  public AdminConfigurationContracts.MailboxConfigResponse updateMailbox(UUID id, AdminConfigurationContracts.MailboxConfigRequest request, UUID actorUserId) {
    MailboxConfig item = mailboxConfigRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Mailbox not found: " + id));
    apply(item, request);
    MailboxConfig saved = mailboxConfigRepository.save(item);
    audit("MAILBOX_UPDATED", actorUserId, saved.getId().toString(), Map.of("name", saved.getName()));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public AdminConfigurationContracts.ReviewSettingResponse getReviewSetting() {
    ReviewSetting setting = reviewSettingRepository.findAll().stream().findFirst().orElseGet(this::defaultReviewSetting);
    return new AdminConfigurationContracts.ReviewSettingResponse(setting.getId(), setting.getMode(), setting.getLowConfidenceThreshold(), setting.getUpdatedAt());
  }

  public AdminConfigurationContracts.ReviewSettingResponse upsertReviewSetting(AdminConfigurationContracts.ReviewSettingRequest request, UUID actorUserId) {
    ReviewSetting setting = reviewSettingRepository.findAll().stream().findFirst().orElseGet(ReviewSetting::new);
    setting.setMode(request.mode().trim());
    setting.setLowConfidenceThreshold(request.lowConfidenceThreshold());
    setting.setUpdatedAt(Instant.now());
    ReviewSetting saved = reviewSettingRepository.save(setting);
    audit("REVIEW_SETTING_UPDATED", actorUserId, saved.getId().toString(), Map.of("mode", saved.getMode()));
    return new AdminConfigurationContracts.ReviewSettingResponse(saved.getId(), saved.getMode(), saved.getLowConfidenceThreshold(), saved.getUpdatedAt());
  }

  @Transactional(readOnly = true)
  public AdminConfigurationContracts.AiProviderSettingResponse getAiProviderSetting() {
    AiProviderSetting setting = aiProviderSettingRepository.findAll().stream().findFirst().orElseGet(this::defaultAiSetting);
    return toAiResponse(setting);
  }

  public AdminConfigurationContracts.AiProviderSettingResponse upsertAiProviderSetting(AdminConfigurationContracts.AiProviderSettingRequest request, UUID actorUserId) {
    AiProviderSetting setting = aiProviderSettingRepository.findAll().stream().findFirst().orElseGet(AiProviderSetting::new);
    setting.setProviderName(request.providerName().trim());
    setting.setModelName(request.modelName().trim());
    setting.setApiBaseUrl(trimToNull(request.apiBaseUrl()));
    if (request.apiKey() != null && !request.apiKey().isBlank()) {
      setting.setApiKey(request.apiKey().trim());
    }
    setting.setOcrProvider(request.ocrProvider().trim());
    setting.setActive(request.active());
    setting.setUpdatedAt(Instant.now());
    AiProviderSetting saved = aiProviderSettingRepository.save(setting);
    audit(
        "AI_PROVIDER_SETTING_UPDATED",
        actorUserId,
        saved.getId().toString(),
        Map.of(
            "provider", saved.getProviderName(),
            "model", saved.getModelName(),
            "apiBaseUrl", saved.getApiBaseUrl() == null ? "" : saved.getApiBaseUrl(),
            "apiKeyConfigured", Boolean.toString(saved.getApiKey() != null && !saved.getApiKey().isBlank())));
    return toAiResponse(saved);
  }

  private void apply(MailboxConfig item, AdminConfigurationContracts.MailboxConfigRequest request) {
    item.setName(request.name().trim());
    item.setHost(request.host().trim());
    item.setUsername(request.username().trim());
    item.setActive(request.active());
  }

  private AdminConfigurationContracts.MailboxConfigResponse toResponse(MailboxConfig item) {
    return new AdminConfigurationContracts.MailboxConfigResponse(item.getId(), item.getName(), item.getHost(), item.getUsername(), item.isActive(), item.getCreatedAt());
  }

  private ReviewSetting defaultReviewSetting() {
    ReviewSetting setting = new ReviewSetting();
    setting.setMode("confidence");
    setting.setLowConfidenceThreshold(0.75d);
    setting.setUpdatedAt(Instant.now());
    return setting;
  }

  private AiProviderSetting defaultAiSetting() {
    AiProviderSetting setting = new AiProviderSetting();
    setting.setProviderName("mistral");
    setting.setModelName("mistral-small");
    setting.setApiBaseUrl("");
    setting.setOcrProvider("tesseract");
    setting.setActive(true);
    setting.setUpdatedAt(Instant.now());
    return setting;
  }

  private AdminConfigurationContracts.AiProviderSettingResponse toAiResponse(AiProviderSetting setting) {
    return new AdminConfigurationContracts.AiProviderSettingResponse(
        setting.getId(),
        setting.getProviderName(),
        setting.getModelName(),
        setting.getApiBaseUrl(),
        setting.getApiKey() != null && !setting.getApiKey().isBlank(),
        setting.getOcrProvider(),
        setting.isActive(),
        setting.getUpdatedAt());
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void audit(String action, UUID actorUserId, String targetId, Map<String, String> details) {
    auditService.write(new AuditEvent(Instant.now(), "CONFIG", action, AuditOutcome.SUCCESS, actorUserId, null, "Configuration", targetId, false, details));
  }
}
