package com.ikms.governance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.audit.AuditLogRepository;
import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.config.AppSettingService;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.retention.RetentionRecord;
import com.ikms.retention.RetentionRecordRepository;
import com.ikms.retention.RetentionWorkflowService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GovernancePolicyService {

  private static final String CLASSIFICATION_KEY = "governance.classification.policy";
  private static final String RETENTION_KEY = "governance.retention.policy";
  private static final String AI_KEY = "governance.ai.policy";
  private static final String SECURITY_KEY = "governance.security.policy";

  private final AppSettingService appSettingService;
  private final ObjectMapper objectMapper;
  private final RetentionRecordRepository retentionRecordRepository;
  private final RetentionWorkflowService retentionWorkflowService;
  private final DocumentRepository documentRepository;
  private final AuditLogRepository auditLogRepository;
  private final AuditService auditService;

  public GovernancePolicyService(
      AppSettingService appSettingService,
      ObjectMapper objectMapper,
      RetentionRecordRepository retentionRecordRepository,
      RetentionWorkflowService retentionWorkflowService,
      DocumentRepository documentRepository,
      AuditLogRepository auditLogRepository,
      AuditService auditService) {
    this.appSettingService = appSettingService;
    this.objectMapper = objectMapper;
    this.retentionRecordRepository = retentionRecordRepository;
    this.retentionWorkflowService = retentionWorkflowService;
    this.documentRepository = documentRepository;
    this.auditLogRepository = auditLogRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public GovernanceContracts.ClassificationPolicyResponse getClassificationPolicy() {
    return read(CLASSIFICATION_KEY, GovernanceContracts.ClassificationPolicyResponse.class, defaultClassificationPolicy());
  }

  public GovernanceContracts.ClassificationPolicyResponse saveClassificationPolicy(
      GovernanceContracts.ClassificationPolicyRequest request,
      UUID actorUserId) {
    GovernanceContracts.ClassificationPolicyResponse response = new GovernanceContracts.ClassificationPolicyResponse(
        List.copyOf(request.levels()),
        request.defaultClassification(),
        request.aiRestrictionThreshold(),
        request.exportRestrictionThreshold(),
        Instant.now());
    write(CLASSIFICATION_KEY, response, "Enterprise classification policy");
    audit(actorUserId, "GOVERNANCE_CLASSIFICATION_UPDATED", Map.of("defaultClassification", request.defaultClassification()));
    return response;
  }

  @Transactional(readOnly = true)
  public GovernanceContracts.RetentionPolicyResponse getRetentionPolicy() {
    return read(RETENTION_KEY, GovernanceContracts.RetentionPolicyResponse.class, defaultRetentionPolicy());
  }

  public GovernanceContracts.RetentionPolicyResponse saveRetentionPolicy(
      GovernanceContracts.RetentionPolicyRequest request,
      UUID actorUserId) {
    GovernanceContracts.RetentionPolicyResponse response = new GovernanceContracts.RetentionPolicyResponse(
        List.copyOf(request.policies()),
        Instant.now());
    write(RETENTION_KEY, response, "Enterprise retention policy");
    audit(actorUserId, "GOVERNANCE_RETENTION_UPDATED", Map.of("policyCount", Integer.toString(request.policies().size())));
    return response;
  }

  @Transactional(readOnly = true)
  public List<GovernanceContracts.LegalHoldResponse> listLegalHolds() {
    return retentionRecordRepository.findByLegalHoldTrueOrderByUpdatedAtDesc().stream()
        .map(this::toLegalHoldResponse)
        .toList();
  }

  public GovernanceContracts.LegalHoldResponse createLegalHold(GovernanceContracts.LegalHoldRequest request, UUID actorUserId) {
    RetentionWorkflowService.RetentionDecision decision = retentionWorkflowService.evaluate(
        new RetentionWorkflowService.RetentionRequest(
            request.targetType(),
            request.targetId(),
            request.clientId(),
            actorUserId,
            RetentionWorkflowService.RetentionAction.APPLY_LEGAL_HOLD,
            true,
            null,
            request.reason(),
            request.holdType(),
            "legal-hold",
            Instant.now().plusSeconds(86400L * 30),
            null,
            null));
    if (!decision.approved()) {
      throw new IllegalArgumentException(decision.reason());
    }
    RetentionRecord record = retentionRecordRepository.findByTargetTypeAndTargetId(request.targetType(), request.targetId())
        .orElseThrow(() -> new IllegalStateException("Legal hold record was not persisted."));
    return toLegalHoldResponse(record);
  }

  public GovernanceContracts.LegalHoldResponse reclassifyDocument(UUID documentId, GovernanceContracts.ReclassifyRequest request, UUID actorUserId) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    document.setClassification(InformationClassification.parse(request.classification(), InformationClassification.INTERNAL));
    document.setSensitivityLevel(parseEnum(request.sensitivityLevel(), SensitivityLevel.class, SensitivityLevel.MODERATE));
    document.setConfidentiality(request.confidentiality());
    document.setDataResidency(request.dataResidency());
    document.setBusinessUnit(request.businessUnit());
    document.setDepartment(request.department());
    document.setRegion(request.region());
    document.setCountry(request.country());
    document.setBrokerOffice(request.brokerOffice());
    document.setExportRestricted(request.exportRestricted());
    document.setWatermarkRequired(request.watermarkRequired());
    documentRepository.save(document);
    audit(actorUserId, "DOCUMENT_RECLASSIFIED", Map.of("documentId", documentId.toString(), "classification", document.getClassification().name()));
    RetentionRecord record = retentionRecordRepository.findByTargetTypeAndTargetId("DOCUMENT", documentId.toString())
        .orElseGet(() -> {
          RetentionRecord next = new RetentionRecord();
          next.setTargetType("DOCUMENT");
          next.setTargetId(documentId.toString());
          next.setClientId(document.getClient() == null ? null : document.getClient().getId());
          return next;
        });
    retentionRecordRepository.save(record);
    return toLegalHoldResponse(record);
  }

  @Transactional(readOnly = true)
  public GovernanceContracts.AiGovernancePolicyResponse getAiGovernancePolicy() {
    return read(AI_KEY, GovernanceContracts.AiGovernancePolicyResponse.class, defaultAiPolicy());
  }

  public GovernanceContracts.AiGovernancePolicyResponse saveAiGovernancePolicy(
      GovernanceContracts.AiGovernancePolicyRequest request,
      UUID actorUserId) {
    GovernanceContracts.AiGovernancePolicyResponse response = new GovernanceContracts.AiGovernancePolicyResponse(
        List.copyOf(request.approvedModels()),
        request.promptPolicyVersion(),
        request.responsePolicyVersion(),
        request.citationRequired(),
        request.groundingValidationRequired(),
        Instant.now());
    write(AI_KEY, response, "Enterprise AI governance policy");
    audit(actorUserId, "AI_GOVERNANCE_UPDATED", Map.of("approvedModelCount", Integer.toString(request.approvedModels().size())));
    return response;
  }

  @Transactional(readOnly = true)
  public GovernanceContracts.SecurityPolicyResponse getSecurityPolicy() {
    return read(SECURITY_KEY, GovernanceContracts.SecurityPolicyResponse.class, defaultSecurityPolicy());
  }

  public GovernanceContracts.SecurityPolicyResponse saveSecurityPolicy(
      GovernanceContracts.SecurityPolicyRequest request,
      UUID actorUserId) {
    GovernanceContracts.SecurityPolicyResponse response = new GovernanceContracts.SecurityPolicyResponse(
        request.encryptionAtRest(),
        request.encryptionInTransit(),
        request.keyManagement(),
        request.secretManagement(),
        request.exportApprovalRequired(),
        request.watermarkByDefault(),
        Instant.now());
    write(SECURITY_KEY, response, "Enterprise security policy");
    audit(actorUserId, "SECURITY_POLICY_UPDATED", Map.of("watermarkByDefault", Boolean.toString(request.watermarkByDefault())));
    return response;
  }

  @Transactional(readOnly = true)
  public GovernanceContracts.ComplianceReportResponse buildComplianceReport() {
    List<Document> documents = documentRepository.findAll();
    int sensitiveDocuments = (int) documents.stream()
        .filter(document -> document.getClassification() != null
            && InformationClassification.rank(document.getClassification()) >= InformationClassification.rank(InformationClassification.CONFIDENTIAL))
        .count();
    int restrictedDocuments = (int) documents.stream()
        .filter(document -> document.getClassification() != null
            && InformationClassification.rank(document.getClassification()) >= InformationClassification.rank(InformationClassification.RESTRICTED))
        .count();
    int retentionExceptions = (int) retentionRecordRepository.findAll().stream()
        .filter(record -> !record.isLegalHold()
            && record.getDisposalEligibleAt() != null
            && record.getDisposalEligibleAt().isBefore(Instant.now())
            && !"DISPOSED".equalsIgnoreCase(record.getLastAction()))
        .count();
    int piiAuditEvents = (int) auditLogRepository.findAll().stream().filter(com.ikms.audit.AuditLog::isPiiAccess).count();
    int exportEvents = (int) auditLogRepository.findAll().stream()
        .filter(log -> log.getAction() != null && log.getAction().contains("EXPORT"))
        .count();
    int aiInteractions = (int) auditLogRepository.findAll().stream()
        .filter(log -> "AI".equalsIgnoreCase(log.getCategory()))
        .count();
    return new GovernanceContracts.ComplianceReportResponse(
        retentionRecordRepository.findByLegalHoldTrueOrderByUpdatedAtDesc().size(),
        retentionExceptions,
        sensitiveDocuments,
        restrictedDocuments,
        piiAuditEvents,
        exportEvents,
        aiInteractions,
        List.of(
            "Customer remains the primary business context.",
            "Policy and Claim values remain Business Reference Fields only.",
            "Destructive retention actions remain operator-controlled and are not automatic."));
  }

  @Transactional(readOnly = true)
  public boolean isApprovedModel(String providerName, String modelName) {
    GovernanceContracts.AiGovernancePolicyResponse policy = getAiGovernancePolicy();
    String needle = (providerName == null ? "" : providerName.trim()) + ":" + (modelName == null ? "" : modelName.trim());
    return policy.approvedModels().stream().anyMatch(value -> value.equalsIgnoreCase(needle) || value.equalsIgnoreCase(modelName));
  }

  private GovernanceContracts.LegalHoldResponse toLegalHoldResponse(RetentionRecord record) {
    return new GovernanceContracts.LegalHoldResponse(
        record.getId(),
        record.getTargetType(),
        record.getTargetId(),
        record.getClientId(),
        record.isLegalHold(),
        record.getHoldType() == null ? HoldType.NONE.name() : record.getHoldType(),
        record.getRetentionPolicyKey(),
        record.getReviewAt(),
        record.getArchivalEligibleAt(),
        record.getDisposalEligibleAt(),
        record.getExecutedAt(),
        record.getLastReason());
  }

  private <T> T read(String key, Class<T> type, T fallback) {
    return appSettingService.get(key)
        .map(value -> {
          try {
            return objectMapper.readValue(value, type);
          } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize governance setting " + key, exception);
          }
        })
        .orElse(fallback);
  }

  private void write(String key, Object value, String description) {
    try {
      appSettingService.put(key, objectMapper.writeValueAsString(value), description);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize governance setting " + key, exception);
    }
  }

  private void audit(UUID actorUserId, String action, Map<String, String> details) {
    auditService.write(new AuditEvent(
        Instant.now(),
        "GOVERNANCE",
        action,
        AuditOutcome.SUCCESS,
        actorUserId,
        null,
        "Governance",
        action,
        false,
        details));
  }

  private static GovernanceContracts.ClassificationPolicyResponse defaultClassificationPolicy() {
    return new GovernanceContracts.ClassificationPolicyResponse(
        List.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED", "HIGHLY_RESTRICTED"),
        InformationClassification.INTERNAL.name(),
        InformationClassification.RESTRICTED.name(),
        InformationClassification.CONFIDENTIAL.name(),
        Instant.EPOCH);
  }

  private static GovernanceContracts.RetentionPolicyResponse defaultRetentionPolicy() {
    return new GovernanceContracts.RetentionPolicyResponse(
        List.of(
            new GovernanceContracts.RetentionPolicyEntry("CUSTOMER_DOCUMENT", 2555, 1825, 2190, 2555),
            new GovernanceContracts.RetentionPolicyEntry("EMAIL", 1825, 1460, 1642, 1825),
            new GovernanceContracts.RetentionPolicyEntry("NOTE", 1095, 730, 912, 1095),
            new GovernanceContracts.RetentionPolicyEntry("AI_CONVERSATION", 730, 365, 548, 730),
            new GovernanceContracts.RetentionPolicyEntry("REVIEW_RECORD", 2555, 1825, 2190, 2555),
            new GovernanceContracts.RetentionPolicyEntry("AUDIT_EVENT", 2555, 2190, 2372, 2555)),
        Instant.EPOCH);
  }

  private static GovernanceContracts.AiGovernancePolicyResponse defaultAiPolicy() {
    return new GovernanceContracts.AiGovernancePolicyResponse(
        List.of("mistral:mistral-small", "openai:gpt-5-mini"),
        "prompt-policy-v1",
        "response-policy-v1",
        true,
        true,
        Instant.EPOCH);
  }

  private static GovernanceContracts.SecurityPolicyResponse defaultSecurityPolicy() {
    return new GovernanceContracts.SecurityPolicyResponse(
        "AES-256 or equivalent file and database encryption at rest.",
        "TLS 1.2+ for all service-to-service and user-to-service traffic.",
        "Provider-agnostic key management abstraction with rotation-capable key identifiers.",
        "Secrets remain outside source control and are injected through environment or secret stores.",
        true,
        true,
        Instant.EPOCH);
  }

  private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }
}
