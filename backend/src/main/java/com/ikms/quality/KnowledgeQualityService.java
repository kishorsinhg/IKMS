package com.ikms.quality;

import com.ikms.ai.EmbeddingChunk;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.client.Client;
import com.ikms.client.ClientRepository;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentProcessingField;
import com.ikms.document.DocumentProcessingFieldRepository;
import com.ikms.document.DocumentProcessingJob;
import com.ikms.document.DocumentProcessingJobRepository;
import com.ikms.document.DocumentProcessingStatus;
import com.ikms.document.DocumentPublishingService;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.Email;
import com.ikms.email.EmailRepository;
import com.ikms.note.Note;
import com.ikms.note.NoteRepository;
import com.ikms.review.ReviewQueueItem;
import com.ikms.review.ReviewQueueRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KnowledgeQualityService {

  private static final int CURRENT_REINDEX_VERSION = 2;

  private static final Set<String> POLICY_KEYS = Set.of("policynumber", "policy_number");
  private static final Set<String> CLAIM_KEYS = Set.of("claimnumber", "claim_number");
  private static final Set<String> BROKER_KEYS = Set.of("brokerreference", "broker_reference");
  private static final Set<String> INSURER_KEYS = Set.of("insurer", "carrier");
  private static final Set<String> EFFECTIVE_DATE_KEYS = Set.of("effectivedate", "effective_date");
  private static final Set<String> EXPIRY_DATE_KEYS = Set.of("expirydate", "expiry_date", "expirationdate", "expiration_date");
  private static final Set<String> RENEWAL_DATE_KEYS = Set.of("renewaldate", "renewal_date");

  private final ClientRepository clientRepository;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final ReviewQueueRepository reviewQueueRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final DocumentProcessingJobRepository documentProcessingJobRepository;
  private final DocumentProcessingFieldRepository documentProcessingFieldRepository;
  private final KnowledgeQualitySnapshotRepository snapshotRepository;
  private final KnowledgeQualityIssueRepository issueRepository;
  private final DocumentPublishingService documentPublishingService;
  private final EmbeddingIndexService embeddingIndexService;
  private final AuditService auditService;

  public KnowledgeQualityService(
      ClientRepository clientRepository,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      ReviewQueueRepository reviewQueueRepository,
      MetadataValueRepository metadataValueRepository,
      EmbeddingChunkRepository embeddingChunkRepository,
      DocumentProcessingJobRepository documentProcessingJobRepository,
      DocumentProcessingFieldRepository documentProcessingFieldRepository,
      KnowledgeQualitySnapshotRepository snapshotRepository,
      KnowledgeQualityIssueRepository issueRepository,
      DocumentPublishingService documentPublishingService,
      EmbeddingIndexService embeddingIndexService,
      AuditService auditService) {
    this.clientRepository = clientRepository;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.reviewQueueRepository = reviewQueueRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.documentProcessingJobRepository = documentProcessingJobRepository;
    this.documentProcessingFieldRepository = documentProcessingFieldRepository;
    this.snapshotRepository = snapshotRepository;
    this.issueRepository = issueRepository;
    this.documentPublishingService = documentPublishingService;
    this.embeddingIndexService = embeddingIndexService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public KnowledgeQualityContracts.KnowledgeQualityCustomerListResponse listCustomers(String query, boolean refresh) {
    List<Client> clients = clientRepository.searchByQuery(query == null ? "" : query.trim());
    List<KnowledgeQualityContracts.CustomerKnowledgeQualitySummaryResponse> responses = clients.stream()
        .map(client -> refresh ? evaluate(client).summary() : findOrEvaluate(client).summary())
        .sorted(Comparator.comparing(KnowledgeQualityContracts.CustomerKnowledgeQualitySummaryResponse::overallScore))
        .toList();
    return new KnowledgeQualityContracts.KnowledgeQualityCustomerListResponse(responses);
  }

  @Transactional(readOnly = true)
  public KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse customer(UUID clientId, boolean refresh) {
    Client client = requireClient(clientId);
    return refresh ? evaluate(client) : findOrEvaluate(client);
  }

  @Transactional(readOnly = true)
  public KnowledgeQualityContracts.KnowledgeQualityIssueQueueResponse issues(UUID clientId) {
    List<KnowledgeQualityIssue> issues = clientId == null
        ? issueRepository.findByStatusOrderBySeverityDescCreatedAtAsc(KnowledgeQualityIssueStatus.OPEN)
        : issueRepository.findByClientIdAndStatusOrderBySeverityDescCreatedAtAsc(clientId, KnowledgeQualityIssueStatus.OPEN);
    return new KnowledgeQualityContracts.KnowledgeQualityIssueQueueResponse(
        issues.stream().map(this::toIssueResponse).toList());
  }

  public KnowledgeQualityContracts.KnowledgeQualityActionResultResponse revalidate(
      KnowledgeQualityContracts.QualityRevalidateRequest request,
      UUID actorUserId) {
    requireConfirmed(request.confirmed());
    List<UUID> distinctClientIds = request.clientIds().stream().distinct().toList();
    distinctClientIds.forEach(clientId -> evaluate(requireClient(clientId)));
    auditService.write(new AuditEvent(
        Instant.now(),
        "KNOWLEDGE_QUALITY",
        "QUALITY_REVALIDATED",
        AuditOutcome.SUCCESS,
        actorUserId,
        null,
        "KnowledgeQuality",
        "bulk-revalidate",
        false,
        Map.of("customerCount", Integer.toString(distinctClientIds.size()))));
    return new KnowledgeQualityContracts.KnowledgeQualityActionResultResponse(
        "REVALIDATE",
        distinctClientIds.size(),
        distinctClientIds.size(),
        distinctClientIds,
        Instant.now());
  }

  public KnowledgeQualityContracts.KnowledgeQualityActionResultResponse reindex(
      KnowledgeQualityContracts.QualityReindexRequest request,
      UUID actorUserId) {
    requireConfirmed(request.confirmed());
    int affectedItems = 0;
    List<UUID> distinctClientIds = request.clientIds().stream().distinct().toList();
    for (UUID clientId : distinctClientIds) {
      Client client = requireClient(clientId);
      for (Document document : documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
        documentPublishingService.publish(document);
        affectedItems++;
      }
      for (Email email : emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId)) {
        embeddingIndexService.indexEmail(clientId, email);
        affectedItems++;
      }
      for (Note note : noteRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
        embeddingIndexService.indexNote(clientId, note);
        affectedItems++;
      }
      evaluate(client);
    }
    auditService.write(new AuditEvent(
        Instant.now(),
        "KNOWLEDGE_QUALITY",
        "QUALITY_REINDEXED",
        AuditOutcome.SUCCESS,
        actorUserId,
        null,
        "KnowledgeQuality",
        "bulk-reindex",
        false,
        Map.of(
            "customerCount", Integer.toString(distinctClientIds.size()),
            "itemCount", Integer.toString(affectedItems))));
    return new KnowledgeQualityContracts.KnowledgeQualityActionResultResponse(
        "REINDEX",
        distinctClientIds.size(),
        affectedItems,
        distinctClientIds,
        Instant.now());
  }

  public KnowledgeQualityContracts.KnowledgeQualityActionResultResponse bulkCorrect(
      KnowledgeQualityContracts.BulkQualityCorrectionRequest request,
      UUID actorUserId) {
    requireConfirmed(request.confirmed());
    int affectedItems = 0;
    Set<UUID> clientIds = new HashSet<>();
    String operation = request.operationType().trim().toUpperCase(Locale.ROOT);
    for (KnowledgeQualityContracts.BulkQualityCorrectionItemRequest item : request.items()) {
      clientIds.add(item.clientId());
      switch (operation) {
        case "METADATA_CORRECTION", "BUSINESS_REFERENCE_CORRECTION" -> {
          applyMetadataCorrection(item);
          affectedItems++;
        }
        case "CUSTOMER_REASSIGNMENT" -> {
          applyCustomerReassignment(item);
          affectedItems++;
        }
        case "PUBLISH" -> {
          documentRepository.findById(item.sourceId()).ifPresent(documentPublishingService::publish);
          affectedItems++;
        }
        default -> throw new IllegalArgumentException("Unsupported bulk quality operation: " + request.operationType());
      }
    }
    clientIds.forEach(clientId -> evaluate(requireClient(clientId)));
    auditService.write(new AuditEvent(
        Instant.now(),
        "KNOWLEDGE_QUALITY",
        "QUALITY_BULK_ACTION",
        AuditOutcome.SUCCESS,
        actorUserId,
        null,
        "KnowledgeQuality",
        operation,
        false,
        Map.of(
            "customerCount", Integer.toString(clientIds.size()),
            "itemCount", Integer.toString(affectedItems))));
    return new KnowledgeQualityContracts.KnowledgeQualityActionResultResponse(
        operation,
        clientIds.size(),
        affectedItems,
        List.copyOf(clientIds),
        Instant.now());
  }

  private void applyMetadataCorrection(KnowledgeQualityContracts.BulkQualityCorrectionItemRequest item) {
    if (!"DOCUMENT".equalsIgnoreCase(item.sourceType()) || item.sourceId() == null || item.fieldKey() == null) {
      throw new IllegalArgumentException("Bulk metadata correction requires DOCUMENT sourceType, sourceId, and fieldKey.");
    }
    List<MetadataValue> metadataValues = metadataValueRepository.findByOwnerTypeAndOwnerId("DOCUMENT", item.sourceId());
    MetadataValue existing = metadataValues.stream()
        .filter(value -> normalizedKey(value.getField().getFieldKey()).equals(normalizedKey(item.fieldKey())))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Metadata field not found for document correction: " + item.fieldKey()));
    existing.setTextValue(item.value() == null ? null : item.value().trim());
    metadataValueRepository.save(existing);
    DocumentProcessingJob latestJob = documentProcessingJobRepository.findTopByDocument_IdOrderByCreatedAtDesc(item.sourceId()).orElse(null);
    if (latestJob != null) {
      documentProcessingFieldRepository.findByJob_IdAndFieldKeyIgnoreCase(latestJob.getId(), item.fieldKey())
          .ifPresent(field -> {
            field.setCorrectedValue(item.value() == null ? null : item.value().trim());
            documentProcessingFieldRepository.save(field);
          });
    }
  }

  private void applyCustomerReassignment(KnowledgeQualityContracts.BulkQualityCorrectionItemRequest item) {
    if (!"DOCUMENT".equalsIgnoreCase(item.sourceType()) || item.sourceId() == null || item.targetClientId() == null) {
      throw new IllegalArgumentException("Bulk customer reassignment requires DOCUMENT sourceType, sourceId, and targetClientId.");
    }
    Document document = documentRepository.findById(item.sourceId())
        .orElseThrow(() -> new IllegalArgumentException("Document not found for customer reassignment: " + item.sourceId()));
    document.setClient(requireClient(item.targetClientId()));
    documentRepository.save(document);
  }

  private KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse findOrEvaluate(Client client) {
    return snapshotRepository.findByClient_Id(client.getId())
        .map(this::toDetailResponse)
        .orElseGet(() -> evaluate(client));
  }

  private KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse evaluate(Client client) {
    EvaluationContext context = loadContext(client);
    List<IssueDraft> issues = new ArrayList<>();

    BigDecimal completenessScore = evaluateMetadataCompleteness(context, issues);
    BigDecimal businessReferenceScore = evaluateBusinessReferences(context, issues);
    BigDecimal linkageScore = evaluateLinkage(context, issues);
    BigDecimal duplicateScore = evaluateDuplicates(context, issues);
    BigDecimal timelineScore = evaluateTimeline(context, issues);
    BigDecimal versionScore = evaluateVersions(context, issues);
    BigDecimal retrievalScore = evaluateRetrievalReadiness(context, issues);
    BigDecimal aiQualityScore = evaluateAiQuality(context, issues);
    BigDecimal overallScore = average(
        completenessScore,
        businessReferenceScore,
        linkageScore,
        duplicateScore,
        timelineScore,
        versionScore,
        retrievalScore,
        aiQualityScore);

    KnowledgeQualityReadinessState readinessState = deriveReadiness(issues, overallScore);
    KnowledgeQualitySnapshot snapshot = snapshotRepository.findByClient_Id(client.getId()).orElseGet(KnowledgeQualitySnapshot::new);
    snapshot.setClient(client);
    snapshot.setOverallScore(overallScore);
    snapshot.setCompletenessScore(completenessScore);
    snapshot.setBusinessReferenceScore(businessReferenceScore);
    snapshot.setLinkageScore(linkageScore);
    snapshot.setDuplicateScore(duplicateScore);
    snapshot.setTimelineScore(timelineScore);
    snapshot.setVersionScore(versionScore);
    snapshot.setRetrievalReadinessScore(retrievalScore);
    snapshot.setAiQualityScore(aiQualityScore);
    snapshot.setIssueCount(issues.size());
    snapshot.setOpenIssueCount(issues.size());
    snapshot.setReadinessState(readinessState);
    snapshot.setSummaryText(summaryFor(snapshot, issues));
    snapshot.setEvaluatedAt(Instant.now());
    snapshot = snapshotRepository.save(snapshot);

    issueRepository.deleteBySnapshot_Id(snapshot.getId());
    List<KnowledgeQualityIssue> persistedIssues = new ArrayList<>();
    for (IssueDraft draft : issues) {
      KnowledgeQualityIssue issue = new KnowledgeQualityIssue();
      issue.setSnapshot(snapshot);
      issue.setClientId(client.getId());
      issue.setSourceType(draft.sourceType());
      issue.setSourceId(draft.sourceId());
      issue.setCategory(draft.category());
      issue.setIssueType(draft.issueType());
      issue.setSeverity(draft.severity());
      issue.setStatus(KnowledgeQualityIssueStatus.OPEN);
      issue.setTitle(draft.title());
      issue.setDetailText(draft.detail());
      issue.setRecommendationType(draft.recommendationType());
      issue.setRecommendationDetail(draft.recommendationDetail());
      issue.setBusinessReferenceKey(draft.businessReferenceKey());
      issue.setScoreImpact(draft.scoreImpact());
      persistedIssues.add(issueRepository.save(issue));
    }
    return toDetailResponse(snapshot, persistedIssues);
  }

  private EvaluationContext loadContext(Client client) {
    List<Document> documents = documentRepository.findByClient_IdOrderByCreatedAtDesc(client.getId());
    Map<UUID, DocumentVersion> currentVersions = new HashMap<>();
    Map<UUID, List<DocumentVersion>> allVersions = new HashMap<>();
    Map<UUID, List<MetadataValue>> metadataByDocument = new HashMap<>();
    Map<UUID, DocumentProcessingJob> jobsByDocument = new HashMap<>();
    Map<UUID, List<DocumentProcessingField>> fieldsByJob = new HashMap<>();

    for (Document document : documents) {
      documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).ifPresent(version -> currentVersions.put(document.getId(), version));
      allVersions.put(document.getId(), documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(document.getId()));
      metadataByDocument.put(document.getId(), metadataValueRepository.findByOwnerTypeAndOwnerId("DOCUMENT", document.getId()));
      DocumentProcessingJob latestJob = documentProcessingJobRepository.findTopByDocument_IdOrderByCreatedAtDesc(document.getId()).orElse(null);
      if (latestJob != null) {
        jobsByDocument.put(document.getId(), latestJob);
        fieldsByJob.put(latestJob.getId(), documentProcessingFieldRepository.findByJob_IdOrderByFieldKeyAsc(latestJob.getId()));
      }
    }

    return new EvaluationContext(
        client,
        documents,
        currentVersions,
        allVersions,
        metadataByDocument,
        jobsByDocument,
        fieldsByJob,
        emailRepository.findByClient_IdOrderByReceivedAtDesc(client.getId()),
        noteRepository.findByClient_IdOrderByCreatedAtDesc(client.getId()),
        reviewQueueRepository.findAll(),
        embeddingChunkRepository.findByClientIdOrderByCreatedAtDesc(client.getId()));
  }

  private BigDecimal evaluateMetadataCompleteness(EvaluationContext context, List<IssueDraft> issues) {
    int totalRequired = 0;
    int validRequired = 0;
    for (Document document : context.documents()) {
      DocumentProcessingJob job = context.jobsByDocument().get(document.getId());
      if (job == null) {
        issues.add(issue(
            "METADATA",
            "MISSING_PROCESSING_JOB",
            KnowledgeQualityIssueSeverity.MEDIUM,
            document.getId(),
            "DOCUMENT",
            "Document is missing a processing-job lineage record.",
            "Revalidate the document processing state.",
            "REVALIDATE",
            "Run knowledge revalidation for the customer.",
            null,
            new BigDecimal("0.1000")));
        continue;
      }
      List<DocumentProcessingField> fields = context.fieldsByJob().getOrDefault(job.getId(), List.of());
      for (DocumentProcessingField field : fields) {
        if (!field.isRequiredFlag()) {
          continue;
        }
        totalRequired++;
        String value = preferredFieldValue(field);
        if (value != null && !value.isBlank()) {
          validRequired++;
        } else {
          issues.add(issue(
              "METADATA",
              "MISSING_REQUIRED_METADATA",
              KnowledgeQualityIssueSeverity.HIGH,
              document.getId(),
              "DOCUMENT",
              "Required metadata is missing for " + field.getFieldLabel() + ".",
              "Correct the missing metadata value before relying on this document in retrieval workflows.",
              "UPDATE_METADATA",
              "Set " + field.getFieldLabel() + " on the document metadata.",
              field.getFieldKey(),
              new BigDecimal("0.1500")));
        }
      }
    }
    if (totalRequired == 0) {
      return new BigDecimal("0.7500");
    }
    return ratio(validRequired, totalRequired);
  }

  private BigDecimal evaluateBusinessReferences(EvaluationContext context, List<IssueDraft> issues) {
    int checks = 0;
    int passed = 0;
    Map<String, Set<String>> insurersByPolicy = new HashMap<>();
    for (Document document : context.documents()) {
      Map<String, String> metadata = metadataMap(context.metadataByDocument().getOrDefault(document.getId(), List.of()));
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        String key = normalizedKey(entry.getKey());
        String value = entry.getValue();
        if (POLICY_KEYS.contains(key)) {
          checks++;
          if (matchesReference(value, "^[A-Z0-9\\-]{5,40}$")) {
            passed++;
          } else {
            issues.add(referenceIssue(document.getId(), "Policy Number", value, "Review or correct the Policy Number format.", "policy_number"));
          }
        }
        if (CLAIM_KEYS.contains(key)) {
          checks++;
          if (matchesReference(value, "^[A-Z0-9\\-]{4,40}$")) {
            passed++;
          } else {
            issues.add(referenceIssue(document.getId(), "Claim Number", value, "Review or correct the Claim Number format.", "claim_number"));
          }
        }
        if (BROKER_KEYS.contains(key)) {
          checks++;
          if (matchesReference(value, "^[A-Z0-9\\-]{3,40}$")) {
            passed++;
          } else {
            issues.add(referenceIssue(document.getId(), "Broker Reference", value, "Review or correct the Broker Reference format.", "broker_reference"));
          }
        }
        if (INSURER_KEYS.contains(key)) {
          checks++;
          if (value != null && !value.isBlank()) {
            passed++;
          } else {
            issues.add(referenceIssue(document.getId(), "Insurer", value, "Add or correct the insurer metadata.", "insurer"));
          }
        }
        if (EFFECTIVE_DATE_KEYS.contains(key) || EXPIRY_DATE_KEYS.contains(key) || RENEWAL_DATE_KEYS.contains(key)) {
          checks++;
          if (isDate(value)) {
            passed++;
          } else {
            issues.add(referenceIssue(document.getId(), humanizeKey(entry.getKey()), value, "Correct the date format.", entry.getKey()));
          }
        }
      }
      String policyNumber = firstValue(metadata, POLICY_KEYS);
      String insurer = firstValue(metadata, INSURER_KEYS);
      if (policyNumber != null && insurer != null) {
        insurersByPolicy.computeIfAbsent(policyNumber, ignored -> new HashSet<>()).add(insurer.trim().toLowerCase(Locale.ROOT));
      }
    }
    insurersByPolicy.forEach((policyNumber, insurers) -> {
      if (insurers.size() > 1) {
        issues.add(issue(
            "BUSINESS_REFERENCE",
            "CONFLICTING_INSURER",
            KnowledgeQualityIssueSeverity.HIGH,
            null,
            "DOCUMENT",
            "Conflicting insurer values were detected for Policy Reference " + policyNumber + ".",
            "Steward review is required to confirm the correct insurer metadata.",
            "BUSINESS_REFERENCE_CORRECTION",
            "Align insurer metadata across documents sharing the same policy reference.",
            "insurer",
            new BigDecimal("0.1200")));
      }
    });
    return checks == 0 ? new BigDecimal("0.8500") : ratio(passed, checks);
  }

  private BigDecimal evaluateLinkage(EvaluationContext context, List<IssueDraft> issues) {
    int total = context.documents().size() + context.emails().size();
    int linked = context.documents().size() + context.emails().size();
    for (Document document : context.documents()) {
      if (document.getClient() == null) {
        linked--;
        issues.add(issue(
            "LINKAGE",
            "UNLINKED_DOCUMENT",
            KnowledgeQualityIssueSeverity.CRITICAL,
            document.getId(),
            "DOCUMENT",
            "Document is not linked to a customer.",
            "Reassign the document to the correct customer before using it as trusted customer knowledge.",
            "CUSTOMER_REASSIGNMENT",
            "Assign the document to the intended customer.",
            null,
            new BigDecimal("0.2000")));
      }
    }
    for (Email email : context.emails()) {
      if (email.getClient() == null) {
        linked--;
        issues.add(issue(
            "LINKAGE",
            "UNLINKED_EMAIL",
            KnowledgeQualityIssueSeverity.HIGH,
            email.getId(),
            "EMAIL",
            "Email is not linked to a customer.",
            "Review the customer linkage before using the correspondence in customer retrieval workflows.",
            "CUSTOMER_REASSIGNMENT",
            "Assign the email to the intended customer.",
            null,
            new BigDecimal("0.1800")));
      }
    }
    if (total == 0) {
      return BigDecimal.ONE;
    }
    return ratio(linked, total);
  }

  private BigDecimal evaluateDuplicates(EvaluationContext context, List<IssueDraft> issues) {
    Map<String, List<Document>> byTitle = context.documents().stream()
        .collect(Collectors.groupingBy(document -> document.getTitle().trim().toLowerCase(Locale.ROOT)));
    int checks = Math.max(context.documents().size(), 1);
    int clean = context.documents().size();
    byTitle.values().forEach(matches -> {
      if (matches.size() > 1) {
        matches.forEach(document -> issues.add(issue(
            "DUPLICATE",
            "POSSIBLE_DUPLICATE_DOCUMENT",
            KnowledgeQualityIssueSeverity.MEDIUM,
            document.getId(),
            "DOCUMENT",
            "Possible duplicate document detected from matching title and customer scope.",
            "Review duplicate lineage before relying on both sources independently.",
            "REVIEW_DUPLICATE",
            "Confirm the preferred source and version lineage.",
            null,
            new BigDecimal("0.0800"))));
      }
    });
    for (DocumentProcessingJob job : context.jobsByDocument().values()) {
      if (job.getDuplicateConfidence() != null && job.getDuplicateConfidence().compareTo(new BigDecimal("0.8500")) >= 0) {
        clean--;
        issues.add(issue(
            "DUPLICATE",
            "HIGH_DUPLICATE_CONFIDENCE",
            KnowledgeQualityIssueSeverity.HIGH,
            job.getDocument().getId(),
            "DOCUMENT",
            "Processing pipeline flagged this document as a likely duplicate.",
            "Human review is required before duplicate knowledge is treated as trusted customer evidence.",
            "REVIEW_DUPLICATE",
            "Confirm whether the document is a duplicate or a valid new version.",
            null,
            new BigDecimal("0.1200")));
      }
    }
    return ratio(Math.max(clean, 0), checks);
  }

  private BigDecimal evaluateTimeline(EvaluationContext context, List<IssueDraft> issues) {
    int checks = 0;
    int passed = 0;
    Instant latest = null;
    for (Document document : context.documents()) {
      checks++;
      if (document.getCreatedAt() != null) {
        passed++;
      } else {
        issues.add(issue(
            "TIMELINE",
            "MISSING_DOCUMENT_DATE",
            KnowledgeQualityIssueSeverity.MEDIUM,
            document.getId(),
            "DOCUMENT",
            "Document is missing a recorded timeline date.",
            "Correct the source dates so customer chronology remains reliable.",
            "UPDATE_METADATA",
            "Add or repair the relevant document date metadata.",
            null,
            new BigDecimal("0.0700")));
      }
      if (latest != null && document.getCreatedAt() != null && document.getCreatedAt().isAfter(latest.plusSeconds(1))) {
        passed++;
      }
      latest = document.getCreatedAt() == null ? latest : document.getCreatedAt();
    }
    for (Email email : context.emails()) {
      checks++;
      if (email.getReceivedAt() != null) {
        passed++;
      } else {
        issues.add(issue(
            "TIMELINE",
            "MISSING_EMAIL_DATE",
            KnowledgeQualityIssueSeverity.MEDIUM,
            email.getId(),
            "EMAIL",
            "Email is missing a received date.",
            "Repair the source chronology to keep the customer knowledge timeline trustworthy.",
            "REVALIDATE",
            "Re-evaluate the customer timeline after correcting the source timestamps.",
            null,
            new BigDecimal("0.0700")));
      }
    }
    return checks == 0 ? BigDecimal.ONE : ratio(passed, checks);
  }

  private BigDecimal evaluateVersions(EvaluationContext context, List<IssueDraft> issues) {
    int checks = 0;
    int passed = 0;
    for (Document document : context.documents()) {
      List<DocumentVersion> versions = context.versionsByDocument().getOrDefault(document.getId(), List.of());
      if (versions.isEmpty()) {
        continue;
      }
      checks++;
      long currentCount = versions.stream().filter(DocumentVersion::isCurrent).count();
      if (currentCount == 1 && document.getCurrentVersionId() != null) {
        passed++;
      } else {
        issues.add(issue(
            "VERSION",
            "INVALID_CURRENT_VERSION",
            KnowledgeQualityIssueSeverity.HIGH,
            document.getId(),
            "DOCUMENT",
            "Document version lineage has no single current version.",
            "Repair version lineage before relying on current-version retrieval.",
            "PUBLISH",
            "Republish the preferred current version after correcting lineage.",
            null,
            new BigDecimal("0.1100")));
      }
      List<Integer> versionNumbers = versions.stream().map(DocumentVersion::getVersionNumber).sorted().toList();
      for (int index = 1; index < versionNumbers.size(); index++) {
        if (versionNumbers.get(index) - versionNumbers.get(index - 1) > 1) {
          issues.add(issue(
              "VERSION",
              "VERSION_GAP",
              KnowledgeQualityIssueSeverity.MEDIUM,
              document.getId(),
              "DOCUMENT",
              "Document version numbering contains gaps.",
              "Review version lineage for missing or incorrectly numbered versions.",
              "REVALIDATE",
              "Re-evaluate version quality after lineage correction.",
              null,
              new BigDecimal("0.0600")));
          break;
        }
      }
    }
    return checks == 0 ? BigDecimal.ONE : ratio(passed, checks);
  }

  private BigDecimal evaluateRetrievalReadiness(EvaluationContext context, List<IssueDraft> issues) {
    int checks = 0;
    int passed = 0;
    for (Document document : context.documents()) {
      DocumentVersion currentVersion = context.currentVersions().get(document.getId());
      if (currentVersion == null) {
        continue;
      }
      checks++;
      List<EmbeddingChunk> chunks = context.embeddingChunks().stream()
          .filter(chunk -> "DOCUMENT".equals(chunk.getSourceType()) && currentVersion.getId().equals(chunk.getDocumentVersionId()))
          .toList();
      if (currentVersion.getExtractedText() == null || currentVersion.getExtractedText().isBlank()) {
        issues.add(issue(
            "RETRIEVAL",
            "MISSING_EXTRACTED_TEXT",
            KnowledgeQualityIssueSeverity.CRITICAL,
            document.getId(),
            "DOCUMENT",
            "Document is missing extracted text and is not retrieval-ready.",
            "Reprocess OCR or text extraction before relying on retrieval results.",
            "REVALIDATE",
            "Re-run extraction and quality evaluation.",
            null,
            new BigDecimal("0.2000")));
        continue;
      }
      if (chunks.isEmpty()) {
        issues.add(issue(
            "RETRIEVAL",
            "MISSING_EMBEDDINGS",
            KnowledgeQualityIssueSeverity.HIGH,
            document.getId(),
            "DOCUMENT",
            "Document is missing retrieval projection chunks or embeddings.",
            "Run reindex to restore retrieval readiness.",
            "REINDEX",
            "Reindex the customer knowledge projection.",
            null,
            new BigDecimal("0.1400")));
        continue;
      }
      if (chunks.stream().anyMatch(chunk -> chunk.getReindexVersion() == null || chunk.getReindexVersion() < CURRENT_REINDEX_VERSION)) {
        issues.add(issue(
            "RETRIEVAL",
            "STALE_PROJECTION",
            KnowledgeQualityIssueSeverity.MEDIUM,
            document.getId(),
            "DOCUMENT",
            "Document retrieval projection is stale.",
            "Reindex the customer knowledge projection to restore current retrieval quality.",
            "REINDEX",
            "Reindex the customer knowledge projection.",
            null,
            new BigDecimal("0.0800")));
        continue;
      }
      passed++;
    }
    return checks == 0 ? BigDecimal.ONE : ratio(passed, checks);
  }

  private BigDecimal evaluateAiQuality(EvaluationContext context, List<IssueDraft> issues) {
    int checks = 0;
    int passed = 0;
    for (DocumentProcessingJob job : context.jobsByDocument().values()) {
      checks++;
      if (job.getOverallConfidence() != null && job.getOverallConfidence().compareTo(new BigDecimal("0.7000")) >= 0) {
        passed++;
      } else {
        issues.add(issue(
            "AI_QUALITY",
            "LOW_AI_CONFIDENCE",
            KnowledgeQualityIssueSeverity.MEDIUM,
            job.getDocument().getId(),
            "DOCUMENT",
            "AI-assisted extraction confidence is below the quality threshold.",
            "Human steward review is recommended before relying on downstream retrieval or assistant workflows.",
            "REVIEW_EXTRACTION",
            "Review extracted metadata and supporting evidence.",
            null,
            new BigDecimal("0.0900")));
      }
    }
    for (ReviewQueueItem item : context.reviewItems()) {
      if (item.getItemType().name().contains("DOCUMENT") && item.getStatus().name().equals("OPEN")) {
        issues.add(issue(
            "AI_QUALITY",
            "OPEN_REVIEW_DEPENDENCY",
            KnowledgeQualityIssueSeverity.MEDIUM,
            UUID.fromString(item.getItemId()),
            "DOCUMENT",
            "Document still has an open review dependency affecting trustworthiness.",
            "Resolve the review dependency before treating the knowledge as fully trusted.",
            "REVIEW_EXTRACTION",
            "Complete human review of the pending item.",
            null,
            new BigDecimal("0.0700")));
      }
    }
    return checks == 0 ? new BigDecimal("0.9000") : ratio(passed, checks);
  }

  private KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse toDetailResponse(KnowledgeQualitySnapshot snapshot) {
    return toDetailResponse(snapshot, issueRepository.findBySnapshot_IdOrderBySeverityDescCreatedAtAsc(snapshot.getId()));
  }

  private KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse toDetailResponse(
      KnowledgeQualitySnapshot snapshot,
      List<KnowledgeQualityIssue> issues) {
    return new KnowledgeQualityContracts.CustomerKnowledgeQualityDetailResponse(
        toSummary(snapshot, issues),
        issues.stream().map(this::toIssueResponse).toList());
  }

  private KnowledgeQualityContracts.CustomerKnowledgeQualitySummaryResponse toSummary(
      KnowledgeQualitySnapshot snapshot,
      List<KnowledgeQualityIssue> issues) {
    return new KnowledgeQualityContracts.CustomerKnowledgeQualitySummaryResponse(
        snapshot.getClient().getId(),
        snapshot.getClient().getDisplayName(),
        snapshot.getClient().getClientId(),
        snapshot.getOverallScore(),
        snapshot.getReadinessState().name(),
        snapshot.getIssueCount(),
        snapshot.getOpenIssueCount(),
        snapshot.getEvaluatedAt(),
        List.of(
            dimension("completeness", "Metadata Completeness", snapshot.getCompletenessScore(), "Required metadata coverage"),
            dimension("businessReferences", "Business Reference Quality", snapshot.getBusinessReferenceScore(), "Searchable reference validity"),
            dimension("linkage", "Customer Linkage", snapshot.getLinkageScore(), "Customer ownership and orphan checks"),
            dimension("duplicates", "Duplicate Quality", snapshot.getDuplicateScore(), "Duplicate and reprocessed knowledge checks"),
            dimension("timeline", "Timeline Quality", snapshot.getTimelineScore(), "Chronology and source continuity"),
            dimension("versions", "Version Quality", snapshot.getVersionScore(), "Current-version and lineage checks"),
            dimension("retrieval", "Retrieval Readiness", snapshot.getRetrievalReadinessScore(), "Projection and embedding readiness"),
            dimension("ai", "AI Quality", snapshot.getAiQualityScore(), "Confidence and review dependency quality")),
        issues.stream()
            .map(KnowledgeQualityIssue::getRecommendationType)
            .filter(Objects::nonNull)
            .distinct()
            .limit(3)
            .toList());
  }

  private KnowledgeQualityContracts.QualityDimensionScoreResponse dimension(
      String key,
      String label,
      BigDecimal score,
      String summary) {
    return new KnowledgeQualityContracts.QualityDimensionScoreResponse(key, label, score, summary);
  }

  private KnowledgeQualityContracts.KnowledgeQualityIssueResponse toIssueResponse(KnowledgeQualityIssue issue) {
    return new KnowledgeQualityContracts.KnowledgeQualityIssueResponse(
        issue.getId(),
        issue.getClientId(),
        issue.getSourceType(),
        issue.getSourceId(),
        issue.getCategory(),
        issue.getIssueType(),
        issue.getSeverity().name(),
        issue.getStatus().name(),
        issue.getTitle(),
        issue.getDetailText(),
        issue.getRecommendationType(),
        issue.getRecommendationDetail(),
        issue.getBusinessReferenceKey(),
        issue.getScoreImpact(),
        issue.getCreatedAt(),
        issue.getUpdatedAt());
  }

  private KnowledgeQualityReadinessState deriveReadiness(List<IssueDraft> issues, BigDecimal overallScore) {
    boolean hasCritical = issues.stream().anyMatch(issue -> issue.severity() == KnowledgeQualityIssueSeverity.CRITICAL);
    if (hasCritical || overallScore.compareTo(new BigDecimal("0.4500")) < 0) {
      return KnowledgeQualityReadinessState.BLOCKED;
    }
    if (overallScore.compareTo(new BigDecimal("0.8000")) >= 0 && issues.stream().noneMatch(issue -> issue.severity().ordinal() >= KnowledgeQualityIssueSeverity.HIGH.ordinal())) {
      return KnowledgeQualityReadinessState.READY;
    }
    return KnowledgeQualityReadinessState.NEEDS_ATTENTION;
  }

  private String summaryFor(KnowledgeQualitySnapshot snapshot, List<IssueDraft> issues) {
    if (issues.isEmpty()) {
      return "Customer knowledge is complete, linked, and retrieval-ready.";
    }
    return snapshot.getReadinessState() == KnowledgeQualityReadinessState.BLOCKED
        ? "Critical knowledge quality issues are blocking retrieval readiness."
        : "Customer knowledge requires stewardship review to improve trust and retrieval quality.";
  }

  private BigDecimal average(BigDecimal... values) {
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal value : values) {
      total = total.add(value);
    }
    return total.divide(BigDecimal.valueOf(values.length), 4, RoundingMode.HALF_UP);
  }

  private BigDecimal ratio(int numerator, int denominator) {
    if (denominator <= 0) {
      return BigDecimal.ONE;
    }
    return BigDecimal.valueOf(numerator)
        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
        .max(BigDecimal.ZERO)
        .min(BigDecimal.ONE);
  }

  private IssueDraft issue(
      String category,
      String issueType,
      KnowledgeQualityIssueSeverity severity,
      UUID sourceId,
      String sourceType,
      String title,
      String detail,
      String recommendationType,
      String recommendationDetail,
      String businessReferenceKey,
      BigDecimal scoreImpact) {
    return new IssueDraft(category, issueType, severity, sourceType, sourceId, title, detail, recommendationType, recommendationDetail, businessReferenceKey, scoreImpact);
  }

  private IssueDraft referenceIssue(UUID documentId, String label, String value, String detail, String fieldKey) {
    return issue(
        "BUSINESS_REFERENCE",
        "INVALID_REFERENCE_VALUE",
        KnowledgeQualityIssueSeverity.MEDIUM,
        documentId,
        "DOCUMENT",
        label + " is missing or invalid" + (value == null || value.isBlank() ? "." : ": " + value),
        detail,
        "BUSINESS_REFERENCE_CORRECTION",
        "Correct the " + label + " value and revalidate the customer knowledge.",
        fieldKey,
        new BigDecimal("0.0600"));
  }

  private Map<String, String> metadataMap(List<MetadataValue> metadataValues) {
    Map<String, String> values = new LinkedHashMap<>();
    for (MetadataValue metadataValue : metadataValues) {
      values.put(metadataValue.getField().getFieldKey(), metadataValue.getTextValue());
    }
    return values;
  }

  private String firstValue(Map<String, String> metadata, Set<String> acceptedKeys) {
    return metadata.entrySet().stream()
        .filter(entry -> acceptedKeys.contains(normalizedKey(entry.getKey())))
        .map(Map.Entry::getValue)
        .filter(Objects::nonNull)
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private boolean matchesReference(String value, String pattern) {
    return value != null && !value.isBlank() && value.trim().matches(pattern);
  }

  private boolean isDate(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      LocalDate.parse(value.trim());
      return true;
    } catch (DateTimeParseException exception) {
      return false;
    }
  }

  private String normalizedKey(String value) {
    return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
  }

  private String preferredFieldValue(DocumentProcessingField field) {
    if (field.getApprovedValue() != null && !field.getApprovedValue().isBlank()) {
      return field.getApprovedValue();
    }
    if (field.getCorrectedValue() != null && !field.getCorrectedValue().isBlank()) {
      return field.getCorrectedValue();
    }
    return field.getExtractedValue();
  }

  private String humanizeKey(String key) {
    String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  private void requireConfirmed(Boolean confirmed) {
    if (!Boolean.TRUE.equals(confirmed)) {
      throw new IllegalArgumentException("Bulk quality actions require explicit confirmation.");
    }
  }

  private Client requireClient(UUID clientId) {
    return clientRepository.findById(clientId)
        .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
  }

  private record EvaluationContext(
      Client client,
      List<Document> documents,
      Map<UUID, DocumentVersion> currentVersions,
      Map<UUID, List<DocumentVersion>> versionsByDocument,
      Map<UUID, List<MetadataValue>> metadataByDocument,
      Map<UUID, DocumentProcessingJob> jobsByDocument,
      Map<UUID, List<DocumentProcessingField>> fieldsByJob,
      List<Email> emails,
      List<Note> notes,
      List<ReviewQueueItem> reviewItems,
      List<EmbeddingChunk> embeddingChunks) {
  }

  private record IssueDraft(
      String category,
      String issueType,
      KnowledgeQualityIssueSeverity severity,
      String sourceType,
      UUID sourceId,
      String title,
      String detail,
      String recommendationType,
      String recommendationDetail,
      String businessReferenceKey,
      BigDecimal scoreImpact) {
  }
}
