package com.ikms.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.audit.AuditService;
import com.ikms.client.Client;
import com.ikms.client.ClientRepository;
import com.ikms.client.ClientStatus;
import com.ikms.client.ClientType;
import com.ikms.config.AppSettingService;
import com.ikms.config.domain.AiProviderSetting;
import com.ikms.config.domain.AiProviderSettingRepository;
import com.ikms.config.domain.DocumentType;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.config.domain.MailboxConfig;
import com.ikms.config.domain.MailboxConfigRepository;
import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataFieldRepository;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.config.domain.ReviewSetting;
import com.ikms.config.domain.ReviewSettingRepository;
import com.ikms.config.domain.SharedFolderConfig;
import com.ikms.config.domain.SharedFolderConfigRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentProcessingField;
import com.ikms.document.DocumentProcessingFieldRepository;
import com.ikms.document.DocumentProcessingFieldType;
import com.ikms.document.DocumentProcessingFieldValidationState;
import com.ikms.document.DocumentProcessingFinding;
import com.ikms.document.DocumentProcessingFindingRepository;
import com.ikms.document.DocumentProcessingFindingSeverity;
import com.ikms.document.DocumentProcessingFindingStatus;
import com.ikms.document.DocumentProcessingJob;
import com.ikms.document.DocumentProcessingJobRepository;
import com.ikms.document.DocumentProcessingJobStatus;
import com.ikms.document.DocumentProcessingStage;
import com.ikms.document.DocumentProcessingStatus;
import com.ikms.document.DocumentPublishingService;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentReviewStatus;
import com.ikms.document.DocumentSource;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.document.RedactionStatus;
import com.ikms.email.Email;
import com.ikms.email.EmailProcessingStatus;
import com.ikms.email.EmailRepository;
import com.ikms.email.EmailReviewStatus;
import com.ikms.governance.DocumentLifecycleState;
import com.ikms.governance.HoldType;
import com.ikms.governance.InformationClassification;
import com.ikms.governance.SensitivityLevel;
import com.ikms.note.Note;
import com.ikms.note.NoteRepository;
import com.ikms.note.NoteStatus;
import com.ikms.operations.OperationsJob;
import com.ikms.operations.OperationsJobRepository;
import com.ikms.operations.OperationsMetric;
import com.ikms.operations.OperationsMetricRepository;
import com.ikms.operations.OperationsQueueState;
import com.ikms.operations.OperationsQueueStateRepository;
import com.ikms.operations.OperationsScheduler;
import com.ikms.operations.OperationsSchedulerExecution;
import com.ikms.operations.OperationsSchedulerExecutionRepository;
import com.ikms.operations.OperationsSchedulerRepository;
import com.ikms.quality.KnowledgeQualityIssue;
import com.ikms.quality.KnowledgeQualityIssueRepository;
import com.ikms.quality.KnowledgeQualityIssueSeverity;
import com.ikms.quality.KnowledgeQualityIssueStatus;
import com.ikms.quality.KnowledgeQualityReadinessState;
import com.ikms.quality.KnowledgeQualitySnapshot;
import com.ikms.quality.KnowledgeQualitySnapshotRepository;
import com.ikms.retention.RetentionRecord;
import com.ikms.retention.RetentionRecordRepository;
import com.ikms.review.ReviewQueueItem;
import com.ikms.review.ReviewQueueItemType;
import com.ikms.review.ReviewQueueReason;
import com.ikms.review.ReviewQueueRepository;
import com.ikms.review.ReviewQueueStatus;
import com.ikms.security.domain.AppUser;
import com.ikms.security.domain.AppUserRepository;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IkmsDemoDataSeeder {

  private static final String APP_SETTING_NAMESPACE_KEY = "demo.dataset.namespace";
  private static final String APP_SETTING_REFERENCE_DATE_KEY = "demo.dataset.reference-date";

  private final ClientRepository clientRepository;
  private final NoteRepository noteRepository;
  private final EmailRepository emailRepository;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentProcessingJobRepository documentProcessingJobRepository;
  private final DocumentProcessingFieldRepository documentProcessingFieldRepository;
  private final DocumentProcessingFindingRepository documentProcessingFindingRepository;
  private final ReviewQueueRepository reviewQueueRepository;
  private final KnowledgeQualitySnapshotRepository snapshotRepository;
  private final KnowledgeQualityIssueRepository issueRepository;
  private final AppUserRepository appUserRepository;
  private final DocumentTypeRepository documentTypeRepository;
  private final MetadataFieldRepository metadataFieldRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final SharedFolderConfigRepository sharedFolderConfigRepository;
  private final MailboxConfigRepository mailboxConfigRepository;
  private final ReviewSettingRepository reviewSettingRepository;
  private final AiProviderSettingRepository aiProviderSettingRepository;
  private final RetentionRecordRepository retentionRecordRepository;
  private final OperationsJobRepository operationsJobRepository;
  private final OperationsQueueStateRepository operationsQueueStateRepository;
  private final OperationsSchedulerRepository operationsSchedulerRepository;
  private final OperationsSchedulerExecutionRepository operationsSchedulerExecutionRepository;
  private final OperationsMetricRepository operationsMetricRepository;
  private final PasswordEncoder passwordEncoder;
  private final DocumentPublishingService documentPublishingService;
  private final EmbeddingIndexService embeddingIndexService;
  private final AuditService auditService;
  private final AppSettingService appSettingService;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final EntityManager entityManager;

  public IkmsDemoDataSeeder(
      ClientRepository clientRepository,
      NoteRepository noteRepository,
      EmailRepository emailRepository,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentProcessingJobRepository documentProcessingJobRepository,
      DocumentProcessingFieldRepository documentProcessingFieldRepository,
      DocumentProcessingFindingRepository documentProcessingFindingRepository,
      ReviewQueueRepository reviewQueueRepository,
      KnowledgeQualitySnapshotRepository snapshotRepository,
      KnowledgeQualityIssueRepository issueRepository,
      AppUserRepository appUserRepository,
      DocumentTypeRepository documentTypeRepository,
      MetadataFieldRepository metadataFieldRepository,
      MetadataValueRepository metadataValueRepository,
      SharedFolderConfigRepository sharedFolderConfigRepository,
      MailboxConfigRepository mailboxConfigRepository,
      ReviewSettingRepository reviewSettingRepository,
      AiProviderSettingRepository aiProviderSettingRepository,
      RetentionRecordRepository retentionRecordRepository,
      OperationsJobRepository operationsJobRepository,
      OperationsQueueStateRepository operationsQueueStateRepository,
      OperationsSchedulerRepository operationsSchedulerRepository,
      OperationsSchedulerExecutionRepository operationsSchedulerExecutionRepository,
      OperationsMetricRepository operationsMetricRepository,
      PasswordEncoder passwordEncoder,
      DocumentPublishingService documentPublishingService,
      EmbeddingIndexService embeddingIndexService,
      AuditService auditService,
      AppSettingService appSettingService,
      ObjectMapper objectMapper,
      JdbcTemplate jdbcTemplate,
      EntityManager entityManager) {
    this.clientRepository = clientRepository;
    this.noteRepository = noteRepository;
    this.emailRepository = emailRepository;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentProcessingJobRepository = documentProcessingJobRepository;
    this.documentProcessingFieldRepository = documentProcessingFieldRepository;
    this.documentProcessingFindingRepository = documentProcessingFindingRepository;
    this.reviewQueueRepository = reviewQueueRepository;
    this.snapshotRepository = snapshotRepository;
    this.issueRepository = issueRepository;
    this.appUserRepository = appUserRepository;
    this.documentTypeRepository = documentTypeRepository;
    this.metadataFieldRepository = metadataFieldRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.sharedFolderConfigRepository = sharedFolderConfigRepository;
    this.mailboxConfigRepository = mailboxConfigRepository;
    this.reviewSettingRepository = reviewSettingRepository;
    this.aiProviderSettingRepository = aiProviderSettingRepository;
    this.retentionRecordRepository = retentionRecordRepository;
    this.operationsJobRepository = operationsJobRepository;
    this.operationsQueueStateRepository = operationsQueueStateRepository;
    this.operationsSchedulerRepository = operationsSchedulerRepository;
    this.operationsSchedulerExecutionRepository = operationsSchedulerExecutionRepository;
    this.operationsMetricRepository = operationsMetricRepository;
    this.passwordEncoder = passwordEncoder;
    this.documentPublishingService = documentPublishingService;
    this.embeddingIndexService = embeddingIndexService;
    this.auditService = auditService;
    this.appSettingService = appSettingService;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
    this.entityManager = entityManager;
  }

  public SeedReport seed(String namespace, LocalDate referenceDate) {
    DemoCatalog catalog = DemoCatalog.build(namespace, referenceDate);
    // Audit rows are intentionally immutable in normal runtime flows. For demo reseeds,
    // clear only this namespace's synthetic audit events before replaying them.
    jdbcTemplate.update("delete from audit_log where details::text like ?", "%" + namespace + "%");
    ensureGovernanceSettings(catalog);
    ensureUsers(catalog);
    ensureAdminConfiguration(catalog);
    ensureClients(catalog);
    ensureNotes(catalog);
    ensureEmails(catalog);
    ensureDocuments(catalog);
    ensureReviews(catalog);
    ensureQualitySnapshots(catalog);
    ensureRetention(catalog);
    ensureOperations(catalog);
    ensureAuditHistory(catalog);
    appSettingService.put(APP_SETTING_NAMESPACE_KEY, namespace, "IKMS deterministic demo dataset namespace.");
    appSettingService.put(APP_SETTING_REFERENCE_DATE_KEY, referenceDate.toString(), "IKMS deterministic demo dataset reference date.");
    entityManager.flush();
    return new SeedReport("seed", countDemoRows(catalog));
  }

  public SeedReport reset(String namespace) {
    DemoCatalog catalog = DemoCatalog.build(namespace, LocalDate.parse(appSettingService.get(APP_SETTING_REFERENCE_DATE_KEY).orElse("2026-07-18")));

    jdbcTemplate.update("delete from audit_log where details::text like ?", "%" + namespace + "%");

    for (UUID reviewId : catalog.reviewIds()) {
      reviewQueueRepository.findById(reviewId).ifPresent(reviewQueueRepository::delete);
    }
    for (UUID issueId : catalog.qualityIssueIds()) {
      issueRepository.findById(issueId).ifPresent(issueRepository::delete);
    }
    for (UUID snapshotId : catalog.snapshotIds()) {
      snapshotRepository.findById(snapshotId).ifPresent(snapshotRepository::delete);
    }
    for (UUID processingFindingId : catalog.processingFindingIds()) {
      documentProcessingFindingRepository.findById(processingFindingId).ifPresent(documentProcessingFindingRepository::delete);
    }
    for (UUID processingFieldId : catalog.processingFieldIds()) {
      documentProcessingFieldRepository.findById(processingFieldId).ifPresent(documentProcessingFieldRepository::delete);
    }
    for (UUID processingJobId : catalog.processingJobIds()) {
      documentProcessingJobRepository.findById(processingJobId).ifPresent(documentProcessingJobRepository::delete);
    }
    for (UUID versionId : catalog.documentVersionIds()) {
      documentVersionRepository.findById(versionId).ifPresent(documentVersionRepository::delete);
    }
    for (UUID documentId : catalog.documentIds()) {
      metadataValueRepository.deleteByOwnerTypeAndOwnerId("DOCUMENT", documentId);
      documentRepository.findById(documentId).ifPresent(documentRepository::delete);
    }
    for (UUID emailId : catalog.emailIds()) {
      emailRepository.findById(emailId).ifPresent(emailRepository::delete);
    }
    for (UUID noteId : catalog.noteIds()) {
      noteRepository.findById(noteId).ifPresent(noteRepository::delete);
    }
    for (UUID retentionId : catalog.retentionIds()) {
      retentionRecordRepository.findById(retentionId).ifPresent(retentionRecordRepository::delete);
    }
    for (UUID operationsJobId : catalog.operationsJobIds()) {
      operationsJobRepository.findById(operationsJobId).ifPresent(operationsJobRepository::delete);
    }
    for (UUID schedulerExecutionId : catalog.schedulerExecutionIds()) {
      operationsSchedulerExecutionRepository.findById(schedulerExecutionId).ifPresent(operationsSchedulerExecutionRepository::delete);
    }
    for (UUID operationsMetricId : catalog.operationsMetricIds()) {
      operationsMetricRepository.findById(operationsMetricId).ifPresent(operationsMetricRepository::delete);
    }
    for (String queueKey : catalog.demoQueueKeys()) {
      operationsQueueStateRepository.findById(queueKey).ifPresent(operationsQueueStateRepository::delete);
    }
    for (UUID clientId : catalog.clientIds()) {
      clientRepository.findById(clientId).ifPresent(clientRepository::delete);
    }
    for (UUID userId : catalog.userIds()) {
      appUserRepository.findById(userId).ifPresent(appUserRepository::delete);
    }
    for (UUID mailboxId : catalog.mailboxIds()) {
      mailboxConfigRepository.findById(mailboxId).ifPresent(mailboxConfigRepository::delete);
    }
    for (UUID folderId : catalog.sharedFolderIds()) {
      sharedFolderConfigRepository.findById(folderId).ifPresent(sharedFolderConfigRepository::delete);
    }
    for (UUID aiSettingId : catalog.aiProviderSettingIds()) {
      aiProviderSettingRepository.findById(aiSettingId).ifPresent(aiProviderSettingRepository::delete);
    }
    for (UUID reviewSettingId : catalog.reviewSettingIds()) {
      reviewSettingRepository.findById(reviewSettingId).ifPresent(reviewSettingRepository::delete);
    }
    for (UUID metadataFieldId : catalog.metadataFieldIds()) {
      metadataFieldRepository.findById(metadataFieldId).ifPresent(metadataFieldRepository::delete);
    }
    for (UUID documentTypeId : catalog.documentTypeIds()) {
      documentTypeRepository.findById(documentTypeId).ifPresent(documentTypeRepository::delete);
    }

    entityManager.flush();
    return new SeedReport("reset", countDemoRows(catalog));
  }

  private Map<String, Long> countDemoRows(DemoCatalog catalog) {
    LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
    counts.put("users", countIds("app_user", catalog.userIds()));
    counts.put("customers", countIds("client", catalog.clientIds()));
    counts.put("notes", countIds("note", catalog.noteIds()));
    counts.put("emails", countIds("email_item", catalog.emailIds()));
    counts.put("documents", countIds("document", catalog.documentIds()));
    counts.put("document_versions", countIds("document_version", catalog.documentVersionIds()));
    counts.put("document_processing_jobs", countIds("document_processing_job", catalog.processingJobIds()));
    counts.put("review_items", countIds("review_queue_item", catalog.reviewIds()));
    counts.put("quality_snapshots", countIds("knowledge_quality_snapshot", catalog.snapshotIds()));
    counts.put("quality_issues", countIds("knowledge_quality_issue", catalog.qualityIssueIds()));
    counts.put("retention_records", countIds("retention_record", catalog.retentionIds()));
    counts.put("operations_jobs", countIds("operations_job", catalog.operationsJobIds()));
    counts.put("operations_metrics", countIds("operations_metric", catalog.operationsMetricIds()));
    counts.put("audit_events", jdbcTemplate.queryForObject(
        "select count(*) from audit_log where details::text like ?",
        Long.class,
        "%" + catalog.namespace() + "%"));
    return counts;
  }

  private long countIds(String table, Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return 0L;
    }
    return ids.stream()
        .filter(id -> Boolean.TRUE.equals(jdbcTemplate.queryForObject(
            "select exists(select 1 from " + table + " where id = ?::uuid)",
            Boolean.class,
            id.toString())))
        .count();
  }

  private void ensureGovernanceSettings(DemoCatalog catalog) {
    putJsonSetting("governance.classification.policy", Map.of(
        "levels", List.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED", "HIGHLY_RESTRICTED"),
        "defaultClassification", "INTERNAL",
        "aiRestrictionThreshold", "RESTRICTED",
        "exportRestrictionThreshold", "CONFIDENTIAL",
        "updatedAt", catalog.instant("governance-updated").toString()));
    putJsonSetting("governance.retention.policy", Map.of(
        "policies", List.of(
            Map.of("contentType", "CUSTOMER_DOCUMENT", "retentionDays", 2555, "reviewAfterDays", 1825, "archivalAfterDays", 2190, "disposalAfterDays", 2555),
            Map.of("contentType", "EMAIL", "retentionDays", 1825, "reviewAfterDays", 1460, "archivalAfterDays", 1642, "disposalAfterDays", 1825),
            Map.of("contentType", "NOTE", "retentionDays", 1095, "reviewAfterDays", 730, "archivalAfterDays", 912, "disposalAfterDays", 1095)),
        "updatedAt", catalog.instant("governance-updated").toString()));
    putJsonSetting("governance.ai.policy", Map.of(
        "approvedModels", List.of("local-demo:ikms-synthetic-chat", "local-demo:ikms-synthetic-embed"),
        "promptPolicyVersion", "demo-prompt-v1",
        "responsePolicyVersion", "demo-response-v1",
        "citationRequired", true,
        "groundingValidationRequired", true,
        "updatedAt", catalog.instant("governance-updated").toString()));
    putJsonSetting("governance.security.policy", Map.of(
        "encryptionAtRest", "AES-256 synthetic local development profile.",
        "encryptionInTransit", "TLS 1.2 synthetic local development profile.",
        "keyManagement", "Local demo keystore alias ikms-demo-keystore.",
        "secretManagement", "Environment-backed local development placeholders.",
        "exportApprovalRequired", true,
        "watermarkByDefault", true,
        "updatedAt", catalog.instant("governance-updated").toString()));
  }

  private void putJsonSetting(String key, Object value) {
    try {
      appSettingService.put(key, objectMapper.writeValueAsString(value), "IKMS demo dataset setting.");
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to write demo setting " + key, exception);
    }
  }

  private void ensureUsers(DemoCatalog catalog) {
    for (DemoUser spec : catalog.users()) {
      AppUser user = appUserRepository.findByUsernameIgnoreCase(spec.username()).orElseGet(AppUser::new);
      if (user.getId() == null) {
        user.setId(spec.id());
      }
      user.setUsername(spec.username());
      user.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
      user.setDisplayName(spec.displayName());
      user.setEmail(spec.email());
      user.setStatus(spec.status());
      user.setRoles(spec.roles());
      user.setBusinessUnit("Enterprise Broking");
      user.setDepartment("Knowledge Operations");
      user.setRegion("North America");
      user.setCountry("USA");
      user.setBrokerOffice("Demo Operations Hub");
      user.setEmploymentRole(spec.employmentRole());
      user.setSecurityClearance(spec.clearance());
      appUserRepository.save(user);
    }
  }

  private void ensureAdminConfiguration(DemoCatalog catalog) {
    for (DemoDocumentType spec : catalog.documentTypes()) {
      DocumentType documentType = documentTypeRepository.findById(spec.id()).orElseGet(DocumentType::new);
      documentType.setId(spec.id());
      documentType.setName(spec.name());
      documentType.setDescription(spec.description());
      documentType.setActive(spec.active());
      documentType.setCreatedAt(catalog.instant("config-created"));
      documentTypeRepository.save(documentType);
    }

    for (DemoMetadataField spec : catalog.metadataFields()) {
      MetadataField field = metadataFieldRepository.findById(spec.id()).orElseGet(MetadataField::new);
      field.setId(spec.id());
      field.setFieldKey(spec.fieldKey());
      field.setLabel(spec.label());
      field.setPii(spec.pii());
      field.setActive(spec.active());
      field.setCreatedAt(catalog.instant("config-created"));
      metadataFieldRepository.save(field);
    }

    for (DemoSharedFolder spec : catalog.sharedFolders()) {
      SharedFolderConfig folder = sharedFolderConfigRepository.findById(spec.id()).orElseGet(SharedFolderConfig::new);
      folder.setId(spec.id());
      folder.setPath(spec.path());
      folder.setActive(spec.active());
      folder.setCreatedAt(catalog.instant("config-created"));
      sharedFolderConfigRepository.save(folder);
    }

    for (DemoMailbox spec : catalog.mailboxes()) {
      MailboxConfig mailbox = mailboxConfigRepository.findById(spec.id()).orElseGet(MailboxConfig::new);
      mailbox.setId(spec.id());
      mailbox.setName(spec.name());
      mailbox.setHost(spec.host());
      mailbox.setUsername(spec.username());
      mailbox.setActive(spec.active());
      mailbox.setCreatedAt(catalog.instant("config-created"));
      mailboxConfigRepository.save(mailbox);
    }

    ReviewSetting reviewSetting = reviewSettingRepository.findById(catalog.reviewSettingId()).orElseGet(ReviewSetting::new);
    reviewSetting.setId(catalog.reviewSettingId());
    reviewSetting.setMode("confidence");
    reviewSetting.setLowConfidenceThreshold(0.72d);
    reviewSetting.setUpdatedAt(catalog.instant("config-updated"));
    reviewSettingRepository.save(reviewSetting);

    AiProviderSetting aiSetting = aiProviderSettingRepository.findById(catalog.aiProviderSettingId()).orElseGet(AiProviderSetting::new);
    aiSetting.setId(catalog.aiProviderSettingId());
    aiSetting.setProviderName("local-demo");
    aiSetting.setModelName("ikms-synthetic-chat");
    aiSetting.setEmbeddingModelName("ikms-synthetic-embed");
    aiSetting.setApiBaseUrl(null);
    aiSetting.setApiKey(null);
    aiSetting.setOcrProvider("synthetic-ocr");
    aiSetting.setActive(false);
    aiSetting.setUpdatedAt(catalog.instant("config-updated"));
    aiProviderSettingRepository.save(aiSetting);
  }

  private void ensureClients(DemoCatalog catalog) {
    for (DemoClient spec : catalog.clients()) {
      Client client = clientRepository.findById(spec.id()).orElseGet(Client::new);
      client.setId(spec.id());
      client.setClientId(spec.externalId());
      client.setClientIdTemporary(spec.temporaryId());
      client.setClientType(spec.type());
      client.setStatus(spec.status());
      client.setDisplayName(spec.displayName());
      client.setLegalName(spec.legalName());
      client.setPrimaryEmail(spec.primaryEmail());
      client.setSecondaryEmail(spec.secondaryEmail());
      client.setPrimaryPhone(spec.primaryPhone());
      client.setSecondaryPhone(spec.secondaryPhone());
      client.setContactPerson(spec.contactPerson());
      client.setAddressLine1(spec.addressLine1());
      client.setAddressLine2(spec.addressLine2());
      client.setCity(spec.city());
      client.setStateOrRegion(spec.region());
      client.setPostalCode(spec.postalCode());
      client.setCountry(spec.country());
      client.setCreatedAt(spec.createdAt());
      client.setUpdatedAt(spec.updatedAt());
      clientRepository.save(client);
    }
  }

  private void ensureNotes(DemoCatalog catalog) {
    for (DemoNote spec : catalog.notes()) {
      Note note = noteRepository.findById(spec.id()).orElseGet(Note::new);
      note.setId(spec.id());
      note.setClient(requireClient(spec.clientId()));
      note.setNoteText(spec.text());
      note.setStatus(spec.status());
      note.setCreatedBy(spec.createdBy());
      note.setUpdatedBy(spec.updatedBy());
      note.setCreatedAt(spec.createdAt());
      note.setUpdatedAt(spec.updatedAt());
      noteRepository.save(note);
      embeddingIndexService.indexNote(spec.clientId(), note);
    }
  }

  private void ensureEmails(DemoCatalog catalog) {
    for (DemoEmail spec : catalog.emails()) {
      Email email = emailRepository.findById(spec.id()).orElseGet(Email::new);
      email.setId(spec.id());
      email.setClient(spec.clientId() == null ? null : requireClient(spec.clientId()));
      email.setMailboxConfigId(spec.mailboxConfigId());
      email.setMessageId(spec.messageId());
      email.setThreadId(spec.threadId());
      email.setSubject(spec.subject());
      email.setSender(spec.sender());
      email.setRecipients(spec.recipients());
      email.setCc(spec.cc());
      email.setReceivedAt(spec.receivedAt());
      email.setBodyText(spec.bodyText());
      email.setBodyHtmlStoragePath(spec.bodyHtmlStoragePath());
      email.setProcessingStatus(spec.processingStatus());
      email.setReviewStatus(spec.reviewStatus());
      email.setCreatedAt(spec.createdAt());
      emailRepository.save(email);
      if (spec.clientId() != null) {
        embeddingIndexService.indexEmail(spec.clientId(), email);
      }
    }
  }

  private void ensureDocuments(DemoCatalog catalog) {
    Map<String, MetadataField> metadataFieldLookup = new LinkedHashMap<>();
    for (DemoMetadataField field : catalog.metadataFields()) {
      metadataFieldLookup.put(field.fieldKey(), metadataFieldRepository.findById(field.id()).orElseThrow());
    }

    for (DemoDocument spec : catalog.documents()) {
      Document document = documentRepository.findById(spec.id()).orElseGet(Document::new);
      document.setId(spec.id());
      document.setClient(spec.clientId() == null ? null : requireClient(spec.clientId()));
      document.setParentEmail(spec.parentEmailId() == null ? null : requireEmail(spec.parentEmailId()));
      document.setDocumentTypeId(spec.documentTypeId());
      document.setTitle(spec.title());
      document.setProcessingStatus(spec.processingStatus());
      document.setReviewStatus(spec.reviewStatus());
      document.setClientMatchConfidence(spec.clientConfidence());
      document.setClassificationConfidence(spec.classificationConfidence());
      document.setExtractionConfidence(spec.extractionConfidence());
      document.setSource(spec.source());
      document.setClassification(spec.classification());
      document.setSensitivityLevel(spec.sensitivityLevel());
      document.setConfidentiality(spec.confidentiality());
      document.setDataResidency("US");
      document.setLifecycleState(spec.lifecycleState());
      document.setExportRestricted(spec.exportRestricted());
      document.setWatermarkRequired(spec.watermarkRequired());
      document.setBusinessUnit("Enterprise Broking");
      document.setDepartment(spec.department());
      document.setRegion(spec.region());
      document.setCountry("USA");
      document.setBrokerOffice(spec.brokerOffice());
      document.setCreatedAt(spec.createdAt());
      documentRepository.save(document);

      UUID currentVersionId = null;
      for (DemoDocumentVersion versionSpec : spec.versions()) {
        DocumentVersion version = documentVersionRepository.findById(versionSpec.id()).orElseGet(DocumentVersion::new);
        version.setId(versionSpec.id());
        version.setDocument(document);
        version.setVersionNumber(versionSpec.versionNumber());
        version.setFileHash(versionSpec.fileHash());
        version.setFileName(versionSpec.fileName());
        version.setMimeType(versionSpec.mimeType());
        version.setFileSizeBytes(versionSpec.fileSizeBytes());
        version.setOriginalStoragePath(versionSpec.originalStoragePath());
        version.setRedactedStoragePath(versionSpec.redactedStoragePath());
        version.setExtractedText(versionSpec.extractedText());
        version.setOcrProvider(versionSpec.ocrProvider());
        version.setEmbeddingModel(versionSpec.embeddingModel());
        version.setLanguage(versionSpec.language());
        version.setRedactionStatus(versionSpec.redactionStatus());
        version.setCurrent(versionSpec.current());
        version.setCreatedBy(versionSpec.createdBy());
        version.setCreatedAt(versionSpec.createdAt());
        documentVersionRepository.save(version);
        if (versionSpec.current()) {
          currentVersionId = versionSpec.id();
        }
      }
      document.setCurrentVersionId(currentVersionId);
      documentRepository.save(document);

      metadataValueRepository.deleteByOwnerTypeAndOwnerId("DOCUMENT", document.getId());
      for (Map.Entry<String, String> metadataEntry : spec.metadataValues().entrySet()) {
        MetadataValue metadataValue = new MetadataValue();
        metadataValue.setId(catalog.id("metadata-value-" + document.getId() + "-" + metadataEntry.getKey()));
        metadataValue.setOwnerType("DOCUMENT");
        metadataValue.setOwnerId(document.getId());
        metadataValue.setField(metadataFieldLookup.get(metadataEntry.getKey()));
        metadataValue.setTextValue(metadataEntry.getValue());
        metadataValue.setCreatedAt(spec.createdAt());
        metadataValue.setUpdatedAt(spec.updatedAt());
        metadataValueRepository.save(metadataValue);
      }

      for (DemoProcessingJob jobSpec : spec.processingJobs()) {
        DocumentProcessingJob job = documentProcessingJobRepository.findById(jobSpec.id()).orElseGet(DocumentProcessingJob::new);
        job.setId(jobSpec.id());
        job.setDocument(document);
        job.setDocumentVersion(requireVersion(jobSpec.documentVersionId()));
        job.setClientId(spec.clientId());
        job.setStatus(jobSpec.status());
        job.setCurrentStage(jobSpec.stage());
        job.setLanguage(jobSpec.language());
        job.setOcrProvider(jobSpec.ocrProvider());
        job.setClassificationProvider(jobSpec.classificationProvider());
        job.setOverallConfidence(jobSpec.overallConfidence());
        job.setOcrConfidence(jobSpec.ocrConfidence());
        job.setClassificationConfidence(jobSpec.classificationConfidence());
        job.setMetadataConfidence(jobSpec.metadataConfidence());
        job.setBusinessReferenceConfidence(jobSpec.businessReferenceConfidence());
        job.setValidationConfidence(jobSpec.validationConfidence());
        job.setDuplicateConfidence(jobSpec.duplicateConfidence());
        job.setRetryCount(jobSpec.retryCount());
        job.setLastErrorCode(jobSpec.lastErrorCode());
        job.setLastErrorMessage(jobSpec.lastErrorMessage());
        job.setReviewerComment(jobSpec.reviewerComment());
        job.setStartedAt(jobSpec.startedAt());
        job.setReviewRequestedAt(jobSpec.reviewRequestedAt());
        job.setApprovedAt(jobSpec.approvedAt());
        job.setRejectedAt(jobSpec.rejectedAt());
        job.setPublishedAt(jobSpec.publishedAt());
        job.setCompletedAt(jobSpec.completedAt());
        job.setCreatedAt(jobSpec.createdAt());
        job.setUpdatedAt(jobSpec.updatedAt());
        documentProcessingJobRepository.save(job);

        for (DemoProcessingField fieldSpec : jobSpec.fields()) {
          DocumentProcessingField field = documentProcessingFieldRepository.findById(fieldSpec.id()).orElseGet(DocumentProcessingField::new);
          field.setId(fieldSpec.id());
          field.setJob(job);
          field.setFieldKey(fieldSpec.fieldKey());
          field.setFieldLabel(fieldSpec.fieldLabel());
          field.setFieldType(fieldSpec.fieldType());
          field.setBusinessReferenceType(fieldSpec.businessReferenceType());
          field.setExtractedValue(fieldSpec.extractedValue());
          field.setCorrectedValue(fieldSpec.correctedValue());
          field.setApprovedValue(fieldSpec.approvedValue());
          field.setConfidence(fieldSpec.confidence());
          field.setSourceType(fieldSpec.sourceType());
          field.setExtractionMethod(fieldSpec.extractionMethod());
          field.setSourcePage(fieldSpec.sourcePage());
          field.setRequiredFlag(fieldSpec.required());
          field.setValidationState(fieldSpec.validationState());
          field.setCreatedAt(fieldSpec.createdAt());
          field.setUpdatedAt(fieldSpec.updatedAt());
          documentProcessingFieldRepository.save(field);
        }

        for (DemoProcessingFinding findingSpec : jobSpec.findings()) {
          DocumentProcessingFinding finding = documentProcessingFindingRepository.findById(findingSpec.id()).orElseGet(DocumentProcessingFinding::new);
          finding.setId(findingSpec.id());
          finding.setJob(job);
          finding.setFindingCode(findingSpec.findingCode());
          finding.setSeverity(findingSpec.severity());
          finding.setStage(findingSpec.stage());
          finding.setFieldKey(findingSpec.fieldKey());
          finding.setMessage(findingSpec.message());
          finding.setEvidenceText(findingSpec.evidenceText());
          finding.setSourcePage(findingSpec.sourcePage());
          finding.setConfidence(findingSpec.confidence());
          finding.setStatus(findingSpec.status());
          finding.setResolutionComment(findingSpec.resolutionComment());
          finding.setCreatedAt(findingSpec.createdAt());
          finding.setResolvedAt(findingSpec.resolvedAt());
          documentProcessingFindingRepository.save(finding);
        }
      }

      if (spec.clientId() != null && spec.lifecycleState() != DocumentLifecycleState.DISPOSED) {
        documentPublishingService.publish(document);
      }
    }
  }

  private void ensureReviews(DemoCatalog catalog) {
    for (DemoReviewItem spec : catalog.reviewItems()) {
      ReviewQueueItem item = reviewQueueRepository.findById(spec.id()).orElseGet(ReviewQueueItem::new);
      item.setId(spec.id());
      item.setItemType(spec.itemType());
      item.setItemId(spec.itemId());
      item.setReason(spec.reason());
      item.setAssignedTo(spec.assignedTo());
      item.setStatus(spec.status());
      item.setCreatedAt(spec.createdAt());
      item.setResolvedAt(spec.resolvedAt());
      reviewQueueRepository.save(item);
    }
  }

  private void ensureQualitySnapshots(DemoCatalog catalog) {
    for (DemoQualitySnapshot spec : catalog.qualitySnapshots()) {
      KnowledgeQualitySnapshot snapshot = snapshotRepository.findById(spec.id()).orElseGet(KnowledgeQualitySnapshot::new);
      snapshot.setId(spec.id());
      snapshot.setClient(requireClient(spec.clientId()));
      snapshot.setOverallScore(spec.overallScore());
      snapshot.setCompletenessScore(spec.completenessScore());
      snapshot.setBusinessReferenceScore(spec.businessReferenceScore());
      snapshot.setLinkageScore(spec.linkageScore());
      snapshot.setDuplicateScore(spec.duplicateScore());
      snapshot.setTimelineScore(spec.timelineScore());
      snapshot.setVersionScore(spec.versionScore());
      snapshot.setRetrievalReadinessScore(spec.retrievalReadinessScore());
      snapshot.setAiQualityScore(spec.aiQualityScore());
      snapshot.setIssueCount(spec.issueCount());
      snapshot.setOpenIssueCount(spec.openIssueCount());
      snapshot.setReadinessState(spec.readinessState());
      snapshot.setSummaryText(spec.summaryText());
      snapshot.setEvaluatedAt(spec.evaluatedAt());
      snapshot.setUpdatedAt(spec.updatedAt());
      snapshotRepository.save(snapshot);
    }

    for (DemoQualityIssue spec : catalog.qualityIssues()) {
      KnowledgeQualityIssue issue = issueRepository.findById(spec.id()).orElseGet(KnowledgeQualityIssue::new);
      issue.setId(spec.id());
      issue.setSnapshot(requireSnapshot(spec.snapshotId()));
      issue.setClientId(spec.clientId());
      issue.setSourceType(spec.sourceType());
      issue.setSourceId(spec.sourceId());
      issue.setCategory(spec.category());
      issue.setIssueType(spec.issueType());
      issue.setSeverity(spec.severity());
      issue.setStatus(spec.status());
      issue.setTitle(spec.title());
      issue.setDetailText(spec.detail());
      issue.setRecommendationType(spec.recommendationType());
      issue.setRecommendationDetail(spec.recommendationDetail());
      issue.setBusinessReferenceKey(spec.businessReferenceKey());
      issue.setScoreImpact(spec.scoreImpact());
      issue.setCreatedAt(spec.createdAt());
      issue.setUpdatedAt(spec.updatedAt());
      issueRepository.save(issue);
    }
  }

  private void ensureRetention(DemoCatalog catalog) {
    for (DemoRetentionRecord spec : catalog.retentionRecords()) {
      RetentionRecord record = retentionRecordRepository.findById(spec.id()).orElseGet(RetentionRecord::new);
      record.setId(spec.id());
      record.setTargetType(spec.targetType());
      record.setTargetId(spec.targetId());
      record.setClientId(spec.clientId());
      record.setLegalHold(spec.legalHold());
      record.setMinimumRetentionUntil(spec.minimumRetentionUntil());
      record.setHoldType(spec.holdType());
      record.setRetentionPolicyKey(spec.retentionPolicyKey());
      record.setReviewAt(spec.reviewAt());
      record.setArchivalEligibleAt(spec.archivalEligibleAt());
      record.setDisposalEligibleAt(spec.disposalEligibleAt());
      record.setLastAction(spec.lastAction());
      record.setLastOutcome(spec.lastOutcome());
      record.setLastReason(spec.lastReason());
      record.setExecutedAt(spec.executedAt());
      record.setCreatedAt(spec.createdAt());
      record.setUpdatedAt(spec.updatedAt());
      retentionRecordRepository.save(record);
    }
  }

  private void ensureOperations(DemoCatalog catalog) {
    for (DemoQueueState spec : catalog.queueStates()) {
      OperationsQueueState state = operationsQueueStateRepository.findById(spec.queueKey()).orElseGet(OperationsQueueState::new);
      state.setQueueKey(spec.queueKey());
      state.setPaused(spec.paused());
      state.setPausedAt(spec.pausedAt());
      state.setResumedAt(spec.resumedAt());
      state.setUpdatedAt(spec.updatedAt());
      state.setUpdatedBy(spec.updatedBy());
      operationsQueueStateRepository.save(state);
    }

    for (DemoOperationsJob spec : catalog.operationsJobs()) {
      OperationsJob job = operationsJobRepository.findById(spec.id()).orElseGet(OperationsJob::new);
      job.setId(spec.id());
      job.setJobType(spec.jobType());
      job.setSubmittedBy(spec.submittedBy());
      job.setSubmittedAt(spec.submittedAt());
      job.setStartedAt(spec.startedAt());
      job.setCompletedAt(spec.completedAt());
      job.setDurationMs(spec.durationMs());
      job.setStatus(spec.status());
      job.setProgress(spec.progress());
      job.setErrorSummary(spec.errorSummary());
      job.setRetryCount(spec.retryCount());
      job.setTargetType(spec.targetType());
      job.setTargetId(spec.targetId());
      job.setQueueKey(spec.queueKey());
      job.setCancelRequested(spec.cancelRequested());
      job.setPriority(spec.priority());
      job.setDetails(spec.details());
      job.setCreatedAt(spec.createdAt());
      job.setUpdatedAt(spec.updatedAt());
      operationsJobRepository.save(job);
    }

    for (DemoSchedulerExecution spec : catalog.schedulerExecutions()) {
      OperationsSchedulerExecution execution = operationsSchedulerExecutionRepository.findById(spec.id()).orElseGet(OperationsSchedulerExecution::new);
      execution.setId(spec.id());
      execution.setSchedulerKey(spec.schedulerKey());
      execution.setTriggeredBy(spec.triggeredBy());
      execution.setTriggerSource(spec.triggerSource());
      execution.setStatus(spec.status());
      execution.setDetails(spec.details());
      execution.setStartedAt(spec.startedAt());
      execution.setCompletedAt(spec.completedAt());
      operationsSchedulerExecutionRepository.save(execution);
    }

    for (DemoOperationsMetric spec : catalog.operationsMetrics()) {
      OperationsMetric metric = operationsMetricRepository.findById(spec.id()).orElseGet(OperationsMetric::new);
      metric.setId(spec.id());
      metric.setMetricGroup(spec.metricGroup());
      metric.setMetricKey(spec.metricKey());
      metric.setMetricValue(spec.metricValue());
      metric.setMetricUnit(spec.metricUnit());
      metric.setDimensionsJson(spec.dimensionsJson());
      metric.setRecordedAt(spec.recordedAt());
      operationsMetricRepository.save(metric);
    }

    for (OperationsScheduler scheduler : operationsSchedulerRepository.findAll()) {
      scheduler.setUpdatedAt(catalog.instant("operations-updated"));
      if (scheduler.getLastStatus() == null) {
        scheduler.setLastStatus("SUCCESS");
      }
      operationsSchedulerRepository.save(scheduler);
    }
  }

  private void ensureAuditHistory(DemoCatalog catalog) {
    for (DemoAuditEvent spec : catalog.auditEvents()) {
      auditService.write(new AuditService.AuditEvent(
          spec.occurredAt(),
          spec.category(),
          spec.action(),
          spec.outcome(),
          spec.actorUserId(),
          spec.clientId(),
          spec.targetType(),
          spec.targetId(),
          spec.piiAccess(),
          spec.details()));
    }
  }

  private Client requireClient(UUID id) {
    return clientRepository.findById(id).orElseThrow();
  }

  private Email requireEmail(UUID id) {
    return emailRepository.findById(id).orElseThrow();
  }

  private DocumentVersion requireVersion(UUID id) {
    return documentVersionRepository.findById(id).orElseThrow();
  }

  private KnowledgeQualitySnapshot requireSnapshot(UUID id) {
    return snapshotRepository.findById(id).orElseThrow();
  }

  public record SeedReport(String mode, Map<String, Long> counts) {
  }

  private record DemoUser(
      UUID id,
      String username,
      String displayName,
      String email,
      UserStatus status,
      Set<UserRole> roles,
      String employmentRole,
      InformationClassification clearance) {
  }

  private record DemoClient(
      UUID id,
      String externalId,
      boolean temporaryId,
      ClientType type,
      ClientStatus status,
      String displayName,
      String legalName,
      String primaryEmail,
      String secondaryEmail,
      String primaryPhone,
      String secondaryPhone,
      String contactPerson,
      String addressLine1,
      String addressLine2,
      String city,
      String region,
      String postalCode,
      String country,
      Instant createdAt,
      Instant updatedAt) {
  }

  private record DemoNote(
      UUID id,
      UUID clientId,
      String text,
      NoteStatus status,
      UUID createdBy,
      UUID updatedBy,
      Instant createdAt,
      Instant updatedAt) {
  }

  private record DemoEmail(
      UUID id,
      UUID clientId,
      UUID mailboxConfigId,
      String messageId,
      String threadId,
      String subject,
      String sender,
      String recipients,
      String cc,
      Instant receivedAt,
      String bodyText,
      String bodyHtmlStoragePath,
      EmailProcessingStatus processingStatus,
      EmailReviewStatus reviewStatus,
      Instant createdAt) {
  }

  private record DemoDocumentType(UUID id, String name, String description, boolean active) {
  }

  private record DemoMetadataField(UUID id, String fieldKey, String label, boolean pii, boolean active) {
  }

  private record DemoSharedFolder(UUID id, String path, boolean active) {
  }

  private record DemoMailbox(UUID id, String name, String host, String username, boolean active) {
  }

  private record DemoDocument(
      UUID id,
      UUID clientId,
      UUID parentEmailId,
      UUID documentTypeId,
      String title,
      DocumentProcessingStatus processingStatus,
      DocumentReviewStatus reviewStatus,
      BigDecimal clientConfidence,
      BigDecimal classificationConfidence,
      BigDecimal extractionConfidence,
      DocumentSource source,
      InformationClassification classification,
      SensitivityLevel sensitivityLevel,
      String confidentiality,
      DocumentLifecycleState lifecycleState,
      boolean exportRestricted,
      boolean watermarkRequired,
      String department,
      String region,
      String brokerOffice,
      Instant createdAt,
      Instant updatedAt,
      Map<String, String> metadataValues,
      List<DemoDocumentVersion> versions,
      List<DemoProcessingJob> processingJobs) {
  }

  private record DemoDocumentVersion(
      UUID id,
      int versionNumber,
      String fileHash,
      String fileName,
      String mimeType,
      long fileSizeBytes,
      String originalStoragePath,
      String redactedStoragePath,
      String extractedText,
      String ocrProvider,
      String embeddingModel,
      String language,
      RedactionStatus redactionStatus,
      boolean current,
      UUID createdBy,
      Instant createdAt) {
  }

  private record DemoProcessingJob(
      UUID id,
      UUID documentVersionId,
      DocumentProcessingJobStatus status,
      DocumentProcessingStage stage,
      String language,
      String ocrProvider,
      String classificationProvider,
      BigDecimal overallConfidence,
      BigDecimal ocrConfidence,
      BigDecimal classificationConfidence,
      BigDecimal metadataConfidence,
      BigDecimal businessReferenceConfidence,
      BigDecimal validationConfidence,
      BigDecimal duplicateConfidence,
      int retryCount,
      String lastErrorCode,
      String lastErrorMessage,
      String reviewerComment,
      Instant startedAt,
      Instant reviewRequestedAt,
      Instant approvedAt,
      Instant rejectedAt,
      Instant publishedAt,
      Instant completedAt,
      Instant createdAt,
      Instant updatedAt,
      List<DemoProcessingField> fields,
      List<DemoProcessingFinding> findings) {
  }

  private record DemoProcessingField(
      UUID id,
      String fieldKey,
      String fieldLabel,
      DocumentProcessingFieldType fieldType,
      String businessReferenceType,
      String extractedValue,
      String correctedValue,
      String approvedValue,
      BigDecimal confidence,
      String sourceType,
      String extractionMethod,
      Integer sourcePage,
      boolean required,
      DocumentProcessingFieldValidationState validationState,
      Instant createdAt,
      Instant updatedAt) {
  }

  private record DemoProcessingFinding(
      UUID id,
      String findingCode,
      DocumentProcessingFindingSeverity severity,
      DocumentProcessingStage stage,
      String fieldKey,
      String message,
      String evidenceText,
      Integer sourcePage,
      BigDecimal confidence,
      DocumentProcessingFindingStatus status,
      String resolutionComment,
      Instant createdAt,
      Instant resolvedAt) {
  }

  private record DemoReviewItem(
      UUID id,
      ReviewQueueItemType itemType,
      String itemId,
      ReviewQueueReason reason,
      UUID assignedTo,
      ReviewQueueStatus status,
      Instant createdAt,
      Instant resolvedAt) {
  }

  private record DemoQualitySnapshot(
      UUID id,
      UUID clientId,
      BigDecimal overallScore,
      BigDecimal completenessScore,
      BigDecimal businessReferenceScore,
      BigDecimal linkageScore,
      BigDecimal duplicateScore,
      BigDecimal timelineScore,
      BigDecimal versionScore,
      BigDecimal retrievalReadinessScore,
      BigDecimal aiQualityScore,
      int issueCount,
      int openIssueCount,
      KnowledgeQualityReadinessState readinessState,
      String summaryText,
      Instant evaluatedAt,
      Instant updatedAt) {
  }

  private record DemoQualityIssue(
      UUID id,
      UUID snapshotId,
      UUID clientId,
      String sourceType,
      UUID sourceId,
      String category,
      String issueType,
      KnowledgeQualityIssueSeverity severity,
      KnowledgeQualityIssueStatus status,
      String title,
      String detail,
      String recommendationType,
      String recommendationDetail,
      String businessReferenceKey,
      BigDecimal scoreImpact,
      Instant createdAt,
      Instant updatedAt) {
  }

  private record DemoRetentionRecord(
      UUID id,
      String targetType,
      String targetId,
      UUID clientId,
      boolean legalHold,
      Instant minimumRetentionUntil,
      String holdType,
      String retentionPolicyKey,
      Instant reviewAt,
      Instant archivalEligibleAt,
      Instant disposalEligibleAt,
      String lastAction,
      String lastOutcome,
      String lastReason,
      Instant executedAt,
      Instant createdAt,
      Instant updatedAt) {
  }

  private record DemoQueueState(
      String queueKey,
      boolean paused,
      Instant pausedAt,
      Instant resumedAt,
      Instant updatedAt,
      UUID updatedBy) {
  }

  private record DemoOperationsJob(
      UUID id,
      String jobType,
      UUID submittedBy,
      Instant submittedAt,
      Instant startedAt,
      Instant completedAt,
      Long durationMs,
      String status,
      int progress,
      String errorSummary,
      int retryCount,
      String targetType,
      String targetId,
      String queueKey,
      boolean cancelRequested,
      int priority,
      String details,
      Instant createdAt,
      Instant updatedAt) {
  }

  private record DemoSchedulerExecution(
      UUID id,
      String schedulerKey,
      UUID triggeredBy,
      String triggerSource,
      String status,
      String details,
      Instant startedAt,
      Instant completedAt) {
  }

  private record DemoOperationsMetric(
      UUID id,
      String metricGroup,
      String metricKey,
      BigDecimal metricValue,
      String metricUnit,
      String dimensionsJson,
      Instant recordedAt) {
  }

  private record DemoAuditEvent(
      Instant occurredAt,
      String category,
      String action,
      AuditService.AuditOutcome outcome,
      UUID actorUserId,
      UUID clientId,
      String targetType,
      String targetId,
      boolean piiAccess,
      Map<String, String> details) {
  }

  private static final class DemoCatalog {
    private final String namespace;
    private final LocalDate referenceDate;
    private final List<DemoUser> users;
    private final List<DemoClient> clients;
    private final List<DemoNote> notes;
    private final List<DemoEmail> emails;
    private final List<DemoDocumentType> documentTypes;
    private final List<DemoMetadataField> metadataFields;
    private final List<DemoSharedFolder> sharedFolders;
    private final List<DemoMailbox> mailboxes;
    private final UUID reviewSettingId;
    private final UUID aiProviderSettingId;
    private final List<DemoDocument> documents;
    private final List<DemoReviewItem> reviewItems;
    private final List<DemoQualitySnapshot> qualitySnapshots;
    private final List<DemoQualityIssue> qualityIssues;
    private final List<DemoRetentionRecord> retentionRecords;
    private final List<DemoQueueState> queueStates;
    private final List<DemoOperationsJob> operationsJobs;
    private final List<DemoSchedulerExecution> schedulerExecutions;
    private final List<DemoOperationsMetric> operationsMetrics;
    private final List<DemoAuditEvent> auditEvents;

    private DemoCatalog(
        String namespace,
        LocalDate referenceDate,
        List<DemoUser> users,
        List<DemoClient> clients,
        List<DemoNote> notes,
        List<DemoEmail> emails,
        List<DemoDocumentType> documentTypes,
        List<DemoMetadataField> metadataFields,
        List<DemoSharedFolder> sharedFolders,
        List<DemoMailbox> mailboxes,
        UUID reviewSettingId,
        UUID aiProviderSettingId,
        List<DemoDocument> documents,
        List<DemoReviewItem> reviewItems,
        List<DemoQualitySnapshot> qualitySnapshots,
        List<DemoQualityIssue> qualityIssues,
        List<DemoRetentionRecord> retentionRecords,
        List<DemoQueueState> queueStates,
        List<DemoOperationsJob> operationsJobs,
        List<DemoSchedulerExecution> schedulerExecutions,
        List<DemoOperationsMetric> operationsMetrics,
        List<DemoAuditEvent> auditEvents) {
      this.namespace = namespace;
      this.referenceDate = referenceDate;
      this.users = users;
      this.clients = clients;
      this.notes = notes;
      this.emails = emails;
      this.documentTypes = documentTypes;
      this.metadataFields = metadataFields;
      this.sharedFolders = sharedFolders;
      this.mailboxes = mailboxes;
      this.reviewSettingId = reviewSettingId;
      this.aiProviderSettingId = aiProviderSettingId;
      this.documents = documents;
      this.reviewItems = reviewItems;
      this.qualitySnapshots = qualitySnapshots;
      this.qualityIssues = qualityIssues;
      this.retentionRecords = retentionRecords;
      this.queueStates = queueStates;
      this.operationsJobs = operationsJobs;
      this.schedulerExecutions = schedulerExecutions;
      this.operationsMetrics = operationsMetrics;
      this.auditEvents = auditEvents;
    }

    static DemoCatalog build(String namespace, LocalDate referenceDate) {
      CatalogBuilder builder = new CatalogBuilder(namespace, referenceDate);
      return builder.build();
    }

    String namespace() {
      return namespace;
    }

    UUID id(String key) {
      return UUID.nameUUIDFromBytes((namespace + "|" + key).getBytes(StandardCharsets.UTF_8));
    }

    Instant instant(String key) {
      long offsetMinutes = Math.abs((long) key.hashCode()) % (60L * 24L * 120L);
      return referenceDate.atStartOfDay().plusMinutes(offsetMinutes).toInstant(ZoneOffset.UTC);
    }

    List<DemoUser> users() { return users; }
    List<DemoClient> clients() { return clients; }
    List<DemoNote> notes() { return notes; }
    List<DemoEmail> emails() { return emails; }
    List<DemoDocumentType> documentTypes() { return documentTypes; }
    List<DemoMetadataField> metadataFields() { return metadataFields; }
    List<DemoSharedFolder> sharedFolders() { return sharedFolders; }
    List<DemoMailbox> mailboxes() { return mailboxes; }
    UUID reviewSettingId() { return reviewSettingId; }
    UUID aiProviderSettingId() { return aiProviderSettingId; }
    List<DemoDocument> documents() { return documents; }
    List<DemoReviewItem> reviewItems() { return reviewItems; }
    List<DemoQualitySnapshot> qualitySnapshots() { return qualitySnapshots; }
    List<DemoQualityIssue> qualityIssues() { return qualityIssues; }
    List<DemoRetentionRecord> retentionRecords() { return retentionRecords; }
    List<DemoQueueState> queueStates() { return queueStates; }
    List<DemoOperationsJob> operationsJobs() { return operationsJobs; }
    List<DemoSchedulerExecution> schedulerExecutions() { return schedulerExecutions; }
    List<DemoOperationsMetric> operationsMetrics() { return operationsMetrics; }
    List<DemoAuditEvent> auditEvents() { return auditEvents; }

    List<UUID> userIds() { return users.stream().map(DemoUser::id).toList(); }
    List<UUID> clientIds() { return clients.stream().map(DemoClient::id).toList(); }
    List<UUID> noteIds() { return notes.stream().map(DemoNote::id).toList(); }
    List<UUID> emailIds() { return emails.stream().map(DemoEmail::id).toList(); }
    List<UUID> documentTypeIds() { return documentTypes.stream().map(DemoDocumentType::id).toList(); }
    List<UUID> metadataFieldIds() { return metadataFields.stream().map(DemoMetadataField::id).toList(); }
    List<UUID> sharedFolderIds() { return sharedFolders.stream().map(DemoSharedFolder::id).toList(); }
    List<UUID> mailboxIds() { return mailboxes.stream().map(DemoMailbox::id).toList(); }
    List<UUID> aiProviderSettingIds() { return List.of(aiProviderSettingId); }
    List<UUID> reviewSettingIds() { return List.of(reviewSettingId); }
    List<UUID> documentIds() { return documents.stream().map(DemoDocument::id).toList(); }
    List<UUID> documentVersionIds() { return documents.stream().flatMap(document -> document.versions().stream()).map(DemoDocumentVersion::id).toList(); }
    List<UUID> processingJobIds() { return documents.stream().flatMap(document -> document.processingJobs().stream()).map(DemoProcessingJob::id).toList(); }
    List<UUID> processingFieldIds() { return documents.stream().flatMap(document -> document.processingJobs().stream()).flatMap(job -> job.fields().stream()).map(DemoProcessingField::id).toList(); }
    List<UUID> processingFindingIds() { return documents.stream().flatMap(document -> document.processingJobs().stream()).flatMap(job -> job.findings().stream()).map(DemoProcessingFinding::id).toList(); }
    List<UUID> reviewIds() { return reviewItems.stream().map(DemoReviewItem::id).toList(); }
    List<UUID> snapshotIds() { return qualitySnapshots.stream().map(DemoQualitySnapshot::id).toList(); }
    List<UUID> qualityIssueIds() { return qualityIssues.stream().map(DemoQualityIssue::id).toList(); }
    List<UUID> retentionIds() { return retentionRecords.stream().map(DemoRetentionRecord::id).toList(); }
    List<UUID> operationsJobIds() { return operationsJobs.stream().map(DemoOperationsJob::id).toList(); }
    List<UUID> schedulerExecutionIds() { return schedulerExecutions.stream().map(DemoSchedulerExecution::id).toList(); }
    List<UUID> operationsMetricIds() { return operationsMetrics.stream().map(DemoOperationsMetric::id).toList(); }
    Set<String> demoQueueKeys() { return queueStates.stream().map(DemoQueueState::queueKey).collect(LinkedHashSet::new, Set::add, Set::addAll); }
  }

  private static final class CatalogBuilder {
    private final String namespace;
    private final LocalDate referenceDate;
    private final DemoCatalog helper;

    private CatalogBuilder(String namespace, LocalDate referenceDate) {
      this.namespace = namespace;
      this.referenceDate = referenceDate;
      this.helper = new DemoCatalog(namespace, referenceDate, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
          UUID.nameUUIDFromBytes((namespace + "|review-setting").getBytes(StandardCharsets.UTF_8)),
          UUID.nameUUIDFromBytes((namespace + "|ai-provider-setting").getBytes(StandardCharsets.UTF_8)),
          List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private UUID id(String key) {
      return helper.id(key);
    }

    private Instant instant(String key) {
      return helper.instant(key);
    }

    DemoCatalog build() {
      List<DemoUser> users = buildUsers();
      List<DemoClient> clients = buildClients();
      List<DemoDocumentType> documentTypes = buildDocumentTypes();
      List<DemoMetadataField> metadataFields = buildMetadataFields();
      List<DemoSharedFolder> sharedFolders = List.of(
          new DemoSharedFolder(id("shared-folder-intake"), "/private/tmp/ikms-demo/intake/shared", true),
          new DemoSharedFolder(id("shared-folder-archive"), "/private/tmp/ikms-demo/intake/archive", false));
      List<DemoMailbox> mailboxes = List.of(
          new DemoMailbox(id("mailbox-intake"), "Demo Intake Mailbox", "mail.demo.local", "intake@example.test", true),
          new DemoMailbox(id("mailbox-review"), "Demo Review Mailbox", "mail.demo.local", "review@example.test", true),
          new DemoMailbox(id("mailbox-archive"), "Demo Archive Mailbox", "mail.demo.local", "archive@example.test", false));
      List<DemoNote> notes = buildNotes(clients, users);
      List<DemoEmail> emails = buildEmails(clients, mailboxes);
      List<DemoDocument> documents = buildDocuments(clients, users, documentTypes, emails);
      List<DemoReviewItem> reviewItems = buildReviewItems(users, emails, documents);
      List<DemoQualitySnapshot> qualitySnapshots = buildQualitySnapshots(clients);
      List<DemoQualityIssue> qualityIssues = buildQualityIssues(qualitySnapshots, documents, emails, clients);
      List<DemoRetentionRecord> retentionRecords = buildRetentionRecords(clients, documents);
      List<DemoQueueState> queueStates = buildQueueStates(users);
      List<DemoOperationsJob> operationsJobs = buildOperationsJobs(users, documents);
      List<DemoSchedulerExecution> schedulerExecutions = buildSchedulerExecutions(users);
      List<DemoOperationsMetric> operationsMetrics = buildOperationsMetrics();
      List<DemoAuditEvent> auditEvents = buildAuditEvents(users, clients, documents, reviewItems, qualityIssues);
      return new DemoCatalog(
          namespace,
          referenceDate,
          users,
          clients,
          notes,
          emails,
          documentTypes,
          metadataFields,
          sharedFolders,
          mailboxes,
          id("review-setting"),
          id("ai-provider-setting"),
          documents,
          reviewItems,
          qualitySnapshots,
          qualityIssues,
          retentionRecords,
          queueStates,
          operationsJobs,
          schedulerExecutions,
          operationsMetrics,
          auditEvents);
    }

    private List<DemoUser> buildUsers() {
      return List.of(
          new DemoUser(id("user-demo-admin"), "demo-admin", "Daniel Ortega", "daniel.ortega@example.test", UserStatus.ACTIVE, Set.of(UserRole.ADMINISTRATOR), "Administrator", InformationClassification.RESTRICTED),
          new DemoUser(id("user-demo-supervisor"), "demo-supervisor", "Sloane Reyes", "sloane.reyes@example.test", UserStatus.ACTIVE, Set.of(UserRole.SUPERVISOR), "Supervisor", InformationClassification.RESTRICTED),
          new DemoUser(id("user-demo-processor"), "demo-processor", "Priya Nair", "priya.nair@example.test", UserStatus.ACTIVE, Set.of(UserRole.PROCESSOR), "Reviewer", InformationClassification.INTERNAL),
          new DemoUser(id("user-demo-indexer"), "demo-indexer", "Marcus Chen", "marcus.chen@example.test", UserStatus.ACTIVE, Set.of(UserRole.INDEXER), "Restricted Operator", InformationClassification.INTERNAL),
          new DemoUser(id("user-demo-locked"), "demo-locked", "Helen Ward", "helen.ward@example.test", UserStatus.LOCKED, Set.of(UserRole.PROCESSOR), "Locked Reviewer", InformationClassification.INTERNAL),
          new DemoUser(id("user-demo-disabled"), "demo-disabled", "Avery Stone", "avery.stone@example.test", UserStatus.DISABLED, Set.of(UserRole.PROCESSOR), "Inactive Reviewer", InformationClassification.INTERNAL));
    }

    private List<DemoClient> buildClients() {
      List<String> names = List.of(
          "Harborview Marine Logistics LLC",
          "Silver Ridge Property Management Holdings and Tenant Stewardship Consortium",
          "North Valley Dental Group PC",
          "Eleanor Briggs",
          "Cedar Peak Manufacturing Ltd",
          "Greenstone Hospitality Group",
          "Westbridge Consulting Partners",
          "Meridian Retail Holdings",
          "Lakeshore Community Services",
          "Apex Industrial Engineering",
          "Bluehaven Technology Services",
          "Oakfield Medical Practice",
          "Brighton Legal Advisory",
          "Summit Transport Solutions",
          "Redgum Construction Group",
          "Maple Grove Education Trust",
          "Clearview Financial Planning",
          "Riverside Food Distribution");
      List<DemoClient> clients = new ArrayList<>();
      for (int index = 0; index < names.size(); index++) {
        String slug = slug(names.get(index));
        boolean business = index != 3;
        ClientStatus status = index == 7 || index == 14 ? ClientStatus.INACTIVE : (index == 16 ? ClientStatus.ARCHIVED : ClientStatus.ACTIVE);
        clients.add(new DemoClient(
            id("client-" + slug),
            business ? "CL-" + (1000 + index) : "TMP-IND-" + (100 + index),
            !business,
            business ? ClientType.BUSINESS : ClientType.INDIVIDUAL,
            status,
            names.get(index),
            names.get(index),
            slug + "@example.test",
            "ops+" + slug + "@example.test",
            "+1-202-555-" + String.format(Locale.ROOT, "%04d", 1000 + index),
            "+1-202-555-" + String.format(Locale.ROOT, "%04d", 2000 + index),
            business ? "Contact " + (index + 1) : names.get(index),
            (200 + index) + " Synthetic Plaza",
            index % 2 == 0 ? "Suite " + (10 + index) : null,
            List.of("Seattle", "Denver", "Phoenix", "Austin", "Chicago", "Boston").get(index % 6),
            List.of("Washington", "Colorado", "Arizona", "Texas", "Illinois", "Massachusetts").get(index % 6),
            "98" + String.format(Locale.ROOT, "%03d", index),
            "USA",
            instant("client-created-" + index),
            instant("client-updated-" + index)));
      }
      return clients;
    }

    private List<DemoDocumentType> buildDocumentTypes() {
      return List.of(
          new DemoDocumentType(id("document-type-acord"), "ACORD Form", "Synthetic ACORD-form intake documents.", true),
          new DemoDocumentType(id("document-type-submission-email"), "Submission Email", "Synthetic renewal and placement correspondence.", true),
          new DemoDocumentType(id("document-type-sov"), "Statement of Values", "Synthetic location schedules and values.", true),
          new DemoDocumentType(id("document-type-renewal-schedule"), "Renewal Schedule", "Synthetic renewal pack schedules.", true),
          new DemoDocumentType(id("document-type-claims"), "Claims Correspondence", "Synthetic claim reference support.", true),
          new DemoDocumentType(id("document-type-risk"), "Risk Survey", "Synthetic risk engineering reports.", true),
          new DemoDocumentType(id("document-type-certificate"), "Certificate", "Synthetic proof of insurance references.", true),
          new DemoDocumentType(id("document-type-general"), "General Attachment", "Synthetic uploaded attachments.", true));
    }

    private List<DemoMetadataField> buildMetadataFields() {
      return List.of(
          new DemoMetadataField(id("metadata-policy-number"), "policy_number", "Policy Number", false, true),
          new DemoMetadataField(id("metadata-claim-number"), "claim_number", "Claim Number", false, true),
          new DemoMetadataField(id("metadata-broker-reference"), "broker_reference", "Broker Reference", false, true),
          new DemoMetadataField(id("metadata-insurer"), "insurer", "Insurer", false, true),
          new DemoMetadataField(id("metadata-effective-date"), "effective_date", "Effective Date", false, true),
          new DemoMetadataField(id("metadata-expiry-date"), "expiry_date", "Expiry Date", false, true),
          new DemoMetadataField(id("metadata-renewal-date"), "renewal_date", "Renewal Date", false, true),
          new DemoMetadataField(id("metadata-customer-reference"), "customer_reference", "Customer Reference", false, true),
          new DemoMetadataField(id("metadata-classification"), "classification", "Classification", false, true),
          new DemoMetadataField(id("metadata-confidentiality"), "confidentiality", "Confidentiality", false, true),
          new DemoMetadataField(id("metadata-location"), "location", "Location", false, true),
          new DemoMetadataField(id("metadata-insured-email"), "insured_email", "Insured Email", true, true),
          new DemoMetadataField(id("metadata-risk-summary"), "risk_summary", "Risk Summary", false, true));
    }

    private List<DemoNote> buildNotes(List<DemoClient> clients, List<DemoUser> users) {
      List<DemoNote> notes = new ArrayList<>();
      UUID author = users.get(2).id();
      for (int index = 0; index < 25; index++) {
        DemoClient client = clients.get(index % clients.size());
        notes.add(new DemoNote(
            id("note-" + index),
            client.id(),
            "Synthetic note " + (index + 1) + " for " + client.displayName() + ". Renewal preparation, evidence routing, and manual stewardship context for " + namespace + ".",
            index == 24 ? NoteStatus.DELETED : NoteStatus.ACTIVE,
            author,
            author,
            instant("note-created-" + index),
            instant("note-updated-" + index)));
      }
      return notes;
    }

    private List<DemoEmail> buildEmails(List<DemoClient> clients, List<DemoMailbox> mailboxes) {
      List<DemoEmail> emails = new ArrayList<>();
      for (int index = 0; index < 25; index++) {
        DemoClient client = clients.get(index % clients.size());
        emails.add(new DemoEmail(
            id("email-" + index),
            index == 23 ? null : client.id(),
            mailboxes.get(index % mailboxes.size()).id(),
            namespace + "-message-" + index,
            namespace + "-thread-" + (index / 2),
            switch (index % 5) {
              case 0 -> "Policy Renewal Email";
              case 1 -> "Silver Ridge schedule with unresolved location values";
              case 2 -> "Customer Correspondence";
              case 3 -> "Open Claim Exposure Note";
              default -> "Submission Checklist";
            } + " " + (index + 1),
            "sender" + index + "@example.test",
            "servicing+" + slug(client.displayName()) + "@example.test",
            index % 4 == 0 ? "underwriting@example.test" : null,
            instant("email-received-" + index),
            client.displayName() + " synthetic email body for " + namespace + " covering marine, acord, renewal, policy number TMP-PROP-90814, claim CLM-SEA-240118, and business reference BRK-REF-2026-00142.",
            null,
            index % 7 == 0 ? EmailProcessingStatus.FAILED : (index % 3 == 0 ? EmailProcessingStatus.PENDING_REVIEW : EmailProcessingStatus.LINKED),
            index % 7 == 0 ? EmailReviewStatus.PENDING_REVIEW : (index % 6 == 0 ? EmailReviewStatus.REJECTED : EmailReviewStatus.APPROVED),
            instant("email-created-" + index)));
      }
      return emails;
    }

    private List<DemoDocument> buildDocuments(
        List<DemoClient> clients,
        List<DemoUser> users,
        List<DemoDocumentType> documentTypes,
        List<DemoEmail> emails) {
      List<String> titles = List.of(
          "ACORD 125 Property Schedule",
          "2026 Marine Cargo Renewal Submission",
          "Statement of Values",
          "Updated Statement of Values with Tenant Improvement Notes",
          "Cyber Liability Renewal Questionnaire",
          "Professional Indemnity Proposal Form",
          "Directors and Officers Liability Schedule",
          "Commercial Property Valuation",
          "Fleet Vehicle Schedule",
          "Business Interruption Worksheet",
          "Claims Experience Summary",
          "Broker Cover Note",
          "Renewal Recommendation",
          "Risk Survey Report",
          "Certificate of Currency",
          "Customer Correspondence",
          "Property Schedule Investigation Summary",
          "Open Claim Exposure Note",
          "Customer Address Confirmation",
          "Policy Renewal Email",
          "Submission Checklist",
          "Compliance Declaration",
          "Board Director Details",
          "Prior Insurance History",
          "Risk Improvement Recommendation");

      List<DemoDocument> documents = new ArrayList<>();
      for (int index = 0; index < 80; index++) {
        DemoClient client = clients.get(index % clients.size());
        String baseSlug = slug(client.displayName()) + "-" + index;
        UUID documentId = id("document-" + baseSlug);
        UUID documentTypeId = documentTypes.get(index % documentTypes.size()).id();
        UUID parentEmailId = index % 4 == 0 ? emails.get(index % emails.size()).id() : null;
        String policyNumber = index % 3 == 0 ? "TMP-PROP-90814" : "MWI-GL-" + (114920 + index);
        String claimNumber = index % 9 == 0 ? "CLM-SEA-240118" : "";
        String insurer = List.of("Atlas Mutual", "Northshore Underwriters", "Harbor Syndicate", "Summit Indemnity").get(index % 4);
        String location = client.displayName().contains("Silver Ridge") && index % 5 == 0 ? "" : client.city() + " campus";
        List<DemoDocumentVersion> versions = buildDocumentVersions(baseSlug, users, index, titles.get(index % titles.size()));
        List<DemoProcessingJob> jobs = buildProcessingJobs(baseSlug, versions, index, policyNumber, claimNumber, insurer, location);
        documents.add(new DemoDocument(
            documentId,
            client.id(),
            parentEmailId,
            documentTypeId,
            titles.get(index % titles.size()) + (index >= titles.size() ? " " + (index + 1) : ""),
            index % 11 == 0 ? DocumentProcessingStatus.FAILED : (index % 4 == 0 ? DocumentProcessingStatus.WAITING_REVIEW : DocumentProcessingStatus.INDEXED),
            index % 11 == 0 ? DocumentReviewStatus.PENDING_REVIEW : (index % 8 == 0 ? DocumentReviewStatus.REJECTED : DocumentReviewStatus.APPROVED),
            score(0.58 + (index % 5) * 0.08),
            score(0.62 + (index % 4) * 0.09),
            score(0.51 + (index % 6) * 0.07),
            index % 3 == 0 ? DocumentSource.EMAIL_ATTACHMENT : (index % 3 == 1 ? DocumentSource.MANUAL_UPLOAD : DocumentSource.SHARED_FOLDER),
            index % 10 == 0 ? InformationClassification.RESTRICTED : (index % 6 == 0 ? InformationClassification.CONFIDENTIAL : InformationClassification.INTERNAL),
            index % 10 == 0 ? SensitivityLevel.CRITICAL : (index % 5 == 0 ? SensitivityLevel.HIGH : SensitivityLevel.MODERATE),
            index % 10 == 0 ? "restricted" : "internal",
            index % 13 == 0 ? DocumentLifecycleState.ARCHIVED : DocumentLifecycleState.ACTIVE,
            index % 10 == 0,
            index % 7 == 0,
            index % 2 == 0 ? "Client Service" : "Claims Support",
            client.region(),
            "Demo Office " + ((index % 3) + 1),
            instant("document-created-" + index),
            instant("document-updated-" + index),
            Map.ofEntries(
                Map.entry("policy_number", policyNumber),
                Map.entry("claim_number", claimNumber.isBlank() ? "N/A" : claimNumber),
                Map.entry("broker_reference", index == 1 ? "BRK-REF-2026-00142" : "BRK-REF-2026-" + String.format(Locale.ROOT, "%05d", 100 + index)),
                Map.entry("insurer", insurer),
                Map.entry("effective_date", "2026-01-" + String.format(Locale.ROOT, "%02d", (index % 20) + 1)),
                Map.entry("expiry_date", "2027-01-" + String.format(Locale.ROOT, "%02d", (index % 20) + 1)),
                Map.entry("renewal_date", "2026-12-" + String.format(Locale.ROOT, "%02d", (index % 20) + 1)),
                Map.entry("customer_reference", client.externalId()),
                Map.entry("classification", index % 10 == 0 ? "RESTRICTED" : "INTERNAL"),
                Map.entry("confidentiality", index % 10 == 0 ? "restricted" : "internal"),
                Map.entry("location", location.isBlank() ? "Pending review" : location),
                Map.entry("insured_email", slug(client.displayName()) + "@example.test"),
                Map.entry("risk_summary", client.displayName() + " synthetic evidence summary for marine, acord, and renewal verification.")),
            versions,
            jobs));
      }
      return documents;
    }

    private List<DemoDocumentVersion> buildDocumentVersions(String baseSlug, List<DemoUser> users, int index, String title) {
      int versionCount = index % 10 == 0 ? 3 : (index % 4 == 0 ? 2 : 1);
      List<DemoDocumentVersion> versions = new ArrayList<>();
      for (int versionNumber = 1; versionNumber <= versionCount; versionNumber++) {
        boolean current = versionNumber == versionCount;
        versions.add(new DemoDocumentVersion(
            id("document-version-" + baseSlug + "-" + versionNumber),
            versionNumber,
            namespace + "-hash-" + baseSlug + "-v" + versionNumber,
            slug(title) + "-v" + versionNumber + (index % 9 == 0 ? ".tiff" : ".pdf"),
            index % 9 == 0 ? "image/tiff" : "application/pdf",
            120_000L + (versionNumber * 2048L),
            "seed/demo/" + baseSlug + "/v" + versionNumber + "/original",
            versionNumber == 1 && index % 10 == 0 ? null : "seed/demo/" + baseSlug + "/v" + versionNumber + "/redacted",
            title + " synthetic extracted text for " + namespace + ". Marine renewal, silverridge, acord, TMP-PROP-90814, CLM-SEA-240118, and " + baseSlug + ".",
            versionNumber == versionCount && index % 5 == 0 ? "synthetic-ocr" : "manual",
            "ikms-synthetic-embed",
            "en",
            versionNumber == 1 && index % 10 == 0 ? RedactionStatus.BLOCKED : (index % 9 == 0 ? RedactionStatus.FAILED : RedactionStatus.AVAILABLE),
            current,
            users.get(versionNumber % users.size()).id(),
            instant("document-version-" + baseSlug + "-" + versionNumber)));
      }
      return versions;
    }

    private List<DemoProcessingJob> buildProcessingJobs(
        String baseSlug,
        List<DemoDocumentVersion> versions,
        int index,
        String policyNumber,
        String claimNumber,
        String insurer,
        String location) {
      DemoDocumentVersion currentVersion = versions.get(versions.size() - 1);
      UUID jobId = id("processing-job-" + baseSlug);
      Instant createdAt = instant("processing-job-created-" + baseSlug);
      List<DemoProcessingField> fields = List.of(
          new DemoProcessingField(id("processing-field-" + baseSlug + "-title"), "title", "Title", DocumentProcessingFieldType.DOCUMENT_METADATA, null,
              "Synthetic " + baseSlug, null, "Synthetic " + baseSlug, score(0.96), "OCR", "OCR", 1, true, DocumentProcessingFieldValidationState.APPROVED, createdAt, createdAt),
          new DemoProcessingField(id("processing-field-" + baseSlug + "-policy"), "policy_number", "Policy Number", DocumentProcessingFieldType.BUSINESS_REFERENCE, "POLICY_NUMBER",
              policyNumber, index % 6 == 0 ? policyNumber + "-ALT" : null, policyNumber, score(index % 4 == 0 ? 0.54 : 0.91), "OCR", "OCR", 1, true,
              index % 6 == 0 ? DocumentProcessingFieldValidationState.CORRECTED : DocumentProcessingFieldValidationState.APPROVED, createdAt, createdAt),
          new DemoProcessingField(id("processing-field-" + baseSlug + "-claim"), "claim_number", "Claim Number", DocumentProcessingFieldType.BUSINESS_REFERENCE, "CLAIM_NUMBER",
              claimNumber.isBlank() ? null : claimNumber, null, claimNumber.isBlank() ? null : claimNumber, score(claimNumber.isBlank() ? 0.42 : 0.88), "OCR", "OCR", 2, false,
              claimNumber.isBlank() ? DocumentProcessingFieldValidationState.MISSING : DocumentProcessingFieldValidationState.APPROVED, createdAt, createdAt),
          new DemoProcessingField(id("processing-field-" + baseSlug + "-insurer"), "insurer", "Insurer", DocumentProcessingFieldType.BUSINESS_REFERENCE, "INSURER",
              insurer, null, insurer, score(0.84), "OCR", "OCR", 1, true, DocumentProcessingFieldValidationState.APPROVED, createdAt, createdAt),
          new DemoProcessingField(id("processing-field-" + baseSlug + "-location"), "location", "Location", DocumentProcessingFieldType.DOCUMENT_METADATA, null,
              location.isBlank() ? null : location, null, location.isBlank() ? null : location, score(location.isBlank() ? 0.35 : 0.79), "OCR", "OCR", 3, index % 5 == 0,
              location.isBlank() ? DocumentProcessingFieldValidationState.MISSING : DocumentProcessingFieldValidationState.APPROVED, createdAt, createdAt));
      List<DemoProcessingFinding> findings = new ArrayList<>();
      if (location.isBlank()) {
        findings.add(new DemoProcessingFinding(
            id("processing-finding-" + baseSlug + "-location"),
            "MISSING_LOCATION",
            DocumentProcessingFindingSeverity.WARNING,
            DocumentProcessingStage.VALIDATION,
            "location",
            "Location value is missing and requires manual review.",
            "No validated location found in the current synthetic version.",
            3,
            score(0.38),
            DocumentProcessingFindingStatus.OPEN,
            null,
            createdAt,
            null));
      }
      if (index % 7 == 0) {
        findings.add(new DemoProcessingFinding(
            id("processing-finding-" + baseSlug + "-confidence"),
            "LOW_EXTRACTION_CONFIDENCE",
            DocumentProcessingFindingSeverity.ERROR,
            DocumentProcessingStage.CONFIDENCE_CALCULATION,
            "policy_number",
            "Policy number confidence is below the review threshold.",
            "Synthetic OCR output produced ambiguous policy digits.",
            1,
            score(0.49),
            DocumentProcessingFindingStatus.OPEN,
            null,
            createdAt,
            null));
      }
      return List.of(new DemoProcessingJob(
          jobId,
          currentVersion.id(),
          index % 11 == 0 ? DocumentProcessingJobStatus.FAILED : (index % 4 == 0 ? DocumentProcessingJobStatus.WAITING_REVIEW : DocumentProcessingJobStatus.COMPLETED),
          index % 11 == 0 ? DocumentProcessingStage.FAILED : (index % 4 == 0 ? DocumentProcessingStage.REVIEW_QUEUE : DocumentProcessingStage.RETRIEVAL_READY),
          "en",
          "synthetic-ocr",
          "synthetic-classifier",
          score(index % 5 == 0 ? 0.61 : 0.93),
          score(index % 7 == 0 ? 0.55 : 0.92),
          score(0.87),
          score(location.isBlank() ? 0.53 : 0.88),
          score(claimNumber.isBlank() ? 0.56 : 0.89),
          score(0.79),
          score(0.81),
          index % 11 == 0 ? 1 : 0,
          index % 11 == 0 ? "OCR_TIMEOUT" : null,
          index % 11 == 0 ? "Synthetic OCR timeout for unsupported preview sample." : null,
          index % 4 == 0 ? "Requires metadata correction before publication." : "Published from synthetic demo seed.",
          createdAt,
          createdAt.plusSeconds(1800),
          index % 4 == 0 ? null : createdAt.plusSeconds(7200),
          index % 8 == 0 ? createdAt.plusSeconds(3600) : null,
          index % 4 == 0 ? null : createdAt.plusSeconds(7500),
          index % 4 == 0 ? null : createdAt.plusSeconds(7800),
          createdAt,
          createdAt.plusSeconds(7800),
          fields,
          findings));
    }

    private List<DemoReviewItem> buildReviewItems(List<DemoUser> users, List<DemoEmail> emails, List<DemoDocument> documents) {
      List<ReviewQueueReason> reasons = List.of(
          ReviewQueueReason.UNLINKED,
          ReviewQueueReason.LOW_CLIENT_CONFIDENCE,
          ReviewQueueReason.LOW_CLASSIFICATION_CONFIDENCE,
          ReviewQueueReason.LOW_EXTRACTION_CONFIDENCE,
          ReviewQueueReason.DUPLICATE_UNCERTAINTY,
          ReviewQueueReason.REDACTION_FAILED,
          ReviewQueueReason.PROMPT_INJECTION_RISK,
          ReviewQueueReason.PROCESSING_FAILED);
      List<DemoReviewItem> items = new ArrayList<>();
      for (int index = 0; index < 20; index++) {
        boolean emailItem = index % 5 == 0;
        String itemId = emailItem ? emails.get(index).id().toString() : documents.get(index).id().toString();
        items.add(new DemoReviewItem(
            id("review-item-" + index),
            emailItem ? ReviewQueueItemType.EMAIL : ReviewQueueItemType.DOCUMENT,
            itemId,
            reasons.get(index % reasons.size()),
            index % 3 == 0 ? users.get(1).id() : null,
            index % 8 == 0 ? ReviewQueueStatus.REJECTED : (index % 6 == 0 ? ReviewQueueStatus.RESOLVED : (index % 4 == 0 ? ReviewQueueStatus.IN_PROGRESS : ReviewQueueStatus.OPEN)),
            instant("review-created-" + index),
            index % 6 == 0 || index % 8 == 0 ? instant("review-resolved-" + index) : null));
      }
      return items;
    }

    private List<DemoQualitySnapshot> buildQualitySnapshots(List<DemoClient> clients) {
      List<DemoQualitySnapshot> snapshots = new ArrayList<>();
      for (int index = 0; index < clients.size(); index++) {
        DemoClient client = clients.get(index);
        int issueCount = index < 6 ? 2 : (index < 12 ? 1 : 0);
        int openIssueCount = index < 5 ? issueCount : Math.max(issueCount - 1, 0);
        KnowledgeQualityReadinessState readiness = openIssueCount >= 2 ? KnowledgeQualityReadinessState.BLOCKED : (openIssueCount == 1 ? KnowledgeQualityReadinessState.NEEDS_ATTENTION : KnowledgeQualityReadinessState.READY);
        snapshots.add(new DemoQualitySnapshot(
            id("quality-snapshot-" + slug(client.displayName())),
            client.id(),
            score(0.56 + (index % 6) * 0.06),
            score(0.62 + (index % 5) * 0.05),
            score(0.57 + (index % 5) * 0.06),
            score(0.60 + (index % 4) * 0.06),
            score(0.64 + (index % 4) * 0.05),
            score(0.61 + (index % 5) * 0.05),
            score(0.65 + (index % 4) * 0.05),
            score(0.63 + (index % 3) * 0.07),
            score(0.58 + (index % 3) * 0.08),
            issueCount,
            openIssueCount,
            readiness,
            client.displayName() + " synthetic knowledge-quality summary for " + namespace + ".",
            instant("quality-evaluated-" + index),
            instant("quality-updated-" + index)));
      }
      return snapshots;
    }

    private List<DemoQualityIssue> buildQualityIssues(
        List<DemoQualitySnapshot> snapshots,
        List<DemoDocument> documents,
        List<DemoEmail> emails,
        List<DemoClient> clients) {
      List<DemoQualityIssue> issues = new ArrayList<>();
      DemoClient harborview = clients.get(0);
      DemoClient silverRidge = clients.get(1);
      DemoClient northValley = clients.get(2);

      issues.add(new DemoQualityIssue(id("quality-issue-harborview-policy"), snapshots.get(0).id(), harborview.id(), "DOCUMENT", documents.get(1).id(),
          "BUSINESS_REFERENCE", "CONFLICTING_POLICY_REFERENCE", KnowledgeQualityIssueSeverity.HIGH, KnowledgeQualityIssueStatus.OPEN,
          "Conflicting policy reference across renewal pack",
          "Policy number values disagree between renewal submission and cover note for Harborview Marine Logistics LLC. Synthetic evidence references TMP-PROP-90814 and MWI-GL-114920.",
          "REVALIDATE", "Confirm the approved policy number and reindex the renewal pack.", "policy_number", score(0.23), instant("quality-issue-1"), instant("quality-issue-1")));
      issues.add(new DemoQualityIssue(id("quality-issue-harborview-email"), snapshots.get(0).id(), harborview.id(), "EMAIL", emails.get(0).id(),
          "CLASSIFICATION", "LOW_CLASSIFICATION_CONFIDENCE", KnowledgeQualityIssueSeverity.MEDIUM, KnowledgeQualityIssueStatus.OPEN,
          "Email classification confidence below stewardship threshold",
          "Synthetic email intake for Harborview Marine Logistics LLC fell below the stewardship threshold and still requires reviewer confirmation.",
          "METADATA_CORRECTION", "Confirm the customer and classification, then publish.", null, score(0.15), instant("quality-issue-2"), instant("quality-issue-2")));
      issues.add(new DemoQualityIssue(id("quality-issue-silver-location"), snapshots.get(1).id(), silverRidge.id(), "DOCUMENT", documents.get(3).id(),
          "METADATA_COMPLETENESS", "MISSING_METADATA", KnowledgeQualityIssueSeverity.HIGH, KnowledgeQualityIssueStatus.OPEN,
          "Silver Ridge schedule missing location data",
          "The ACORD and Statement of Values records disagree on one location and the latest schedule still has a missing location field.",
          "METADATA_CORRECTION", "Add the missing location value before approving the schedule.", "location", score(0.21), instant("quality-issue-3"), instant("quality-issue-3")));
      issues.add(new DemoQualityIssue(id("quality-issue-silver-link"), snapshots.get(1).id(), silverRidge.id(), "EMAIL", emails.get(1).id(),
          "LINKAGE", "MISSING_CUSTOMER_LINK", KnowledgeQualityIssueSeverity.MEDIUM, KnowledgeQualityIssueStatus.OPEN,
          "Silver Ridge intake email still needs customer confirmation",
          "The supporting synthetic email remained ambiguous after initial classification and needs a customer-link decision.",
          "CUSTOMER_REASSIGNMENT", "Link the email and republish related evidence.", null, score(0.12), instant("quality-issue-4"), instant("quality-issue-4")));
      issues.add(new DemoQualityIssue(id("quality-issue-north-summary"), snapshots.get(2).id(), northValley.id(), "DOCUMENT", documents.get(2).id(),
          "SUMMARY", "STALE_SUMMARY", KnowledgeQualityIssueSeverity.LOW, KnowledgeQualityIssueStatus.RESOLVED,
          "Customer summary freshness lag resolved",
          "North Valley Dental Group PC had a stale synthetic summary that was refreshed during the last quality pass.",
          "REVALIDATE", "No further action required.", null, score(0.05), instant("quality-issue-5"), instant("quality-resolved-5")));

      for (int index = 5; index < 15; index++) {
        DemoClient client = clients.get(index);
        DemoQualitySnapshot snapshot = snapshots.get(index);
        issues.add(new DemoQualityIssue(
            id("quality-issue-" + index),
            snapshot.id(),
            client.id(),
            index % 2 == 0 ? "DOCUMENT" : "EMAIL",
            index % 2 == 0 ? documents.get(index).id() : emails.get(index).id(),
            index % 2 == 0 ? "RETRIEVAL" : "TIMELINE",
            index % 3 == 0 ? "INCOMPLETE_EXTRACTION" : "STALE_METADATA",
            index % 4 == 0 ? KnowledgeQualityIssueSeverity.CRITICAL : (index % 3 == 0 ? KnowledgeQualityIssueSeverity.HIGH : KnowledgeQualityIssueSeverity.MEDIUM),
            index % 5 == 0 ? KnowledgeQualityIssueStatus.RESOLVED : KnowledgeQualityIssueStatus.OPEN,
            client.displayName() + " synthetic quality issue " + index,
            "Synthetic issue detail for " + client.displayName() + " covering " + namespace + ".",
            "REINDEX",
            "Reindex the customer knowledge after steward review.",
            index % 2 == 0 ? "policy_number" : null,
            score(0.08 + (index * 0.01)),
            instant("quality-issue-extra-" + index),
            instant("quality-issue-extra-" + index)));
      }
      return issues;
    }

    private List<DemoRetentionRecord> buildRetentionRecords(List<DemoClient> clients, List<DemoDocument> documents) {
      List<DemoRetentionRecord> records = new ArrayList<>();
      for (int index = 0; index < 8; index++) {
        DemoClient client = clients.get(index);
        DemoDocument document = documents.get(index);
        records.add(new DemoRetentionRecord(
            id("retention-" + index),
            "DOCUMENT",
            document.id().toString(),
            client.id(),
            index == 0,
            instant("retention-min-" + index).plusSeconds(86400L * 365),
            index == 0 ? HoldType.LITIGATION.name() : HoldType.NONE.name(),
            index % 2 == 0 ? "CUSTOMER_DOCUMENT" : "EMAIL",
            instant("retention-review-" + index),
            instant("retention-archive-" + index),
            instant("retention-disposal-" + index),
            index == 0 ? "LEGAL_HOLD_APPLIED" : "REVIEWED",
            "SUCCESS",
            "Synthetic retention record for " + namespace + ".",
            instant("retention-executed-" + index),
            instant("retention-created-" + index),
            instant("retention-updated-" + index)));
      }
      return records;
    }

    private List<DemoQueueState> buildQueueStates(List<DemoUser> users) {
      UUID updatedBy = users.get(0).id();
      return List.of(
          new DemoQueueState("REINDEX", false, null, instant("queue-resumed-reindex"), instant("queue-updated-reindex"), updatedBy),
          new DemoQueueState("EMBEDDING", false, null, instant("queue-resumed-embedding"), instant("queue-updated-embedding"), updatedBy),
          new DemoQueueState("REVIEW", false, null, instant("queue-resumed-review"), instant("queue-updated-review"), updatedBy),
          new DemoQueueState("DOCUMENT_PROCESSING", false, null, instant("queue-resumed-document"), instant("queue-updated-document"), updatedBy),
          new DemoQueueState("OCR", true, instant("queue-paused-ocr"), null, instant("queue-updated-ocr"), updatedBy),
          new DemoQueueState("AI", false, null, instant("queue-resumed-ai"), instant("queue-updated-ai"), updatedBy),
          new DemoQueueState("PUBLISHING", false, null, instant("queue-resumed-publishing"), instant("queue-updated-publishing"), updatedBy));
    }

    private List<DemoOperationsJob> buildOperationsJobs(List<DemoUser> users, List<DemoDocument> documents) {
      List<DemoOperationsJob> jobs = new ArrayList<>();
      for (int index = 0; index < 12; index++) {
        jobs.add(new DemoOperationsJob(
            id("operations-job-" + index),
            index % 3 == 0 ? "CUSTOMER_REINDEX" : (index % 3 == 1 ? "OCR_RETRY" : "EMBEDDING_REGENERATE"),
            users.get(index % users.size()).id(),
            instant("operations-submitted-" + index),
            instant("operations-started-" + index),
            index % 4 == 0 ? null : instant("operations-completed-" + index),
            index % 4 == 0 ? null : 4800L + index * 120L,
            index % 5 == 0 ? "FAILED" : (index % 4 == 0 ? "RUNNING" : "COMPLETED"),
            index % 4 == 0 ? 55 : 100,
            index % 5 == 0 ? "Synthetic processing exception for " + namespace : null,
            index % 5 == 0 ? 1 : 0,
            "DOCUMENT",
            documents.get(index).id().toString(),
            index % 3 == 0 ? "REINDEX" : (index % 3 == 1 ? "OCR" : "EMBEDDING"),
            false,
            100 - index,
            "{\"namespace\":\"" + namespace + "\",\"jobIndex\":\"" + index + "\"}",
            instant("operations-created-" + index),
            instant("operations-updated-" + index)));
      }
      return jobs;
    }

    private List<DemoSchedulerExecution> buildSchedulerExecutions(List<DemoUser> users) {
      List<String> schedulerKeys = List.of("nightly-reindex", "retention-evaluation", "embedding-refresh", "quality-recalculation");
      List<DemoSchedulerExecution> executions = new ArrayList<>();
      for (int index = 0; index < schedulerKeys.size(); index++) {
        executions.add(new DemoSchedulerExecution(
            id("scheduler-execution-" + index),
            schedulerKeys.get(index),
            users.get(0).id(),
            index % 2 == 0 ? "MANUAL" : "SCHEDULED",
            "SUCCESS",
            "{\"namespace\":\"" + namespace + "\"}",
            instant("scheduler-started-" + index),
            instant("scheduler-completed-" + index)));
      }
      return executions;
    }

    private List<DemoOperationsMetric> buildOperationsMetrics() {
      return List.of(
          new DemoOperationsMetric(id("metric-review-open"), "REVIEW", "open-items", new BigDecimal("20.0000"), "count", "{\"namespace\":\"" + namespace + "\"}", instant("metric-review-open")),
          new DemoOperationsMetric(id("metric-quality-open"), "QUALITY", "open-issues", new BigDecimal("13.0000"), "count", "{\"namespace\":\"" + namespace + "\"}", instant("metric-quality-open")),
          new DemoOperationsMetric(id("metric-ocr-failures"), "OCR", "failed-documents", new BigDecimal("7.0000"), "count", "{\"namespace\":\"" + namespace + "\"}", instant("metric-ocr-failures")),
          new DemoOperationsMetric(id("metric-reindex-duration"), "REINDEX", "avg-duration-ms", new BigDecimal("4850.0000"), "ms", "{\"namespace\":\"" + namespace + "\"}", instant("metric-reindex-duration")));
    }

    private List<DemoAuditEvent> buildAuditEvents(
        List<DemoUser> users,
        List<DemoClient> clients,
        List<DemoDocument> documents,
        List<DemoReviewItem> reviewItems,
        List<DemoQualityIssue> qualityIssues) {
      List<String> actions = List.of(
          "LOGIN_SUCCESS",
          "LOGIN_FAILURE",
          "CLIENT_VIEWED",
          "CLIENT_UPDATED",
          "DOCUMENT_UPLOADED",
          "DOCUMENT_VERSION_CREATED",
          "METADATA_EDITED",
          "SEARCH_EXECUTED",
          "RESULT_OPENED",
          "RESTRICTED_ACCESS_ATTEMPT",
          "REVIEW_ASSIGNED",
          "REVIEW_APPROVE",
          "REVIEW_REJECT",
          "QUALITY_REVALIDATED",
          "QUALITY_REINDEXED",
          "ADMIN_CONFIGURATION_VIEWED",
          "ADMIN_CONFIGURATION_CHANGED",
          "EXPORT_REQUESTED",
          "AI_ASSISTANT_QUERIED",
          "EVIDENCE_REFERENCE_OPENED");
      List<DemoAuditEvent> events = new ArrayList<>();
      for (int index = 0; index < 100; index++) {
        DemoClient client = clients.get(index % clients.size());
        DemoUser user = users.get(index % users.size());
        String action = actions.get(index % actions.size());
        events.add(new DemoAuditEvent(
            instant("audit-" + index),
            action.contains("QUALITY") ? "KNOWLEDGE_QUALITY" : (action.contains("REVIEW") ? "REVIEW" : (action.contains("AI") ? "AI" : (action.contains("ADMIN") ? "ADMIN" : "SECURITY"))),
            action,
            "LOGIN_FAILURE".equals(action) || "RESTRICTED_ACCESS_ATTEMPT".equals(action) ? AuditService.AuditOutcome.DENIED : AuditService.AuditOutcome.SUCCESS,
            user.id(),
            client.id(),
            action.contains("DOCUMENT") ? "Document" : (action.contains("REVIEW") ? "ReviewQueueItem" : "Client"),
            action.contains("DOCUMENT") ? documents.get(index % documents.size()).id().toString() : (action.contains("REVIEW") ? reviewItems.get(index % reviewItems.size()).id().toString() : client.id().toString()),
            action.contains("RESTRICTED") || index % 9 == 0,
            Map.of(
                "namespace", namespace,
                "clientExternalId", client.externalId(),
                "policyNumber", index % 4 == 0 ? "TMP-PROP-90814" : "MWI-GL-" + (114920 + index),
                "claimNumber", index % 9 == 0 ? "CLM-SEA-240118" : "N/A",
                "qualityIssueId", qualityIssues.get(index % qualityIssues.size()).id().toString(),
                "correlationId", namespace + "-audit-" + index)));
      }
      return events;
    }

    private static BigDecimal score(double value) {
      return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static String slug(String value) {
      return value.toLowerCase(Locale.ROOT)
          .replaceAll("[^a-z0-9]+", "-")
          .replaceAll("^-+", "")
          .replaceAll("-+$", "");
    }
  }
}
