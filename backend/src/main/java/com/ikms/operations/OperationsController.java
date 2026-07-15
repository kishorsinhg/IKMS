package com.ikms.operations;

import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.Permission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {

  private final OperationsService operationsService;
  private final AlertDefinitionService alertDefinitionService;

  public OperationsController(OperationsService operationsService, AlertDefinitionService alertDefinitionService) {
    this.operationsService = operationsService;
    this.alertDefinitionService = alertDefinitionService;
  }

  @GetMapping("/jobs")
  public List<OperationsContracts.JobResponse> listJobs(Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.listJobs();
  }

  @GetMapping("/jobs/{jobId}")
  public OperationsContracts.JobResponse getJob(@PathVariable UUID jobId, Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.getJob(jobId);
  }

  @PostMapping("/jobs/{jobId}/retry")
  public OperationsContracts.JobResponse retryJob(@PathVariable UUID jobId, Authentication authentication) {
    require(authentication, Permission.MANAGE_JOBS);
    return operationsService.retryJob(jobId, principal(authentication).id());
  }

  @PostMapping("/jobs/{jobId}/cancel")
  public OperationsContracts.JobResponse cancelJob(@PathVariable UUID jobId, Authentication authentication) {
    require(authentication, Permission.MANAGE_JOBS);
    return operationsService.cancelJob(jobId, principal(authentication).id());
  }

  @GetMapping("/queues")
  public List<OperationsContracts.QueueResponse> listQueues(Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.listQueues();
  }

  @GetMapping("/queues/{queueKey}/items")
  public List<OperationsContracts.QueueItemResponse> listQueueItems(@PathVariable String queueKey, Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.listQueueItems(queueKey);
  }

  @PostMapping("/queues/{queueKey}/pause")
  public OperationsContracts.QueueResponse pauseQueue(@PathVariable String queueKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.pauseQueue(queueKey, principal(authentication).id());
  }

  @PostMapping("/queues/{queueKey}/resume")
  public OperationsContracts.QueueResponse resumeQueue(@PathVariable String queueKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.resumeQueue(queueKey, principal(authentication).id());
  }

  @PostMapping("/queues/{queueKey}/items/{itemId}/retry")
  public OperationsContracts.QueueItemResponse retryQueueItem(@PathVariable String queueKey, @PathVariable String itemId, Authentication authentication) {
    require(authentication, Permission.MANAGE_JOBS);
    return operationsService.retryQueueItem(queueKey, itemId, principal(authentication).id());
  }

  @PostMapping("/queues/{queueKey}/items/{itemId}/cancel")
  public OperationsContracts.QueueItemResponse cancelQueueItem(@PathVariable String queueKey, @PathVariable String itemId, Authentication authentication) {
    require(authentication, Permission.MANAGE_JOBS);
    return operationsService.cancelQueueItem(queueKey, itemId, principal(authentication).id());
  }

  @PostMapping("/queues/{queueKey}/items/{itemId}/prioritize")
  public OperationsContracts.QueueItemResponse prioritizeQueueItem(
      @PathVariable String queueKey,
      @PathVariable String itemId,
      @Valid @RequestBody OperationsContracts.PrioritizeRequest request,
      Authentication authentication) {
    require(authentication, Permission.MANAGE_JOBS);
    return operationsService.prioritizeQueueItem(queueKey, itemId, request.priority(), principal(authentication).id());
  }

  @GetMapping("/schedulers")
  public List<OperationsContracts.SchedulerResponse> listSchedulers(Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.listSchedulers();
  }

  @PostMapping("/schedulers/{schedulerKey}/enable")
  public OperationsContracts.SchedulerResponse enableScheduler(@PathVariable String schedulerKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.enableScheduler(schedulerKey, principal(authentication).id());
  }

  @PostMapping("/schedulers/{schedulerKey}/disable")
  public OperationsContracts.SchedulerResponse disableScheduler(@PathVariable String schedulerKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.disableScheduler(schedulerKey, principal(authentication).id());
  }

  @PostMapping("/schedulers/{schedulerKey}/run")
  public OperationsContracts.SchedulerResponse runScheduler(@PathVariable String schedulerKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.runScheduler(schedulerKey, principal(authentication).id());
  }

  @PostMapping("/reindex")
  public OperationsContracts.JobResponse requestReindex(@Valid @RequestBody OperationsContracts.ReindexRequest request, Authentication authentication) {
    require(authentication, Permission.MANAGE_REINDEX);
    return operationsService.requestReindex(request, principal(authentication).id());
  }

  @PostMapping("/embeddings/rebuild")
  public OperationsContracts.JobResponse requestEmbeddingRebuild(@Valid @RequestBody OperationsContracts.EmbeddingRebuildRequest request, Authentication authentication) {
    require(authentication, Permission.MANAGE_EMBEDDINGS);
    return operationsService.requestEmbeddingRebuild(request, principal(authentication).id());
  }

  @PostMapping("/ocr/retry/{documentId}")
  public OperationsContracts.JobResponse requestOcrRetry(@PathVariable UUID documentId, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.requestOcrRetry(documentId, principal(authentication).id());
  }

  @PostMapping("/ai/retry/{interactionId}")
  public OperationsContracts.JobResponse requestAiRetry(@PathVariable UUID interactionId, Authentication authentication) {
    require(authentication, Permission.MANAGE_AI);
    return operationsService.requestAiRetry(interactionId, principal(authentication).id());
  }

  @GetMapping("/cache")
  public List<OperationsContracts.CacheResponse> listCaches(Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.listCaches();
  }

  @PostMapping("/cache/{cacheKey}/clear")
  public OperationsContracts.CacheResponse clearCache(@PathVariable String cacheKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.clearCache(cacheKey, principal(authentication).id());
  }

  @PostMapping("/cache/{cacheKey}/invalidate")
  public OperationsContracts.CacheResponse invalidateCache(@PathVariable String cacheKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.invalidateCache(cacheKey, principal(authentication).id());
  }

  @PostMapping("/cache/{cacheKey}/refresh")
  public OperationsContracts.CacheResponse refreshCache(@PathVariable String cacheKey, Authentication authentication) {
    require(authentication, Permission.MANAGE_OPERATIONS);
    return operationsService.refreshCache(cacheKey, principal(authentication).id());
  }

  @GetMapping("/health")
  public OperationsContracts.HealthResponse getHealth(Authentication authentication) {
    require(authentication, Permission.VIEW_HEALTH);
    return operationsService.getHealth();
  }

  @GetMapping("/diagnostics")
  public OperationsContracts.DiagnosticsResponse getDiagnostics(Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return operationsService.getDiagnostics();
  }

  @GetMapping("/alerts")
  public List<OperationsContracts.AlertDefinitionResponse> listAlerts(Authentication authentication) {
    require(authentication, Permission.VIEW_OPERATIONS);
    return alertDefinitionService.definitions();
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }

  private void require(Authentication authentication, Permission permission) {
    AppUserPrincipal principal = principal(authentication);
    if (!principal.permissions().contains(permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access operations.");
    }
  }
}
