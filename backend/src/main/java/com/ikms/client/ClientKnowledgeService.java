package com.ikms.client;

import com.ikms.ai.AiConversation;
import com.ikms.ai.AiConversationRepository;
import com.ikms.ai.EmbeddingChunk;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.Email;
import com.ikms.email.EmailRepository;
import com.ikms.note.Note;
import com.ikms.note.NoteRepository;
import com.ikms.note.NoteStatus;
import com.ikms.review.ReviewQueueItem;
import com.ikms.review.ReviewQueueItemType;
import com.ikms.review.ReviewQueueRepository;
import com.ikms.security.PiiMaskingService;
import com.ikms.security.domain.Permission;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientKnowledgeService {

  private static final Set<String> BUSINESS_REFERENCE_KEYS = Set.of(
      "policy_number",
      "claim_number",
      "insurer",
      "policy_type",
      "effective_date",
      "expiry_date",
      "renewal_date",
      "broker_reference",
      "external_reference");

  private final ClientService clientService;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final ReviewQueueRepository reviewQueueRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final AiConversationRepository aiConversationRepository;
  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final JdbcTemplate jdbcTemplate;
  private final PiiMaskingService piiMaskingService;
  private final AuditService auditService;

  public ClientKnowledgeService(
      ClientService clientService,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      ReviewQueueRepository reviewQueueRepository,
      MetadataValueRepository metadataValueRepository,
      AiConversationRepository aiConversationRepository,
      EmbeddingChunkRepository embeddingChunkRepository,
      JdbcTemplate jdbcTemplate,
      PiiMaskingService piiMaskingService,
      AuditService auditService) {
    this.clientService = clientService;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.reviewQueueRepository = reviewQueueRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.aiConversationRepository = aiConversationRepository;
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.piiMaskingService = piiMaskingService;
    this.auditService = auditService;
  }

  public ClientContracts.CustomerKnowledgeTimelinePageResponse timeline(
      UUID clientId,
      TimelineQuery query,
      UUID actorUserId,
      Set<Permission> permissions) {
    clientService.requireClient(clientId);
    Map<String, List<MetadataValue>> metadataCache = new HashMap<>();
    List<TimelineEventRecord> events = new ArrayList<>();

    addDocumentEvents(clientId, permissions, metadataCache, events);
    addEmailEvents(clientId, permissions, metadataCache, events);
    addNoteEvents(clientId, permissions, events);
    addReviewEvents(clientId, permissions, metadataCache, events);
    addMetadataEvents(clientId, permissions, metadataCache, events);
    addConversationEvents(clientId, events);

    List<TimelineEventRecord> filtered = events.stream()
        .filter(event -> matchesTimelineQuery(event, query))
        .sorted(timelineComparator())
        .toList();

    int startIndex = resolveTimelineStartIndex(filtered, query.cursor());
    int limit = sanitizeLimit(query.limit(), 50, 25);
    int endExclusive = Math.min(filtered.size(), startIndex + limit);
    List<TimelineEventRecord> page = filtered.subList(startIndex, endExclusive);
    boolean hasMore = endExclusive < filtered.size();
    String nextCursor = hasMore && !page.isEmpty() ? encodeCursor(page.get(page.size() - 1).eventId()) : null;

    auditService.write(new AuditEvent(
        Instant.now(),
        "KNOWLEDGE",
        "CUSTOMER_TIMELINE_VIEWED",
        AuditOutcome.SUCCESS,
        actorUserId,
        clientId,
        "CustomerKnowledgeTimeline",
        clientId.toString(),
        false,
        Map.of(
            "eventCount", Integer.toString(page.size()),
            "hasMore", Boolean.toString(hasMore))));

    return new ClientContracts.CustomerKnowledgeTimelinePageResponse(
        page.stream().map(TimelineEventRecord::toResponse).toList(),
        nextCursor,
        hasMore,
        new ClientContracts.CustomerKnowledgeTimelineFiltersResponse(
            query.query(),
            query.from(),
            query.to(),
            query.sourceType(),
            query.eventType(),
            query.documentType(),
            query.reviewStatus(),
            query.policyNumber(),
            query.claimNumber(),
            query.insurer(),
            query.actor(),
            "DESC",
            limit));
  }

  public ClientContracts.RelatedKnowledgeResponse relatedForClient(
      UUID clientId,
      int limit,
      UUID actorUserId,
      Set<Permission> permissions) {
    clientService.requireClient(clientId);
    List<RelatedKnowledgeRecord> links = buildRelatedKnowledge(clientId, null, permissions, sanitizeLimit(limit, 40, 20));
    auditService.write(new AuditEvent(
        Instant.now(),
        "KNOWLEDGE",
        "CUSTOMER_RELATED_KNOWLEDGE_VIEWED",
        AuditOutcome.SUCCESS,
        actorUserId,
        clientId,
        "CustomerRelatedKnowledge",
        clientId.toString(),
        false,
        Map.of("relationshipCount", Integer.toString(links.size()))));
    return new ClientContracts.RelatedKnowledgeResponse(
        clientId,
        "CUSTOMER",
        clientId,
        links.stream().map(RelatedKnowledgeRecord::toResponse).toList(),
        null);
  }

  public ClientContracts.RelatedKnowledgeResponse relatedForSource(
      String sourceType,
      UUID sourceId,
      int limit,
      UUID actorUserId,
      Set<Permission> permissions) {
    SourceContext sourceContext = resolveSourceContext(sourceType, sourceId);
    List<RelatedKnowledgeRecord> links = buildRelatedKnowledge(
        sourceContext.clientId(),
        sourceContext.sourceRef(),
        permissions,
        sanitizeLimit(limit, 30, 12));
    auditService.write(new AuditEvent(
        Instant.now(),
        "KNOWLEDGE",
        "SOURCE_RELATED_KNOWLEDGE_VIEWED",
        AuditOutcome.SUCCESS,
        actorUserId,
        sourceContext.clientId(),
        "KnowledgeSource",
        sourceType + ":" + sourceId,
        false,
        Map.of("relationshipCount", Integer.toString(links.size()))));
    return new ClientContracts.RelatedKnowledgeResponse(
        sourceContext.clientId(),
        sourceContext.sourceRef().sourceType(),
        sourceContext.sourceRef().sourceId(),
        links.stream().map(RelatedKnowledgeRecord::toResponse).toList(),
        null);
  }

  public List<ClientContracts.DocumentVersionSummaryResponse> listDocumentVersions(UUID documentId) {
    return documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(documentId).stream()
        .map(version -> new ClientContracts.DocumentVersionSummaryResponse(
            version.getId(),
            version.getDocument().getId(),
            version.getVersionNumber(),
            version.getFileName(),
            version.getMimeType(),
            version.getRedactionStatus().name(),
            version.isCurrent(),
            version.getFileHash(),
            version.getCreatedAt(),
            version.getCreatedBy()))
        .toList();
  }

  private void addDocumentEvents(
      UUID clientId,
      Set<Permission> permissions,
      Map<String, List<MetadataValue>> metadataCache,
      List<TimelineEventRecord> events) {
    for (Document document : documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
      List<MetadataValue> metadata = metadataFor("DOCUMENT", document.getId(), metadataCache);
      List<ClientContracts.BusinessReferenceFieldResponse> references = toBusinessReferenceFields(metadata, permissions);
      DocumentVersion currentVersion = documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).orElse(null);
      String eventType = document.getParentEmail() != null ? "DOCUMENT_RECEIVED" : "DOCUMENT_CREATED";
      events.add(new TimelineEventRecord(
          "document-" + document.getId(),
          clientId,
          eventType,
          "DOCUMENT",
          document.getId(),
          currentVersion == null ? null : currentVersion.getId(),
          document.getTitle(),
          document.getSource().name() + " document recorded in customer knowledge.",
          document.getCreatedAt(),
          document.getCreatedAt(),
          "System",
          document.getDocumentTypeId() == null ? document.getSource().name() : document.getDocumentTypeId().toString(),
          references,
          document.getReviewStatus().name(),
          List.of(toEvidenceReference(
              "DOCUMENT",
              document.getId(),
              currentVersion == null ? null : currentVersion.getId(),
              document.getTitle(),
              "Primary document source",
              null,
              "document")),
          List.of("OPEN_SOURCE"),
          "AVAILABLE",
          document.getId().toString()));

      for (DocumentVersion version : documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(document.getId())) {
        if (version.getVersionNumber() <= 1) {
          continue;
        }
        events.add(new TimelineEventRecord(
            "document-version-" + version.getId(),
            clientId,
            "DOCUMENT_VERSION_ADDED",
            "DOCUMENT",
            document.getId(),
            version.getId(),
            document.getTitle() + " v" + version.getVersionNumber(),
            "Document version " + version.getVersionNumber() + " added to customer knowledge.",
            version.getCreatedAt(),
            version.getCreatedAt(),
            version.getCreatedBy() == null ? null : version.getCreatedBy().toString(),
            document.getDocumentTypeId() == null ? document.getSource().name() : document.getDocumentTypeId().toString(),
            references,
            version.isCurrent() ? "CURRENT" : "HISTORICAL",
            List.of(toEvidenceReference(
                "DOCUMENT",
                document.getId(),
                version.getId(),
                document.getTitle(),
                "Document version " + version.getVersionNumber(),
                null,
                "document-version")),
            List.of("OPEN_SOURCE"),
            "AVAILABLE",
            version.getId().toString()));
      }
    }
  }

  private void addEmailEvents(
      UUID clientId,
      Set<Permission> permissions,
      Map<String, List<MetadataValue>> metadataCache,
      List<TimelineEventRecord> events) {
    for (Email email : emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId)) {
      List<MetadataValue> metadata = metadataFor("EMAIL", email.getId(), metadataCache);
      String sender = piiMaskingService.canViewPii(permissions) ? email.getSender() : piiMaskingService.maskEmail(email.getSender());
      events.add(new TimelineEventRecord(
          "email-" + email.getId(),
          clientId,
          "EMAIL_RECEIVED",
          "EMAIL",
          email.getId(),
          null,
          email.getSubject(),
          "Email received from " + sender + ".",
          email.getReceivedAt(),
          email.getCreatedAt(),
          sender,
          null,
          toBusinessReferenceFields(metadata, permissions),
          email.getReviewStatus().name(),
          List.of(toEvidenceReference("EMAIL", email.getId(), null, email.getSubject(), sender, null, "email")),
          List.of("OPEN_SOURCE"),
          "AVAILABLE",
          email.getId().toString()));
    }
  }

  private void addNoteEvents(
      UUID clientId,
      Set<Permission> permissions,
      List<TimelineEventRecord> events) {
    for (Note note : noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(clientId, NoteStatus.ACTIVE)) {
      String detail = piiMaskingService.trimFreeText(note.getNoteText(), permissions);
      events.add(new TimelineEventRecord(
          "note-created-" + note.getId(),
          clientId,
          "NOTE_CREATED",
          "NOTE",
          note.getId(),
          null,
          "Customer note",
          detail,
          note.getCreatedAt(),
          note.getCreatedAt(),
          note.getCreatedBy() == null ? null : note.getCreatedBy().toString(),
          null,
          List.of(),
          note.getStatus().name(),
          List.of(toEvidenceReference("NOTE", note.getId(), null, "Customer note", "Note created", null, "note")),
          List.of("OPEN_SOURCE"),
          "AVAILABLE",
          note.getId().toString()));
      if (note.getUpdatedAt() != null && note.getUpdatedAt().isAfter(note.getCreatedAt())) {
        events.add(new TimelineEventRecord(
            "note-updated-" + note.getId(),
            clientId,
            "NOTE_UPDATED",
            "NOTE",
            note.getId(),
            null,
            "Customer note updated",
            detail,
            note.getUpdatedAt(),
            note.getUpdatedAt(),
            note.getUpdatedBy() == null ? null : note.getUpdatedBy().toString(),
            null,
            List.of(),
            note.getStatus().name(),
            List.of(toEvidenceReference("NOTE", note.getId(), null, "Customer note", "Note updated", null, "note")),
            List.of("OPEN_SOURCE"),
            "AVAILABLE",
            note.getId().toString()));
      }
    }
  }

  private void addReviewEvents(
      UUID clientId,
      Set<Permission> permissions,
      Map<String, List<MetadataValue>> metadataCache,
      List<TimelineEventRecord> events) {
    for (ReviewQueueItem item : reviewQueueRepository.findAll()) {
      RelatedSourceRef linked = reviewSourceFor(item, clientId);
      if (linked == null) {
        continue;
      }
      List<ClientContracts.BusinessReferenceFieldResponse> references = linked.sourceType().equals("DOCUMENT")
          ? toBusinessReferenceFields(metadataFor("DOCUMENT", linked.sourceId(), metadataCache), permissions)
          : List.of();
      events.add(new TimelineEventRecord(
          "review-created-" + item.getId(),
          clientId,
          "REVIEW_CREATED",
          linked.sourceType(),
          linked.sourceId(),
          null,
          linked.title(),
          "Review created for " + item.getReason().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".",
          item.getCreatedAt(),
          item.getCreatedAt(),
          null,
          null,
          references,
          item.getStatus().name(),
          List.of(toEvidenceReference(linked.sourceType(), linked.sourceId(), null, linked.title(), "Review source", null, "review")),
          List.of("OPEN_REVIEW"),
          "AVAILABLE",
          item.getId().toString()));
      if (item.getResolvedAt() != null && "RESOLVED".equals(item.getStatus().name())) {
        events.add(new TimelineEventRecord(
            "review-approved-" + item.getId(),
            clientId,
            "REVIEW_APPROVED",
            linked.sourceType(),
            linked.sourceId(),
            null,
            linked.title(),
            "Review approved.",
            item.getResolvedAt(),
            item.getResolvedAt(),
            null,
            null,
            references,
            item.getStatus().name(),
            List.of(toEvidenceReference(linked.sourceType(), linked.sourceId(), null, linked.title(), "Review approved", null, "review")),
            List.of("OPEN_REVIEW"),
            "AVAILABLE",
            item.getId().toString()));
      } else if (item.getResolvedAt() != null && "REJECTED".equals(item.getStatus().name())) {
        events.add(new TimelineEventRecord(
            "review-rejected-" + item.getId(),
            clientId,
            "REVIEW_REJECTED",
            linked.sourceType(),
            linked.sourceId(),
            null,
            linked.title(),
            "Review rejected.",
            item.getResolvedAt(),
            item.getResolvedAt(),
            null,
            null,
            references,
            item.getStatus().name(),
            List.of(toEvidenceReference(linked.sourceType(), linked.sourceId(), null, linked.title(), "Review rejected", null, "review")),
            List.of("OPEN_REVIEW"),
            "AVAILABLE",
            item.getId().toString()));
      }
    }
  }

  private void addMetadataEvents(
      UUID clientId,
      Set<Permission> permissions,
      Map<String, List<MetadataValue>> metadataCache,
      List<TimelineEventRecord> events) {
    for (Document document : documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
      for (MetadataValue value : metadataFor("DOCUMENT", document.getId(), metadataCache)) {
        String normalizedKey = normalizeFieldKey(value.getField());
        if (!BUSINESS_REFERENCE_KEYS.contains(normalizedKey)) {
          continue;
        }
        String label = businessReferenceLabel(normalizedKey);
        String maskedValue = maskMetadataValue(value, permissions);
        events.add(new TimelineEventRecord(
            "metadata-created-" + value.getId(),
            clientId,
            "BUSINESS_REFERENCE_EXTRACTED",
            "DOCUMENT",
            document.getId(),
            null,
            label,
            label + " extracted as " + maskedValue + ".",
            value.getCreatedAt(),
            value.getCreatedAt(),
            null,
            document.getDocumentTypeId() == null ? document.getSource().name() : document.getDocumentTypeId().toString(),
            List.of(new ClientContracts.BusinessReferenceFieldResponse(normalizedKey, label, maskedValue)),
            "RECORDED",
            List.of(toEvidenceReference("DOCUMENT", document.getId(), null, document.getTitle(), label, null, "metadata")),
            List.of("OPEN_SOURCE"),
            "AVAILABLE",
            value.getId().toString()));
        if (value.getUpdatedAt() != null && value.getUpdatedAt().isAfter(value.getCreatedAt())) {
          events.add(new TimelineEventRecord(
              "metadata-updated-" + value.getId(),
              clientId,
              "BUSINESS_REFERENCE_CORRECTED",
              "DOCUMENT",
              document.getId(),
              null,
              label,
              label + " corrected to " + maskedValue + ".",
              value.getUpdatedAt(),
              value.getUpdatedAt(),
              null,
              document.getDocumentTypeId() == null ? document.getSource().name() : document.getDocumentTypeId().toString(),
              List.of(new ClientContracts.BusinessReferenceFieldResponse(normalizedKey, label, maskedValue)),
              "CORRECTED",
              List.of(toEvidenceReference("DOCUMENT", document.getId(), null, document.getTitle(), label, null, "metadata")),
              List.of("OPEN_SOURCE"),
              "AVAILABLE",
              value.getId().toString()));
        }
      }
    }
  }

  private void addConversationEvents(UUID clientId, List<TimelineEventRecord> events) {
    for (AiConversation conversation : aiConversationRepository.findByClientIdOrderByCreatedAtDesc(clientId)) {
      events.add(new TimelineEventRecord(
          "conversation-" + conversation.getId(),
          clientId,
          "AI_CONVERSATION_CREATED",
          "AI_CONVERSATION",
          conversation.getId(),
          null,
          conversation.getTitle() == null || conversation.getTitle().isBlank()
              ? conversation.getOperationType()
              : conversation.getTitle(),
          conversation.getOperationType() + " conversation created.",
          conversation.getCreatedAt(),
          conversation.getCreatedAt(),
          conversation.getActorUserId() == null ? null : conversation.getActorUserId().toString(),
          null,
          List.of(),
          conversation.getStatus(),
          List.of(toEvidenceReference(
              "AI_CONVERSATION",
              conversation.getId(),
              null,
              conversation.getTitle() == null || conversation.getTitle().isBlank()
                  ? conversation.getOperationType()
                  : conversation.getTitle(),
              "Conversation context",
              null,
              "conversation")),
          List.of("OPEN_ASSISTANT"),
          "AVAILABLE",
          conversation.getId().toString()));
    }
  }

  private boolean matchesTimelineQuery(TimelineEventRecord event, TimelineQuery query) {
    if (query.sourceType() != null && !query.sourceType().isBlank()
        && !event.sourceType().equalsIgnoreCase(query.sourceType())) {
      return false;
    }
    if (query.eventType() != null && !query.eventType().isBlank()
        && !event.eventType().equalsIgnoreCase(query.eventType())) {
      return false;
    }
    if (query.documentType() != null && !query.documentType().isBlank()
        && !query.documentType().equalsIgnoreCase(nullSafe(event.documentType()))) {
      return false;
    }
    if (query.reviewStatus() != null && !query.reviewStatus().isBlank()
        && !query.reviewStatus().equalsIgnoreCase(nullSafe(event.status()))) {
      return false;
    }
    if (query.actor() != null && !query.actor().isBlank()
        && !nullSafe(event.actor()).toLowerCase(Locale.ROOT).contains(query.actor().trim().toLowerCase(Locale.ROOT))) {
      return false;
    }
    if (!matchesTimelineDate(query.from(), query.to(), event.occurredAt() == null ? event.recordedAt() : event.occurredAt())) {
      return false;
    }
    if (!matchesReferences(event.businessReferenceFields(), query)) {
      return false;
    }
    if (query.query() != null && !query.query().isBlank()) {
      String haystack = String.join(" ",
          nullSafe(event.title()),
          nullSafe(event.summary()),
          nullSafe(event.actor()),
          nullSafe(event.eventType()),
          nullSafe(event.status())).toLowerCase(Locale.ROOT);
      if (!haystack.contains(query.query().trim().toLowerCase(Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  private boolean matchesReferences(
      List<ClientContracts.BusinessReferenceFieldResponse> fields,
      TimelineQuery query) {
    Map<String, String> values = new HashMap<>();
    for (ClientContracts.BusinessReferenceFieldResponse field : fields) {
      values.put(field.key(), field.value());
    }
    return matchesReferenceValue(values.get("policy_number"), query.policyNumber())
        && matchesReferenceValue(values.get("claim_number"), query.claimNumber())
        && matchesReferenceValue(values.get("insurer"), query.insurer());
  }

  private boolean matchesReferenceValue(String actual, String requested) {
    return requested == null
        || requested.isBlank()
        || nullSafe(actual).toLowerCase(Locale.ROOT).contains(requested.trim().toLowerCase(Locale.ROOT));
  }

  private boolean matchesTimelineDate(String from, String to, Instant value) {
    if (value == null) {
      return true;
    }
    try {
      if (from != null && !from.isBlank() && value.isBefore(java.time.LocalDate.parse(from).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())) {
        return false;
      }
      if (to != null && !to.isBlank()
          && value.isAfter(java.time.LocalDate.parse(to).plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).minusNanos(1).toInstant())) {
        return false;
      }
      return true;
    } catch (Exception ignored) {
      return true;
    }
  }

  private Comparator<TimelineEventRecord> timelineComparator() {
    return Comparator.comparing(
            (TimelineEventRecord event) -> event.occurredAt() == null ? event.recordedAt() : event.occurredAt(),
            Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(TimelineEventRecord::recordedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(TimelineEventRecord::eventId, Comparator.reverseOrder());
  }

  private int resolveTimelineStartIndex(List<TimelineEventRecord> events, String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return 0;
    }
    String eventId = decodeCursor(cursor);
    for (int index = 0; index < events.size(); index++) {
      if (events.get(index).eventId().equals(eventId)) {
        return index + 1;
      }
    }
    return 0;
  }

  private List<RelatedKnowledgeRecord> buildRelatedKnowledge(
      UUID clientId,
      RelatedSourceRef requestedSource,
      Set<Permission> permissions,
      int limit) {
    Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources = loadKnowledgeSources(clientId, permissions);
    List<RelatedKnowledgeRecord> links = new ArrayList<>();
    addEmailAttachmentLinks(sources, links);
    addVersionLinks(sources, links);
    addBusinessReferenceLinks(sources, links);
    addThreadLinks(sources, links);
    addDuplicateLinks(sources, links);
    addSimilarityLinks(clientId, sources, links);

    List<RelatedKnowledgeRecord> filtered = links.stream()
        .map(link -> requestedSource == null ? link : reorient(link, requestedSource))
        .filter(Objects::nonNull)
        .distinct()
        .sorted(Comparator.comparing(RelatedKnowledgeRecord::score, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(RelatedKnowledgeRecord::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(RelatedKnowledgeRecord::relationshipId))
        .limit(limit)
        .toList();
    return filtered;
  }

  private Map<RelatedSourceRef, KnowledgeSourceSnapshot> loadKnowledgeSources(UUID clientId, Set<Permission> permissions) {
    Map<String, List<MetadataValue>> metadataCache = new HashMap<>();
    Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources = new LinkedHashMap<>();
    for (Document document : documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
      List<MetadataValue> metadata = metadataFor("DOCUMENT", document.getId(), metadataCache);
      DocumentVersion currentVersion = documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).orElse(null);
      sources.put(
          new RelatedSourceRef("DOCUMENT", document.getId(), document.getTitle()),
          new KnowledgeSourceSnapshot(
              clientId,
              "DOCUMENT",
              document.getId(),
              document.getTitle(),
              document.getCreatedAt(),
              businessReferenceMap(metadata, permissions),
              currentVersion == null ? null : currentVersion.getFileHash(),
              document.getParentEmail() == null ? null : document.getParentEmail().getId(),
              null,
              document.getDocumentTypeId() == null ? null : document.getDocumentTypeId().toString(),
              document.getId(),
              currentVersion == null ? null : currentVersion.getId(),
              currentVersion == null ? null : currentVersion.getVersionNumber()));
      for (DocumentVersion version : documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(document.getId())) {
        sources.put(
            new RelatedSourceRef("DOCUMENT_VERSION", version.getId(), document.getTitle() + " v" + version.getVersionNumber()),
            new KnowledgeSourceSnapshot(
                clientId,
                "DOCUMENT_VERSION",
                version.getId(),
                document.getTitle() + " v" + version.getVersionNumber(),
                version.getCreatedAt(),
                businessReferenceMap(metadata, permissions),
                version.getFileHash(),
                document.getParentEmail() == null ? null : document.getParentEmail().getId(),
                null,
                document.getDocumentTypeId() == null ? null : document.getDocumentTypeId().toString(),
                document.getId(),
                version.getId(),
                version.getVersionNumber()));
      }
    }
    for (Email email : emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId)) {
      sources.put(
          new RelatedSourceRef("EMAIL", email.getId(), email.getSubject()),
          new KnowledgeSourceSnapshot(
              clientId,
              "EMAIL",
              email.getId(),
              email.getSubject(),
              email.getReceivedAt(),
              businessReferenceMap(metadataFor("EMAIL", email.getId(), metadataCache), permissions),
              firstContentHash(clientId, "EMAIL", email.getId()),
              null,
              email.getThreadId(),
              null,
              null,
              null,
              null));
    }
    for (Note note : noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(clientId, NoteStatus.ACTIVE)) {
      sources.put(
          new RelatedSourceRef("NOTE", note.getId(), "Customer note"),
          new KnowledgeSourceSnapshot(
              clientId,
              "NOTE",
              note.getId(),
              "Customer note",
              note.getUpdatedAt(),
              businessReferenceMap(metadataFor("NOTE", note.getId(), metadataCache), permissions),
              firstContentHash(clientId, "NOTE", note.getId()),
              null,
              null,
              null,
              null,
              null,
              null));
    }
    return sources;
  }

  private void addEmailAttachmentLinks(
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links) {
    for (KnowledgeSourceSnapshot source : sources.values()) {
      if (!"DOCUMENT".equals(source.sourceType()) || source.parentEmailId() == null) {
        continue;
      }
      KnowledgeSourceSnapshot email = findSource(sources, "EMAIL", source.parentEmailId());
      if (email == null) {
        continue;
      }
      links.add(buildLink(
          email,
          source,
          "EMAIL_ATTACHMENT",
          0.99d,
          "Email includes this document as related knowledge.",
          Map.of("emailSubject", email.title(), "documentTitle", source.title()),
          "EMAIL_ATTACHMENT",
          false));
    }
  }

  private void addVersionLinks(
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links) {
    Map<UUID, List<KnowledgeSourceSnapshot>> versionsByDocument = new LinkedHashMap<>();
    for (KnowledgeSourceSnapshot snapshot : sources.values()) {
      if (!"DOCUMENT_VERSION".equals(snapshot.sourceType()) || snapshot.documentId() == null) {
        continue;
      }
      versionsByDocument.computeIfAbsent(snapshot.documentId(), ignored -> new ArrayList<>()).add(snapshot);
    }
    for (List<KnowledgeSourceSnapshot> versions : versionsByDocument.values()) {
      versions.sort(Comparator.comparing(KnowledgeSourceSnapshot::versionNumber, Comparator.nullsLast(Comparator.naturalOrder())));
      for (int index = 0; index < versions.size() - 1; index++) {
        KnowledgeSourceSnapshot previous = versions.get(index);
        KnowledgeSourceSnapshot next = versions.get(index + 1);
        links.add(buildLink(
            previous,
            next,
            "VERSION_LINEAGE",
            0.98d,
            "These document versions belong to the same document lineage.",
            Map.of(
                "documentId", String.valueOf(previous.documentId()),
                "versionPair", previous.versionNumber() + "→" + next.versionNumber()),
            "VERSION_LINEAGE",
            false));
      }
    }
  }

  private void addBusinessReferenceLinks(
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links) {
    addReferenceLinksByKey(sources.values(), links, "policy_number", "SAME_POLICY_REFERENCE", "Same Policy Reference", 0.93d);
    addReferenceLinksByKey(sources.values(), links, "claim_number", "SAME_CLAIM_REFERENCE", "Same Claim Reference", 0.92d);
    addReferenceLinksByKey(sources.values(), links, "insurer", "SAME_INSURER", "Same Insurer", 0.84d);
    addReferenceLinksByKey(sources.values(), links, "external_reference", "SAME_EXTERNAL_REFERENCE", "Same External Reference", 0.9d);
    addReferenceLinksByKey(sources.values(), links, "broker_reference", "SAME_BROKER_REFERENCE", "Same Broker Reference", 0.88d);
  }

  private void addReferenceLinksByKey(
      Collection<KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links,
      String key,
      String relationshipType,
      String explanationPrefix,
      double score) {
    Map<String, List<KnowledgeSourceSnapshot>> grouped = new LinkedHashMap<>();
    for (KnowledgeSourceSnapshot source : sources) {
      String value = source.businessReferenceFields().get(key);
      if (value == null || value.isBlank()) {
        continue;
      }
      grouped.computeIfAbsent(value.trim().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(source);
    }
    for (List<KnowledgeSourceSnapshot> groupedSources : grouped.values()) {
      for (int leftIndex = 0; leftIndex < groupedSources.size(); leftIndex++) {
        for (int rightIndex = leftIndex + 1; rightIndex < groupedSources.size(); rightIndex++) {
          KnowledgeSourceSnapshot left = groupedSources.get(leftIndex);
          KnowledgeSourceSnapshot right = groupedSources.get(rightIndex);
          if (left.id().equals(right.id()) && left.sourceType().equals(right.sourceType())) {
            continue;
          }
          String value = left.businessReferenceFields().get(key);
          links.add(buildLink(
              left,
              right,
              relationshipType,
              score,
              explanationPrefix + ": " + value,
              Map.of(key, value),
              "BUSINESS_REFERENCE_MATCH",
              false));
        }
      }
    }
  }

  private void addThreadLinks(
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links) {
    Map<String, List<KnowledgeSourceSnapshot>> grouped = new LinkedHashMap<>();
    for (KnowledgeSourceSnapshot source : sources.values()) {
      if (!"EMAIL".equals(source.sourceType()) || source.threadId() == null || source.threadId().isBlank()) {
        continue;
      }
      grouped.computeIfAbsent(source.threadId(), ignored -> new ArrayList<>()).add(source);
    }
    for (List<KnowledgeSourceSnapshot> threadSources : grouped.values()) {
      for (int leftIndex = 0; leftIndex < threadSources.size(); leftIndex++) {
        for (int rightIndex = leftIndex + 1; rightIndex < threadSources.size(); rightIndex++) {
          KnowledgeSourceSnapshot left = threadSources.get(leftIndex);
          KnowledgeSourceSnapshot right = threadSources.get(rightIndex);
          links.add(buildLink(
              left,
              right,
              "SAME_EMAIL_THREAD",
              0.85d,
              "These emails belong to the same correspondence thread.",
              Map.of("threadId", left.threadId()),
              "DETERMINISTIC",
              false));
        }
      }
    }
  }

  private void addDuplicateLinks(
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links) {
    Map<String, List<KnowledgeSourceSnapshot>> grouped = new LinkedHashMap<>();
    for (KnowledgeSourceSnapshot source : sources.values()) {
      if (source.contentHash() == null || source.contentHash().isBlank()) {
        continue;
      }
      grouped.computeIfAbsent(source.contentHash(), ignored -> new ArrayList<>()).add(source);
    }
    for (List<KnowledgeSourceSnapshot> duplicates : grouped.values()) {
      if (duplicates.size() < 2) {
        continue;
      }
      for (int leftIndex = 0; leftIndex < duplicates.size(); leftIndex++) {
        for (int rightIndex = leftIndex + 1; rightIndex < duplicates.size(); rightIndex++) {
          KnowledgeSourceSnapshot left = duplicates.get(leftIndex);
          KnowledgeSourceSnapshot right = duplicates.get(rightIndex);
          if (left.documentId() != null && left.documentId().equals(right.documentId())) {
            continue;
          }
          links.add(buildLink(
              left,
              right,
              "EXACT_DUPLICATE",
              0.97d,
              "These knowledge sources share the same indexed content hash.",
              Map.of("contentHash", left.contentHash()),
              "DETERMINISTIC",
              false));
        }
      }
    }
  }

  private void addSimilarityLinks(
      UUID clientId,
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      List<RelatedKnowledgeRecord> links) {
    Set<String> existing = new HashSet<>();
    for (RelatedKnowledgeRecord link : links) {
      existing.add(link.sourceType() + ":" + link.sourceId() + "->" + link.relatedSourceType() + ":" + link.relatedSourceId());
      existing.add(link.relatedSourceType() + ":" + link.relatedSourceId() + "->" + link.sourceType() + ":" + link.sourceId());
    }
    for (KnowledgeSourceSnapshot source : sources.values()) {
      if (!Set.of("DOCUMENT", "EMAIL", "NOTE").contains(source.sourceType())) {
        continue;
      }
      for (VectorMatch match : findSimilarSources(clientId, source.sourceType(), source.id())) {
        KnowledgeSourceSnapshot related = findSource(sources, match.relatedSourceType(), match.relatedSourceId());
        if (related == null) {
          continue;
        }
        String key = source.sourceType() + ":" + source.id() + "->" + related.sourceType() + ":" + related.id();
        if (existing.contains(key)) {
          continue;
        }
        existing.add(key);
        existing.add(related.sourceType() + ":" + related.id() + "->" + source.sourceType() + ":" + source.id());
        links.add(buildLink(
            source,
            related,
            "CONTENT_SIMILARITY",
            match.score(),
            "Related by indexed content similarity. Review the underlying sources before treating this as confirmed.",
            Map.of("similarity", String.format(Locale.ROOT, "%.3f", match.score())),
            "CONTENT_SIMILARITY",
            true));
      }
    }
  }

  private List<VectorMatch> findSimilarSources(UUID clientId, String sourceType, UUID sourceId) {
    List<EmbeddingChunk> chunks = embeddingChunkRepository.findByClientIdAndSourceTypeAndSourceIdOrderByChunkIndexAsc(clientId, sourceType, sourceId);
    if (chunks.isEmpty() || chunks.get(0).getEmbeddingVector() == null || chunks.get(0).getEmbeddingVector().isBlank()) {
      return List.of();
    }
    try {
      return jdbcTemplate.query(
          """
              select source_type, source_id,
                     min(cast(embedding_vector as vector) <=> cast(? as vector)) as distance
              from embedding_chunk
              where client_id = ?
                and embedding_vector is not null
                and not (source_type = ? and source_id = ?)
              group by source_type, source_id
              order by min(cast(embedding_vector as vector) <=> cast(? as vector)) asc
              limit 4
              """,
          (resultSet, rowNum) -> new VectorMatch(
              resultSet.getString("source_type"),
              UUID.fromString(resultSet.getString("source_id")),
              Math.max(0d, 1d - Math.min(resultSet.getDouble("distance"), 1d))),
          chunks.get(0).getEmbeddingVector(),
          clientId,
          sourceType,
          sourceId,
          chunks.get(0).getEmbeddingVector()).stream()
          .filter(match -> match.score() >= 0.55d)
          .toList();
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private RelatedKnowledgeRecord reorient(RelatedKnowledgeRecord link, RelatedSourceRef requestedSource) {
    if (link.sourceType().equalsIgnoreCase(requestedSource.sourceType()) && link.sourceId().equals(requestedSource.sourceId())) {
      return link;
    }
    if (link.relatedSourceType().equalsIgnoreCase(requestedSource.sourceType()) && link.relatedSourceId().equals(requestedSource.sourceId())) {
      return link.reversed();
    }
    return null;
  }

  private RelatedKnowledgeRecord buildLink(
      KnowledgeSourceSnapshot source,
      KnowledgeSourceSnapshot related,
      String relationshipType,
      double score,
      String explanation,
      Map<String, String> supportingFields,
      String derivationType,
      boolean inferred) {
    return new RelatedKnowledgeRecord(
        stableRelationshipId(source, related, relationshipType),
        source.sourceType(),
        source.id(),
        source.title(),
        related.sourceType(),
        related.id(),
        related.title(),
        relationshipType,
        score,
        explanation,
        supportingFields,
        List.of(toEvidenceReference(
            source.sourceType(),
            source.id(),
            source.versionId(),
            source.title(),
            explanation,
            null,
            derivationType.toLowerCase(Locale.ROOT))),
        derivationType,
        source.occurredAt() == null ? related.occurredAt() : source.occurredAt(),
        inferred,
        source.clientId());
  }

  private String stableRelationshipId(KnowledgeSourceSnapshot source, KnowledgeSourceSnapshot related, String relationshipType) {
    String left = source.sourceType() + ":" + source.id();
    String right = related.sourceType() + ":" + related.id();
    String ordered = left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString((relationshipType + "|" + ordered).getBytes(StandardCharsets.UTF_8));
  }

  private KnowledgeSourceSnapshot findSource(
      Map<RelatedSourceRef, KnowledgeSourceSnapshot> sources,
      String sourceType,
      UUID sourceId) {
    return sources.entrySet().stream()
        .filter(entry -> entry.getKey().sourceType().equals(sourceType) && entry.getKey().sourceId().equals(sourceId))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private RelatedSourceRef reviewSourceFor(ReviewQueueItem item, UUID clientId) {
    if (item.getItemType() == ReviewQueueItemType.DOCUMENT) {
      return documentRepository.findById(UUID.fromString(item.getItemId()))
          .filter(document -> document.getClient() != null && clientId.equals(document.getClient().getId()))
          .map(document -> new RelatedSourceRef("DOCUMENT", document.getId(), document.getTitle()))
          .orElse(null);
    }
    if (item.getItemType() == ReviewQueueItemType.EMAIL) {
      return emailRepository.findById(UUID.fromString(item.getItemId()))
          .filter(email -> email.getClient() != null && clientId.equals(email.getClient().getId()))
          .map(email -> new RelatedSourceRef("EMAIL", email.getId(), email.getSubject()))
          .orElse(null);
    }
    if (item.getItemType() == ReviewQueueItemType.DOCUMENT_VERSION) {
      return documentVersionRepository.findById(UUID.fromString(item.getItemId()))
          .filter(version -> version.getDocument() != null
              && version.getDocument().getClient() != null
              && clientId.equals(version.getDocument().getClient().getId()))
          .map(version -> new RelatedSourceRef("DOCUMENT", version.getDocument().getId(), version.getDocument().getTitle()))
          .orElse(null);
    }
    return null;
  }

  private SourceContext resolveSourceContext(String sourceType, UUID sourceId) {
    String normalized = sourceType == null ? "" : sourceType.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "DOCUMENT" -> documentRepository.findById(sourceId)
          .filter(document -> document.getClient() != null)
          .map(document -> new SourceContext(document.getClient().getId(), new RelatedSourceRef("DOCUMENT", document.getId(), document.getTitle())))
          .orElseThrow(() -> new IllegalArgumentException("Knowledge source not found: " + sourceType + ":" + sourceId));
      case "EMAIL" -> emailRepository.findById(sourceId)
          .filter(email -> email.getClient() != null)
          .map(email -> new SourceContext(email.getClient().getId(), new RelatedSourceRef("EMAIL", email.getId(), email.getSubject())))
          .orElseThrow(() -> new IllegalArgumentException("Knowledge source not found: " + sourceType + ":" + sourceId));
      case "NOTE" -> noteRepository.findById(sourceId)
          .filter(note -> note.getClient() != null)
          .map(note -> new SourceContext(note.getClient().getId(), new RelatedSourceRef("NOTE", note.getId(), "Customer note")))
          .orElseThrow(() -> new IllegalArgumentException("Knowledge source not found: " + sourceType + ":" + sourceId));
      default -> throw new IllegalArgumentException("Unsupported source type: " + sourceType);
    };
  }

  private List<MetadataValue> metadataFor(String ownerType, UUID ownerId, Map<String, List<MetadataValue>> cache) {
    String key = ownerType + ":" + ownerId;
    return cache.computeIfAbsent(key, ignored -> metadataValueRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId));
  }

  private List<ClientContracts.BusinessReferenceFieldResponse> toBusinessReferenceFields(
      List<MetadataValue> metadataValues,
      Set<Permission> permissions) {
    List<ClientContracts.BusinessReferenceFieldResponse> fields = new ArrayList<>();
    for (MetadataValue value : metadataValues) {
      String key = normalizeFieldKey(value.getField());
      if (!BUSINESS_REFERENCE_KEYS.contains(key)) {
        continue;
      }
      fields.add(new ClientContracts.BusinessReferenceFieldResponse(
          key,
          businessReferenceLabel(key),
          maskMetadataValue(value, permissions)));
    }
    return fields;
  }

  private Map<String, String> businessReferenceMap(List<MetadataValue> metadataValues, Set<Permission> permissions) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (ClientContracts.BusinessReferenceFieldResponse field : toBusinessReferenceFields(metadataValues, permissions)) {
      fields.put(field.key(), field.value());
    }
    return fields;
  }

  private String maskMetadataValue(MetadataValue value, Set<Permission> permissions) {
    if (!value.getField().isPii() || piiMaskingService.canViewPii(permissions)) {
      return value.getTextValue();
    }
    return piiMaskingService.trimFreeText(value.getTextValue(), permissions);
  }

  private String normalizeFieldKey(MetadataField field) {
    String raw = field.getFieldKey() == null || field.getFieldKey().isBlank() ? field.getLabel() : field.getFieldKey();
    return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
  }

  private String businessReferenceLabel(String key) {
    return switch (key) {
      case "policy_number" -> "Policy Number";
      case "claim_number" -> "Claim Number";
      case "insurer" -> "Insurer";
      case "policy_type" -> "Policy Type";
      case "effective_date" -> "Effective Date";
      case "expiry_date" -> "Expiry Date";
      case "renewal_date" -> "Renewal Date";
      case "broker_reference" -> "Broker Reference";
      case "external_reference" -> "External Reference";
      default -> key;
    };
  }

  private String firstContentHash(UUID clientId, String sourceType, UUID sourceId) {
    return embeddingChunkRepository.findByClientIdAndSourceTypeAndSourceIdOrderByChunkIndexAsc(clientId, sourceType, sourceId).stream()
        .map(EmbeddingChunk::getContentHash)
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private ClientContracts.KnowledgeEvidenceReferenceResponse toEvidenceReference(
      String sourceType,
      UUID sourceId,
      UUID sourceVersionId,
      String title,
      String detail,
      Integer pageNumber,
      String section) {
    String jumpTarget = pageNumber != null
        ? sourceType.toLowerCase(Locale.ROOT) + ":" + sourceId + ":page:" + pageNumber
        : sourceType.toLowerCase(Locale.ROOT) + ":" + sourceId + (section == null ? "" : ":" + section);
    return new ClientContracts.KnowledgeEvidenceReferenceResponse(
        sourceType,
        sourceId,
        sourceVersionId,
        title,
        detail,
        pageNumber,
        section,
        jumpTarget);
  }

  private String encodeCursor(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private String decodeCursor(String cursor) {
    try {
      return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ignored) {
      return cursor;
    }
  }

  private int sanitizeLimit(Integer requested, int max, int fallback) {
    if (requested == null || requested <= 0) {
      return fallback;
    }
    return Math.min(requested, max);
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }

  public record TimelineQuery(
      String cursor,
      Integer limit,
      String query,
      String from,
      String to,
      String sourceType,
      String eventType,
      String documentType,
      String reviewStatus,
      String policyNumber,
      String claimNumber,
      String insurer,
      String actor) {
  }

  private record TimelineEventRecord(
      String eventId,
      UUID customerId,
      String eventType,
      String sourceType,
      UUID sourceId,
      UUID sourceVersionId,
      String title,
      String summary,
      Instant occurredAt,
      Instant recordedAt,
      String actor,
      String documentType,
      List<ClientContracts.BusinessReferenceFieldResponse> businessReferenceFields,
      String status,
      List<ClientContracts.KnowledgeEvidenceReferenceResponse> evidenceReferences,
      List<String> availableActions,
      String permissionState,
      String correlationId) {

    ClientContracts.CustomerKnowledgeTimelineEventResponse toResponse() {
      return new ClientContracts.CustomerKnowledgeTimelineEventResponse(
          eventId,
          customerId,
          eventType,
          sourceType,
          sourceId,
          sourceVersionId,
          title,
          summary,
          occurredAt,
          recordedAt,
          actor,
          documentType,
          businessReferenceFields,
          status,
          evidenceReferences,
          availableActions,
          permissionState,
          correlationId);
    }
  }

  private record KnowledgeSourceSnapshot(
      UUID clientId,
      String sourceType,
      UUID id,
      String title,
      Instant occurredAt,
      Map<String, String> businessReferenceFields,
      String contentHash,
      UUID parentEmailId,
      String threadId,
      String documentType,
      UUID documentId,
      UUID versionId,
      Integer versionNumber) {
  }

  private record RelatedKnowledgeRecord(
      String relationshipId,
      String sourceType,
      UUID sourceId,
      String sourceTitle,
      String relatedSourceType,
      UUID relatedSourceId,
      String relatedTitle,
      String relationshipType,
      Double score,
      String explanation,
      Map<String, String> supportingFields,
      List<ClientContracts.KnowledgeEvidenceReferenceResponse> evidenceReferences,
      String derivationType,
      Instant createdAt,
      boolean inferred,
      UUID customerId) {

    ClientContracts.RelatedKnowledgeLinkResponse toResponse() {
      return new ClientContracts.RelatedKnowledgeLinkResponse(
          relationshipId,
          customerId,
          sourceType,
          sourceId,
          sourceTitle,
          relatedSourceType,
          relatedSourceId,
          relatedTitle,
          relationshipType,
          score,
          explanation,
          supportingFields,
          evidenceReferences,
          derivationType,
          createdAt,
          inferred);
    }

    RelatedKnowledgeRecord reversed() {
      return new RelatedKnowledgeRecord(
          relationshipId,
          relatedSourceType,
          relatedSourceId,
          relatedTitle,
          sourceType,
          sourceId,
          sourceTitle,
          relationshipType,
          score,
          explanation,
          supportingFields,
          evidenceReferences,
          derivationType,
          createdAt,
          inferred,
          customerId);
    }
  }

  private record RelatedSourceRef(String sourceType, UUID sourceId, String title) {
  }

  private record SourceContext(UUID clientId, RelatedSourceRef sourceRef) {
  }

  private record VectorMatch(String relatedSourceType, UUID relatedSourceId, double score) {
  }
}
