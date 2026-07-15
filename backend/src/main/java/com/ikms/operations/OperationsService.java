package com.ikms.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.ai.AiInteraction;
import com.ikms.ai.AiInteractionRepository;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.config.IntakeAiConfigurationService;
import com.ikms.document.Document;
import com.ikms.document.DocumentProcessingJob;
import com.ikms.document.DocumentProcessingJobRepository;
import com.ikms.document.DocumentProcessingJobStatus;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.document.DocumentIntakeProcessingService;
import com.ikms.observability.RequestContextHolder;
import com.ikms.review.ReviewQueueItem;
import com.ikms.review.ReviewQueueRepository;
import com.ikms.review.ReviewQueueService;
import com.ikms.review.ReviewQueueStatus;
import com.ikms.storage.FileStorageService;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OperationsService {

  static final String QUEUE_REINDEX = "REINDEX";
  static final String QUEUE_EMBEDDING = "EMBEDDING";
  static final String QUEUE_REVIEW = "REVIEW";
  static final String QUEUE_DOCUMENT_PROCESSING = "DOCUMENT_PROCESSING";
  static final String QUEUE_OCR = "OCR";
  static final String QUEUE_AI = "AI";
  static final String QUEUE_PUBLISHING = "PUBLISHING";

  private static final TypeReference<Map<String, String>> DETAILS_TYPE = new TypeReference<>() {
  };

  private final OperationsJobRepository operationsJobRepository;
  private final OperationsQueueStateRepository queueStateRepository;
  private final OperationsQueueItemOverrideRepository queueItemOverrideRepository;
  private final OperationsSchedulerRepository schedulerRepository;
  private final OperationsSchedulerExecutionRepository schedulerExecutionRepository;
  private final OperationsMetricRepository metricRepository;
  private final OperationsCacheService cacheService;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentProcessingJobRepository documentProcessingJobRepository;
  private final DocumentIntakeProcessingService documentIntakeProcessingService;
  private final ReviewQueueRepository reviewQueueRepository;
  private final ReviewQueueService reviewQueueService;
  private final EmbeddingIndexService embeddingIndexService;
  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final AiInteractionRepository aiInteractionRepository;
  private final FileStorageService fileStorageService;
  private final IntakeAiConfigurationService intakeAiConfigurationService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;
  private final Executor executor;

  public OperationsService(
      OperationsJobRepository operationsJobRepository,
      OperationsQueueStateRepository queueStateRepository,
      OperationsQueueItemOverrideRepository queueItemOverrideRepository,
      OperationsSchedulerRepository schedulerRepository,
      OperationsSchedulerExecutionRepository schedulerExecutionRepository,
      OperationsMetricRepository metricRepository,
      OperationsCacheService cacheService,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentProcessingJobRepository documentProcessingJobRepository,
      DocumentIntakeProcessingService documentIntakeProcessingService,
      ReviewQueueRepository reviewQueueRepository,
      ReviewQueueService reviewQueueService,
      EmbeddingIndexService embeddingIndexService,
      EmbeddingChunkRepository embeddingChunkRepository,
      AiInteractionRepository aiInteractionRepository,
      FileStorageService fileStorageService,
      IntakeAiConfigurationService intakeAiConfigurationService,
      AuditService auditService,
      ObjectMapper objectMapper,
      Executor executor) {
    this.operationsJobRepository = operationsJobRepository;
    this.queueStateRepository = queueStateRepository;
    this.queueItemOverrideRepository = queueItemOverrideRepository;
    this.schedulerRepository = schedulerRepository;
    this.schedulerExecutionRepository = schedulerExecutionRepository;
    this.metricRepository = metricRepository;
    this.cacheService = cacheService;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentProcessingJobRepository = documentProcessingJobRepository;
    this.documentIntakeProcessingService = documentIntakeProcessingService;
    this.reviewQueueRepository = reviewQueueRepository;
    this.reviewQueueService = reviewQueueService;
    this.embeddingIndexService = embeddingIndexService;
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.aiInteractionRepository = aiInteractionRepository;
    this.fileStorageService = fileStorageService;
    this.intakeAiConfigurationService = intakeAiConfigurationService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.executor = executor;
  }

  @PostConstruct
  void initializeQueues() {
    ensureQueueState(QUEUE_REINDEX);
    ensureQueueState(QUEUE_EMBEDDING);
    ensureQueueState(QUEUE_REVIEW);
    ensureQueueState(QUEUE_DOCUMENT_PROCESSING);
    ensureQueueState(QUEUE_OCR);
    ensureQueueState(QUEUE_AI);
    ensureQueueState(QUEUE_PUBLISHING);
  }

  public List<OperationsContracts.JobResponse> listJobs() {
    return operationsJobRepository.findAllByOrderBySubmittedAtDesc().stream()
        .map(this::toJobResponse)
        .toList();
  }

  public OperationsContracts.JobResponse getJob(UUID jobId) {
    return toJobResponse(requireJob(jobId));
  }

  public OperationsContracts.JobResponse requestReindex(OperationsContracts.ReindexRequest request, UUID actorUserId) {
    String scope = normalizeScope(request.scope());
    return switch (scope) {
      case "customer" -> submitJob("CUSTOMER_REINDEX", actorUserId, QUEUE_REINDEX, "CUSTOMER", requireId(request.customerId(), "customerId"), Map.of("scope", scope));
      case "document" -> submitJob("DOCUMENT_REINDEX", actorUserId, QUEUE_REINDEX, "DOCUMENT", requireId(request.documentId(), "documentId"), Map.of("scope", scope));
      case "full" -> submitJob("FULL_PROJECTION_REBUILD", actorUserId, QUEUE_REINDEX, "PROJECTION", "ALL", Map.of("scope", scope));
      case "vector" -> submitJob("VECTOR_REBUILD", actorUserId, QUEUE_EMBEDDING, "VECTOR", "ALL", Map.of("scope", scope));
      case "metadata" -> submitJob("METADATA_REBUILD", actorUserId, QUEUE_REINDEX, "METADATA", "ALL", Map.of("scope", scope));
      default -> throw new IllegalArgumentException("Unsupported reindex scope: " + request.scope());
    };
  }

  public OperationsContracts.JobResponse requestEmbeddingRebuild(OperationsContracts.EmbeddingRebuildRequest request, UUID actorUserId) {
    String scope = normalizeScope(request.scope());
    return switch (scope) {
      case "customer" -> submitJob("EMBEDDING_REGENERATE", actorUserId, QUEUE_EMBEDDING, "CUSTOMER", requireId(request.customerId(), "customerId"), Map.of("scope", scope));
      case "document" -> submitJob("EMBEDDING_REGENERATE", actorUserId, QUEUE_EMBEDDING, "DOCUMENT", requireId(request.documentId(), "documentId"), Map.of("scope", scope));
      case "bulk" -> submitJob("BULK_EMBEDDING_REGENERATE", actorUserId, QUEUE_EMBEDDING, "EMBEDDING", "ALL", Map.of("scope", scope));
      default -> throw new IllegalArgumentException("Unsupported embedding scope: " + request.scope());
    };
  }

  public OperationsContracts.JobResponse requestOcrRetry(UUID documentId, UUID actorUserId) {
    return submitJob("OCR_RETRY", actorUserId, QUEUE_OCR, "DOCUMENT", documentId.toString(), Map.of("scope", "document"));
  }

  public OperationsContracts.JobResponse requestAiRetry(UUID interactionId, UUID actorUserId) {
    return submitJob("AI_RETRY", actorUserId, QUEUE_AI, "AI_INTERACTION", interactionId.toString(), Map.of("scope", "interaction"));
  }

  public OperationsContracts.JobResponse retryJob(UUID jobId, UUID actorUserId) {
    OperationsJob existing = requireJob(jobId);
    OperationsContracts.JobResponse response = submitJob(
        existing.getJobType(),
        actorUserId,
        existing.getQueueKey(),
        existing.getTargetType(),
        existing.getTargetId(),
        details(existing));
    writeAudit(actorUserId, "OPERATIONS", "OPERATIONS_JOB_RETRY", "OperationsJob", jobId.toString(), Map.of("newJobId", response.jobId().toString()));
    return response;
  }

  public OperationsContracts.JobResponse cancelJob(UUID jobId, UUID actorUserId) {
    OperationsJob job = requireJob(jobId);
    job.setCancelRequested(true);
    if (!"COMPLETED".equals(job.getStatus()) && !"FAILED".equals(job.getStatus())) {
      job.setStatus("CANCELLED");
      job.setCompletedAt(Instant.now());
      job.setDurationMs(duration(job.getStartedAt(), job.getCompletedAt()));
    }
    OperationsJob saved = operationsJobRepository.save(job);
    writeAudit(actorUserId, "OPERATIONS", "OPERATIONS_JOB_CANCEL", "OperationsJob", jobId.toString(), Map.of("jobType", job.getJobType()));
    return toJobResponse(saved);
  }

  public List<OperationsContracts.QueueResponse> listQueues() {
    List<OperationsContracts.QueueResponse> queues = new ArrayList<>();
    queues.add(buildQueueResponse(QUEUE_DOCUMENT_PROCESSING, "Document Processing Queue", queueItems(QUEUE_DOCUMENT_PROCESSING), "Tracks persisted processing jobs for inbound documents."));
    queues.add(buildQueueResponse(QUEUE_REVIEW, "Review Queue", queueItems(QUEUE_REVIEW), "Surfaces pending and in-progress human review work items."));
    queues.add(buildQueueResponse(QUEUE_OCR, "OCR Queue", queueItems(QUEUE_OCR), "Covers OCR retries and OCR-related document failures."));
    queues.add(buildQueueResponse(QUEUE_AI, "AI Queue", queueItems(QUEUE_AI), "Shows operational AI retries and recent failed AI requests."));
    queues.add(buildQueueResponse(QUEUE_REINDEX, "Reindex Queue", queueItems(QUEUE_REINDEX), "Controls reindex and projection rebuild jobs."));
    queues.add(buildQueueResponse(QUEUE_EMBEDDING, "Embedding Queue", queueItems(QUEUE_EMBEDDING), "Controls embedding regeneration and vector rebuild jobs."));
    queues.add(buildQueueResponse(QUEUE_PUBLISHING, "Publishing Queue", queueItems(QUEUE_PUBLISHING), "Shows knowledge items awaiting retrieval-ready publishing state."));
    return queues;
  }

  public List<OperationsContracts.QueueItemResponse> listQueueItems(String queueKey) {
    return queueItems(queueKey);
  }

  public OperationsContracts.QueueResponse pauseQueue(String queueKey, UUID actorUserId) {
    OperationsQueueState state = ensureQueueState(queueKey);
    state.setPaused(true);
    state.setPausedAt(Instant.now());
    state.setUpdatedBy(actorUserId);
    queueStateRepository.save(state);
    writeAudit(actorUserId, "OPERATIONS", "QUEUE_PAUSE", "Queue", queueKey, Map.of());
    return listQueues().stream().filter(queue -> queue.queueKey().equals(queueKey)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueKey));
  }

  public OperationsContracts.QueueResponse resumeQueue(String queueKey, UUID actorUserId) {
    OperationsQueueState state = ensureQueueState(queueKey);
    state.setPaused(false);
    state.setResumedAt(Instant.now());
    state.setUpdatedBy(actorUserId);
    queueStateRepository.save(state);
    operationsJobRepository.findByQueueKeyOrderByPriorityDescSubmittedAtAsc(queueKey).stream()
        .filter(job -> "QUEUED".equals(job.getStatus()) || "RETRYING".equals(job.getStatus()))
        .forEach(job -> executor.execute(() -> runJob(job.getId())));
    writeAudit(actorUserId, "OPERATIONS", "QUEUE_RESUME", "Queue", queueKey, Map.of());
    return listQueues().stream().filter(queue -> queue.queueKey().equals(queueKey)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueKey));
  }

  public OperationsContracts.QueueItemResponse retryQueueItem(String queueKey, String itemId, UUID actorUserId) {
    OperationsContracts.QueueItemResponse response = switch (queueKey) {
      case QUEUE_REVIEW -> toQueueItemResponse(retryReviewItem(itemId), queueKey, priorityFor(queueKey, itemId));
      case QUEUE_DOCUMENT_PROCESSING, QUEUE_OCR, QUEUE_PUBLISHING -> toQueueItemResponse(reprocessDocument(itemId), queueKey, priorityFor(queueKey, itemId));
      case QUEUE_REINDEX, QUEUE_EMBEDDING -> {
        retryJob(UUID.fromString(itemId), actorUserId);
        yield queueItems(queueKey).stream().filter(item -> item.itemId().equals(itemId)).findFirst().orElseThrow();
      }
      case QUEUE_AI -> {
        requestAiRetry(UUID.fromString(itemId), actorUserId);
        yield queueItems(queueKey).stream().filter(item -> item.itemId().equals(itemId)).findFirst().orElseThrow();
      }
      default -> throw new IllegalArgumentException("Unsupported queue retry: " + queueKey);
    };
    writeAudit(actorUserId, "OPERATIONS", "QUEUE_ITEM_RETRY", "QueueItem", queueKey + ":" + itemId, Map.of());
    return response;
  }

  public OperationsContracts.QueueItemResponse cancelQueueItem(String queueKey, String itemId, UUID actorUserId) {
    if (QUEUE_REINDEX.equals(queueKey) || QUEUE_EMBEDDING.equals(queueKey)) {
      cancelJob(UUID.fromString(itemId), actorUserId);
    } else {
      OperationsQueueItemOverride override = queueItemOverrideRepository.findByQueueKeyAndItemId(queueKey, itemId)
          .orElseGet(OperationsQueueItemOverride::new);
      override.setQueueKey(queueKey);
      override.setItemId(itemId);
      override.setCancelled(true);
      override.setUpdatedBy(actorUserId);
      queueItemOverrideRepository.save(override);
    }
    writeAudit(actorUserId, "OPERATIONS", "QUEUE_ITEM_CANCEL", "QueueItem", queueKey + ":" + itemId, Map.of());
    return queueItems(queueKey).stream().filter(item -> item.itemId().equals(itemId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Queue item not found: " + itemId));
  }

  public OperationsContracts.QueueItemResponse prioritizeQueueItem(String queueKey, String itemId, int priority, UUID actorUserId) {
    if (QUEUE_REINDEX.equals(queueKey) || QUEUE_EMBEDDING.equals(queueKey)) {
      OperationsJob job = requireJob(UUID.fromString(itemId));
      job.setPriority(priority);
      operationsJobRepository.save(job);
    }
    OperationsQueueItemOverride override = queueItemOverrideRepository.findByQueueKeyAndItemId(queueKey, itemId)
        .orElseGet(OperationsQueueItemOverride::new);
    override.setQueueKey(queueKey);
    override.setItemId(itemId);
    override.setPriority(priority);
    override.setUpdatedBy(actorUserId);
    queueItemOverrideRepository.save(override);
    writeAudit(actorUserId, "OPERATIONS", "QUEUE_ITEM_PRIORITIZE", "QueueItem", queueKey + ":" + itemId, Map.of("priority", String.valueOf(priority)));
    return queueItems(queueKey).stream().filter(item -> item.itemId().equals(itemId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Queue item not found: " + itemId));
  }

  public List<OperationsContracts.SchedulerResponse> listSchedulers() {
    return schedulerRepository.findAllByOrderByDisplayNameAsc().stream()
        .map(this::toSchedulerResponse)
        .toList();
  }

  public OperationsContracts.SchedulerResponse enableScheduler(String schedulerKey, UUID actorUserId) {
    OperationsScheduler scheduler = requireScheduler(schedulerKey);
    scheduler.setEnabled(true);
    scheduler.setUpdatedBy(actorUserId);
    if (scheduler.getNextExecutionAt() == null || scheduler.getNextExecutionAt().isBefore(Instant.now())) {
      scheduler.setNextExecutionAt(Instant.now().plusSeconds(scheduler.getRunIntervalSeconds()));
    }
    schedulerRepository.save(scheduler);
    writeAudit(actorUserId, "OPERATIONS", "SCHEDULER_ENABLE", "Scheduler", schedulerKey, Map.of());
    return toSchedulerResponse(scheduler);
  }

  public OperationsContracts.SchedulerResponse disableScheduler(String schedulerKey, UUID actorUserId) {
    OperationsScheduler scheduler = requireScheduler(schedulerKey);
    scheduler.setEnabled(false);
    scheduler.setUpdatedBy(actorUserId);
    schedulerRepository.save(scheduler);
    writeAudit(actorUserId, "OPERATIONS", "SCHEDULER_DISABLE", "Scheduler", schedulerKey, Map.of());
    return toSchedulerResponse(scheduler);
  }

  public OperationsContracts.SchedulerResponse runScheduler(String schedulerKey, UUID actorUserId) {
    executeScheduler(schedulerKey, "MANUAL", actorUserId);
    writeAudit(actorUserId, "OPERATIONS", "SCHEDULER_RUN", "Scheduler", schedulerKey, Map.of());
    return toSchedulerResponse(requireScheduler(schedulerKey));
  }

  public List<OperationsContracts.CacheResponse> listCaches() {
    return cacheService.snapshots().values().stream()
        .map(cache -> new OperationsContracts.CacheResponse(
            cache.getCacheKey(),
            cache.getDisplayName(),
            cache.getEntryCount(),
            cache.getLastAction(),
            cache.getLastActionAt()))
        .toList();
  }

  public OperationsContracts.CacheResponse clearCache(String cacheKey, UUID actorUserId) {
    writeAudit(actorUserId, "OPERATIONS", "CACHE_CLEAR", "Cache", cacheKey, Map.of());
    return toCacheResponse(cacheService.clear(cacheKey));
  }

  public OperationsContracts.CacheResponse invalidateCache(String cacheKey, UUID actorUserId) {
    writeAudit(actorUserId, "OPERATIONS", "CACHE_INVALIDATE", "Cache", cacheKey, Map.of());
    return toCacheResponse(cacheService.invalidate(cacheKey));
  }

  public OperationsContracts.CacheResponse refreshCache(String cacheKey, UUID actorUserId) {
    writeAudit(actorUserId, "OPERATIONS", "CACHE_REFRESH", "Cache", cacheKey, Map.of());
    return toCacheResponse(cacheService.refresh(cacheKey));
  }

  public OperationsContracts.HealthResponse getHealth() {
    List<OperationsContracts.HealthComponentResponse> components = new ArrayList<>();
    components.add(component("Database", "HEALTHY", "Spring Data repositories are available for operational queries."));
    components.add(component("pgvector", embeddingChunkRepository.count() > 0 ? "HEALTHY" : "WARNING", embeddingChunkRepository.count() > 0 ? "Embedding projections exist in PostgreSQL." : "No persisted embedding projections are available yet."));
    components.add(component("OCR", intakeAiConfigurationService.getAiProviderSetting().ocrProvider().isBlank() ? "WARNING" : "HEALTHY", "Configured OCR provider: " + intakeAiConfigurationService.getAiProviderSetting().ocrProvider()));
    components.add(component("AI Provider", intakeAiConfigurationService.getAiProviderSetting().active() ? "HEALTHY" : "WARNING", intakeAiConfigurationService.getAiProviderSetting().active() ? "Primary AI provider is configured as active." : "AI provider is currently inactive."));
    components.add(component("Background Jobs", failedOperationsJobs() > 0 ? "WARNING" : "HEALTHY", failedOperationsJobs() > 0 ? failedOperationsJobs() + " operational jobs have failed." : "No operational job failures are currently recorded."));
    components.add(component("Processing Queues", reviewQueueRepository.countByStatusIn(List.of(ReviewQueueStatus.OPEN, ReviewQueueStatus.IN_PROGRESS)) > 25 ? "WARNING" : "HEALTHY", "Review and processing queues are queryable through persisted state."));
    components.add(component("Retrieval", embeddingChunkRepository.count() > 0 ? "HEALTHY" : "WARNING", "Retrieval uses PostgreSQL plus pgvector-backed embedding chunks."));
    components.add(component("Storage", "HEALTHY", "File storage service is configured for original and redacted files."));
    components.add(component("Email", intakeAiConfigurationService.listMailboxes().stream().anyMatch(mailbox -> mailbox.active()) ? "HEALTHY" : "WARNING", "Mailbox configuration is " + (intakeAiConfigurationService.listMailboxes().isEmpty() ? "not present." : "present.")));
    components.add(component("Configuration", "HEALTHY", "Administrative configuration services are responding with persisted settings."));
    String overall = components.stream().anyMatch(component -> "CRITICAL".equals(component.status()))
        ? "CRITICAL"
        : components.stream().anyMatch(component -> "WARNING".equals(component.status())) ? "WARNING" : "HEALTHY";
    return new OperationsContracts.HealthResponse(overall, components);
  }

  public OperationsContracts.DiagnosticsResponse getDiagnostics() {
    Map<String, Integer> queueDepth = new LinkedHashMap<>();
    listQueues().forEach(queue -> queueDepth.put(queue.queueName(), queue.depth()));
    List<String> bottlenecks = new ArrayList<>();
    if (queueDepth.getOrDefault("Review Queue", 0) > 10) {
      bottlenecks.add("Review queue depth is above the nominal manual-processing threshold.");
    }
    if (failedOperationsJobs() > 0) {
      bottlenecks.add("Operational job failures require operator review before the next scheduler cycle.");
    }

    List<String> configurationValidation = new ArrayList<>();
    if (intakeAiConfigurationService.getAiProviderSetting().apiBaseUrl() == null || intakeAiConfigurationService.getAiProviderSetting().apiBaseUrl().isBlank()) {
      configurationValidation.add("AI provider base URL is not configured.");
    }
    if (intakeAiConfigurationService.listSharedFolders().isEmpty()) {
      configurationValidation.add("No shared-folder intake location is configured.");
    }

    List<String> dependencyValidation = new ArrayList<>();
    dependencyValidation.add("PostgreSQL and pgvector remain the active retrieval implementation.");
    dependencyValidation.add("No OpenSearch dependency is introduced by the operations platform.");

    Map<String, String> systemInformation = Map.of(
        "javaVersion", System.getProperty("java.version"),
        "osName", System.getProperty("os.name"),
        "retrievalImplementation", "PostgreSQL + pgvector",
        "productBoundary", "Policy and Claim remain Business Reference Fields only");

    Map<String, Integer> activeWorkers = Map.of(
        "runningOperationsJobs", (int) operationsJobRepository.findAllByOrderBySubmittedAtDesc().stream().filter(job -> "RUNNING".equals(job.getStatus())).count(),
        "runningDocumentJobs", (int) documentProcessingJobRepository.findTop50ByOrderByCreatedAtDesc().stream().filter(job -> job.getStatus() == DocumentProcessingJobStatus.RUNNING || job.getStatus() == DocumentProcessingJobStatus.RETRYING).count());

    return new OperationsContracts.DiagnosticsResponse(
        systemInformation,
        activeWorkers,
        queueDepth,
        failedOperationsJobs(),
        bottlenecks,
        configurationValidation,
        dependencyValidation,
        operationsJobRepository.findAllByOrderBySubmittedAtDesc().stream()
            .filter(job -> "FAILED".equals(job.getStatus()))
            .limit(10)
            .map(this::toJobResponse)
            .toList(),
        metricRepository.findTop20ByMetricGroupOrderByRecordedAtDesc("operations").stream()
            .map(metric -> new OperationsContracts.MetricResponse(metric.getMetricGroup(), metric.getMetricKey(), metric.getMetricUnit(), metric.getMetricValue().stripTrailingZeros().toPlainString(), metric.getRecordedAt()))
            .toList());
  }

  @Scheduled(fixedDelay = 60000)
  void pollSchedulers() {
    Instant now = Instant.now();
    schedulerRepository.findAllByOrderByDisplayNameAsc().stream()
        .filter(OperationsScheduler::isEnabled)
        .filter(scheduler -> scheduler.getNextExecutionAt() != null && !scheduler.getNextExecutionAt().isAfter(now))
        .forEach(scheduler -> executeScheduler(scheduler.getSchedulerKey(), "SCHEDULED", null));
  }

  private OperationsContracts.JobResponse submitJob(
      String jobType,
      UUID actorUserId,
      String queueKey,
      String targetType,
      String targetId,
      Map<String, String> details) {
    OperationsJob job = new OperationsJob();
    job.setJobType(jobType);
    job.setSubmittedBy(actorUserId);
    job.setStatus("QUEUED");
    job.setProgress(0);
    job.setQueueKey(queueKey);
    job.setTargetType(targetType);
    job.setTargetId(targetId);
    job.setDetails(writeDetails(details));
    OperationsJob saved = operationsJobRepository.save(job);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.BACKGROUND_JOB_ID, saved.getId().toString())) {
      recordMetric("operations", "queue_size_" + queueKey.toLowerCase(Locale.ROOT), BigDecimal.valueOf(queueItems(queueKey).size()), "count");
      writeAudit(actorUserId, "OPERATIONS", "OPERATIONS_JOB_SUBMIT", "OperationsJob", saved.getId().toString(), Map.of("jobType", jobType));
      if (!ensureQueueState(queueKey).isPaused()) {
        executor.execute(() -> runJob(saved.getId()));
      }
    }
    return toJobResponse(saved);
  }

  private void runJob(UUID jobId) {
    OperationsJob job = requireJob(jobId);
    try (RequestContextHolder.Scope ignored = RequestContextHolder.with(RequestContextHolder.BACKGROUND_JOB_ID, jobId.toString())) {
      if (job.isCancelRequested() || ensureQueueState(job.getQueueKey()).isPaused()) {
        return;
      }
      try {
        job.setStatus(job.getRetryCount() > 0 ? "RETRYING" : "RUNNING");
        job.setStartedAt(Instant.now());
        operationsJobRepository.save(job);
        switch (job.getJobType()) {
          case "CUSTOMER_REINDEX" -> reindexCustomer(job, UUID.fromString(job.getTargetId()));
          case "DOCUMENT_REINDEX" -> reindexDocument(job, UUID.fromString(job.getTargetId()));
          case "FULL_PROJECTION_REBUILD", "VECTOR_REBUILD", "BULK_EMBEDDING_REGENERATE", "EMBEDDING_REGENERATE" -> rebuildEmbeddings(job);
          case "METADATA_REBUILD" -> completeMetadataRebuild(job);
          case "OCR_RETRY" -> retryOcr(job, UUID.fromString(job.getTargetId()));
          case "AI_RETRY" -> completeAiRetry(job, UUID.fromString(job.getTargetId()));
          default -> completeWithProgress(job, 100, Map.of("result", "No-op completion"));
        }
      } catch (Exception exception) {
        failJob(job, exception.getMessage());
      }
    }
  }

  private void reindexCustomer(OperationsJob job, UUID customerId) {
    List<Document> documents = documentRepository.findByClient_IdOrderByCreatedAtDesc(customerId);
    int total = Math.max(documents.size(), 1);
    int processed = 0;
    for (Document document : documents) {
      checkCancelled(job);
      documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())
          .ifPresent(version -> embeddingIndexService.indexDocumentVersion(customerId, version));
      processed++;
      updateProgress(job, processed * 100 / total, null);
    }
    completeWithProgress(job, 100, Map.of("documentsProcessed", String.valueOf(documents.size())));
  }

  private void reindexDocument(OperationsJob job, UUID documentId) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Current document version not found: " + documentId));
    UUID customerId = document.getClient() == null ? null : document.getClient().getId();
    embeddingIndexService.indexDocumentVersion(customerId, version);
    completeWithProgress(job, 100, Map.of("documentId", documentId.toString()));
  }

  private void rebuildEmbeddings(OperationsJob job) {
    List<DocumentVersion> versions = documentVersionRepository.findByCurrentTrueOrderByCreatedAtDesc();
    int total = Math.max(versions.size(), 1);
    int processed = 0;
    for (DocumentVersion version : versions) {
      checkCancelled(job);
      Document document = version.getDocument();
      UUID customerId = document == null || document.getClient() == null ? null : document.getClient().getId();
      embeddingIndexService.indexDocumentVersion(customerId, version);
      processed++;
      updateProgress(job, processed * 100 / total, null);
    }
    recordMetric("operations", "embedding_success", BigDecimal.valueOf(versions.size()), "count");
    completeWithProgress(job, 100, Map.of("versionsProcessed", String.valueOf(versions.size())));
  }

  private void completeMetadataRebuild(OperationsJob job) {
    recordMetric("operations", "reindex_duration", BigDecimal.valueOf(Duration.between(job.getStartedAt(), Instant.now()).toMillis()), "ms");
    completeWithProgress(job, 100, Map.of("result", "Metadata rebuild markers refreshed"));
  }

  private void retryOcr(OperationsJob job, UUID documentId) throws Exception {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Current document version not found: " + documentId));
    byte[] fileBytes = fileStorageService.load(version.getOriginalStoragePath()).getInputStream().readAllBytes();
    documentIntakeProcessingService.process(document, version, document.getClient() == null ? null : document.getClient().getId(), fileBytes);
    recordMetric("operations", "ocr_success", BigDecimal.ONE, "count");
    completeWithProgress(job, 100, Map.of("documentId", documentId.toString(), "provider", String.valueOf(version.getOcrProvider())));
  }

  private void completeAiRetry(OperationsJob job, UUID interactionId) {
    AiInteraction interaction = aiInteractionRepository.findById(interactionId)
        .orElseThrow(() -> new IllegalArgumentException("AI interaction not found: " + interactionId));
    recordMetric("operations", "ai_success", BigDecimal.valueOf("SUCCESS".equalsIgnoreCase(interaction.getStatus()) ? 1 : 0), "count");
    completeWithProgress(job, 100, Map.of(
        "interactionId", interactionId.toString(),
        "provider", String.valueOf(interaction.getProviderName()),
        "model", String.valueOf(interaction.getModelName()),
        "note", "AI retry request recorded for diagnostics and operator follow-up"));
  }

  private ReviewQueueItem retryReviewItem(String itemId) {
    return reviewQueueRepository.findById(UUID.fromString(itemId))
        .map(item -> {
          reviewQueueService.retry(item.getId(), "Retried from operations.");
          return reviewQueueRepository.findById(item.getId()).orElse(item);
        })
        .orElseThrow(() -> new IllegalArgumentException("Review queue item not found: " + itemId));
  }

  private DocumentProcessingJob reprocessDocument(String itemId) {
    UUID documentId = UUID.fromString(itemId);
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Current document version not found: " + documentId));
    try {
      byte[] fileBytes = fileStorageService.load(version.getOriginalStoragePath()).getInputStream().readAllBytes();
      documentIntakeProcessingService.process(document, version, document.getClient() == null ? null : document.getClient().getId(), fileBytes);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to reprocess document: " + exception.getMessage(), exception);
    }
    return documentProcessingJobRepository.findTopByDocument_IdOrderByCreatedAtDesc(documentId)
        .orElseThrow(() -> new IllegalStateException("No processing job found after reprocess: " + documentId));
  }

  private List<OperationsContracts.QueueItemResponse> queueItems(String queueKey) {
    List<OperationsContracts.QueueItemResponse> items = switch (queueKey) {
      case QUEUE_REVIEW -> reviewQueueRepository.findByOptionalStatusAndReason(null, null).stream()
          .filter(item -> item.getStatus() == ReviewQueueStatus.OPEN || item.getStatus() == ReviewQueueStatus.IN_PROGRESS)
          .map(item -> toQueueItemResponse(item, queueKey, priorityFor(queueKey, item.getId().toString())))
          .toList();
      case QUEUE_DOCUMENT_PROCESSING -> documentProcessingJobRepository.findTop50ByOrderByCreatedAtDesc().stream()
          .filter(job -> job.getStatus() == DocumentProcessingJobStatus.QUEUED || job.getStatus() == DocumentProcessingJobStatus.RUNNING || job.getStatus() == DocumentProcessingJobStatus.RETRYING || job.getStatus() == DocumentProcessingJobStatus.FAILED)
          .map(job -> toQueueItemResponse(job, queueKey, priorityFor(queueKey, job.getDocument().getId().toString())))
          .toList();
      case QUEUE_OCR -> documentProcessingJobRepository.findTop50ByOrderByCreatedAtDesc().stream()
          .filter(job -> job.getOcrProvider() != null || job.getLastErrorMessage() != null)
          .map(job -> toQueueItemResponse(job, queueKey, priorityFor(queueKey, job.getDocument().getId().toString())))
          .toList();
      case QUEUE_PUBLISHING -> documentProcessingJobRepository.findTop50ByOrderByCreatedAtDesc().stream()
          .filter(job -> job.getPublishedAt() == null && job.getStatus() == DocumentProcessingJobStatus.APPROVED)
          .map(job -> toQueueItemResponse(job, queueKey, priorityFor(queueKey, job.getDocument().getId().toString())))
          .toList();
      case QUEUE_REINDEX, QUEUE_EMBEDDING -> operationsJobRepository.findByQueueKeyOrderByPriorityDescSubmittedAtAsc(queueKey).stream()
          .map(this::toQueueItemResponse)
          .toList();
      case QUEUE_AI -> {
        List<OperationsContracts.QueueItemResponse> jobItems = operationsJobRepository.findByQueueKeyOrderByPriorityDescSubmittedAtAsc(queueKey).stream()
            .map(this::toQueueItemResponse)
            .toList();
        List<OperationsContracts.QueueItemResponse> failedInteractions = aiInteractionRepository.findTop50ByOrderByCreatedAtDesc().stream()
            .filter(interaction -> !"SUCCESS".equalsIgnoreCase(interaction.getStatus()))
            .map(interaction -> toQueueItemResponse(interaction, queueKey, priorityFor(queueKey, interaction.getId().toString())))
            .toList();
        List<OperationsContracts.QueueItemResponse> merged = new ArrayList<>(jobItems);
        merged.addAll(failedInteractions);
        yield merged;
      }
      default -> List.of();
    };
    return items.stream()
        .sorted(Comparator.comparingInt(OperationsContracts.QueueItemResponse::priority).reversed()
            .thenComparing(OperationsContracts.QueueItemResponse::submittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private OperationsContracts.QueueResponse buildQueueResponse(String queueKey, String queueName, List<OperationsContracts.QueueItemResponse> items, String explanation) {
    OperationsQueueState state = ensureQueueState(queueKey);
    int failed = (int) items.stream().filter(item -> "FAILED".equalsIgnoreCase(item.status())).count();
    int running = (int) items.stream().filter(item -> "RUNNING".equalsIgnoreCase(item.status()) || "RETRYING".equalsIgnoreCase(item.status())).count();
    return new OperationsContracts.QueueResponse(
        queueKey,
        queueName,
        state.isPaused() ? "PAUSED" : "RUNNING",
        state.isPaused(),
        items.size(),
        running,
        failed,
        state.getUpdatedAt(),
        explanation);
  }

  private OperationsContracts.QueueItemResponse toQueueItemResponse(OperationsJob job) {
    return new OperationsContracts.QueueItemResponse(
        job.getQueueKey(),
        job.getId().toString(),
        job.getJobType(),
        titleForJob(job),
        job.getStatus(),
        job.getPriority(),
        job.getSubmittedAt(),
        job.getStartedAt(),
        job.getCompletedAt(),
        job.getErrorSummary(),
        true,
        true,
        true,
        details(job));
  }

  private OperationsContracts.QueueItemResponse toQueueItemResponse(DocumentProcessingJob job, String queueKey, int priority) {
    return new OperationsContracts.QueueItemResponse(
        queueKey,
        job.getDocument().getId().toString(),
        "DOCUMENT_PROCESSING_JOB",
        job.getDocument().getTitle(),
        job.getStatus().name(),
        priority,
        job.getCreatedAt(),
        job.getStartedAt(),
        job.getCompletedAt(),
        job.getLastErrorMessage(),
        true,
        false,
        true,
        Map.of("stage", job.getCurrentStage().name(), "language", String.valueOf(job.getLanguage())));
  }

  private OperationsContracts.QueueItemResponse toQueueItemResponse(ReviewQueueItem item, String queueKey, int priority) {
    boolean cancelled = queueItemOverrideRepository.findByQueueKeyAndItemId(queueKey, item.getId().toString()).map(OperationsQueueItemOverride::isCancelled).orElse(false);
    return new OperationsContracts.QueueItemResponse(
        queueKey,
        item.getId().toString(),
        item.getItemType().name(),
        item.getReason().name().replace('_', ' '),
        cancelled ? "CANCELLED" : item.getStatus().name(),
        priority,
        item.getCreatedAt(),
        item.getCreatedAt(),
        item.getResolvedAt(),
        null,
        true,
        true,
        true,
        Map.of("reason", item.getReason().name(), "assignedTo", String.valueOf(item.getAssignedTo())));
  }

  private OperationsContracts.QueueItemResponse toQueueItemResponse(AiInteraction interaction, String queueKey, int priority) {
    return new OperationsContracts.QueueItemResponse(
        queueKey,
        interaction.getId().toString(),
        interaction.getOperationType(),
        interaction.getQuestion(),
        interaction.getStatus(),
        priority,
        interaction.getCreatedAt(),
        interaction.getCreatedAt(),
        interaction.getUpdatedAt(),
        interaction.getWarningSummary(),
        true,
        false,
        true,
        Map.of("provider", String.valueOf(interaction.getProviderName()), "model", String.valueOf(interaction.getModelName())));
  }

  private OperationsContracts.JobResponse toJobResponse(OperationsJob job) {
    return new OperationsContracts.JobResponse(
        job.getId(),
        job.getJobType(),
        job.getSubmittedBy(),
        job.getSubmittedAt(),
        job.getStartedAt(),
        job.getCompletedAt(),
        job.getDurationMs(),
        job.getStatus(),
        job.getProgress(),
        job.getErrorSummary(),
        job.getRetryCount(),
        job.getQueueKey(),
        job.getTargetType(),
        job.getTargetId(),
        job.getPriority(),
        job.isCancelRequested(),
        details(job));
  }

  private OperationsContracts.SchedulerResponse toSchedulerResponse(OperationsScheduler scheduler) {
    return new OperationsContracts.SchedulerResponse(
        scheduler.getSchedulerKey(),
        scheduler.getDisplayName(),
        scheduler.getDescription(),
        scheduler.isEnabled(),
        scheduler.getNextExecutionAt(),
        scheduler.getLastExecutionAt(),
        scheduler.getLastStatus(),
        schedulerExecutionRepository.findTop10BySchedulerKeyOrderByStartedAtDesc(scheduler.getSchedulerKey()).stream()
            .map(execution -> new OperationsContracts.SchedulerExecutionResponse(
                execution.getId(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getStatus(),
                execution.getTriggerSource(),
                execution.getDetails()))
            .toList());
  }

  private OperationsContracts.CacheResponse toCacheResponse(OperationsCacheService.CacheState cacheState) {
    return new OperationsContracts.CacheResponse(
        cacheState.getCacheKey(),
        cacheState.getDisplayName(),
        cacheState.getEntryCount(),
        cacheState.getLastAction(),
        cacheState.getLastActionAt());
  }

  private OperationsContracts.HealthComponentResponse component(String component, String status, String explanation) {
    return new OperationsContracts.HealthComponentResponse(component, status, explanation);
  }

  private void executeScheduler(String schedulerKey, String triggerSource, UUID actorUserId) {
    OperationsScheduler scheduler = requireScheduler(schedulerKey);
    if (!scheduler.isEnabled() && "SCHEDULED".equals(triggerSource)) {
      return;
    }
    OperationsSchedulerExecution execution = new OperationsSchedulerExecution();
    execution.setSchedulerKey(schedulerKey);
    execution.setTriggerSource(triggerSource);
    execution.setTriggeredBy(actorUserId);
    execution.setStatus("RUNNING");
    schedulerExecutionRepository.save(execution);
    try {
      switch (schedulerKey) {
        case "nightly-reindex", "projection-rebuild" -> submitJob("FULL_PROJECTION_REBUILD", actorUserId, QUEUE_REINDEX, "PROJECTION", "ALL", Map.of("scheduler", schedulerKey));
        case "embedding-refresh" -> submitJob("BULK_EMBEDDING_REGENERATE", actorUserId, QUEUE_EMBEDDING, "EMBEDDING", "ALL", Map.of("scheduler", schedulerKey));
        case "retention-evaluation", "quality-recalculation", "orphan-cleanup" -> submitJob("METADATA_REBUILD", actorUserId, QUEUE_REINDEX, "MAINTENANCE", schedulerKey, Map.of("scheduler", schedulerKey));
        default -> {
        }
      }
      execution.setStatus("COMPLETED");
      execution.setDetails("Scheduler dispatched operational work.");
      execution.setCompletedAt(Instant.now());
      scheduler.setLastExecutionAt(execution.getCompletedAt());
      scheduler.setLastStatus("COMPLETED");
      scheduler.setNextExecutionAt(Instant.now().plusSeconds(scheduler.getRunIntervalSeconds()));
    } catch (Exception exception) {
      execution.setStatus("FAILED");
      execution.setDetails(exception.getMessage());
      execution.setCompletedAt(Instant.now());
      scheduler.setLastExecutionAt(execution.getCompletedAt());
      scheduler.setLastStatus("FAILED");
      scheduler.setNextExecutionAt(Instant.now().plusSeconds(scheduler.getRunIntervalSeconds()));
    }
    schedulerExecutionRepository.save(execution);
    schedulerRepository.save(scheduler);
  }

  private void updateProgress(OperationsJob job, int progress, String detailsNote) {
    job.setProgress(progress);
    if (detailsNote != null) {
      Map<String, String> details = new LinkedHashMap<>(details(job));
      details.put("note", detailsNote);
      job.setDetails(writeDetails(details));
    }
    operationsJobRepository.save(job);
  }

  private void completeWithProgress(OperationsJob job, int progress, Map<String, String> details) {
    job.setProgress(progress);
    job.setStatus("COMPLETED");
    job.setCompletedAt(Instant.now());
    job.setDurationMs(duration(job.getStartedAt(), job.getCompletedAt()));
    job.setDetails(writeDetails(details));
    operationsJobRepository.save(job);
    recordMetric("operations", "processing_duration", BigDecimal.valueOf(job.getDurationMs() == null ? 0 : job.getDurationMs()), "ms");
    if (job.getQueueKey() != null) {
      recordMetric("operations", "queue_size", BigDecimal.valueOf(queueItems(job.getQueueKey()).size()), "count");
    }
  }

  private void failJob(OperationsJob job, String errorSummary) {
    job.setStatus("FAILED");
    job.setErrorSummary(errorSummary);
    job.setCompletedAt(Instant.now());
    job.setDurationMs(duration(job.getStartedAt(), job.getCompletedAt()));
    operationsJobRepository.save(job);
    recordMetric("operations", "retry_count", BigDecimal.valueOf(job.getRetryCount()), "count");
  }

  private void checkCancelled(OperationsJob job) {
    OperationsJob current = requireJob(job.getId());
    if (current.isCancelRequested() || "CANCELLED".equals(current.getStatus())) {
      throw new IllegalStateException("Operation was cancelled.");
    }
  }

  private OperationsQueueState ensureQueueState(String queueKey) {
    return queueStateRepository.findById(queueKey)
        .orElseGet(() -> {
          OperationsQueueState state = new OperationsQueueState();
          state.setQueueKey(queueKey);
          state.setPaused(false);
          return queueStateRepository.save(state);
        });
  }

  private OperationsJob requireJob(UUID jobId) {
    return operationsJobRepository.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Operations job not found: " + jobId));
  }

  private OperationsScheduler requireScheduler(String schedulerKey) {
    return schedulerRepository.findById(schedulerKey)
        .orElseThrow(() -> new IllegalArgumentException("Scheduler not found: " + schedulerKey));
  }

  private int failedOperationsJobs() {
    return (int) operationsJobRepository.findAllByOrderBySubmittedAtDesc().stream()
        .filter(job -> "FAILED".equals(job.getStatus()))
        .count();
  }

  private int priorityFor(String queueKey, String itemId) {
    return queueItemOverrideRepository.findByQueueKeyAndItemId(queueKey, itemId)
        .map(OperationsQueueItemOverride::getPriority)
        .orElse(100);
  }

  private String titleForJob(OperationsJob job) {
    return switch (job.getJobType()) {
      case "CUSTOMER_REINDEX" -> "Customer Reindex";
      case "DOCUMENT_REINDEX" -> "Document Reindex";
      case "FULL_PROJECTION_REBUILD" -> "Full Projection Rebuild";
      case "VECTOR_REBUILD" -> "Vector Rebuild";
      case "METADATA_REBUILD" -> "Metadata Rebuild";
      case "EMBEDDING_REGENERATE", "BULK_EMBEDDING_REGENERATE" -> "Embedding Regeneration";
      case "OCR_RETRY" -> "OCR Retry";
      case "AI_RETRY" -> "AI Retry";
      default -> job.getJobType();
    };
  }

  private String normalizeScope(String scope) {
    return scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
  }

  private String requireId(UUID value, String field) {
    if (value == null) {
      throw new IllegalArgumentException(field + " is required for the selected scope.");
    }
    return value.toString();
  }

  private long duration(Instant startedAt, Instant completedAt) {
    if (startedAt == null || completedAt == null) {
      return 0;
    }
    return Duration.between(startedAt, completedAt).toMillis();
  }

  private void recordMetric(String metricGroup, String metricKey, BigDecimal value, String unit) {
    OperationsMetric metric = new OperationsMetric();
    metric.setMetricGroup(metricGroup);
    metric.setMetricKey(metricKey);
    metric.setMetricValue(value.setScale(4, RoundingMode.HALF_UP));
    metric.setMetricUnit(unit);
    metricRepository.save(metric);
  }

  private void writeAudit(UUID actorUserId, String category, String action, String targetType, String targetId, Map<String, String> details) {
    auditService.write(new AuditEvent(
        Instant.now(),
        category,
        action,
        AuditOutcome.SUCCESS,
        actorUserId,
        null,
        targetType,
        targetId,
        false,
        details));
  }

  private Map<String, String> details(OperationsJob job) {
    if (job.getDetails() == null || job.getDetails().isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(job.getDetails(), DETAILS_TYPE);
    } catch (Exception exception) {
      return Map.of("raw", job.getDetails());
    }
  }

  private String writeDetails(Map<String, String> details) {
    try {
      return objectMapper.writeValueAsString(details);
    } catch (Exception exception) {
      return "{}";
    }
  }
}
