package com.ikms.operations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OperationsContracts {

  private OperationsContracts() {
  }

  public record JobResponse(
      UUID jobId,
      String jobType,
      UUID submittedBy,
      Instant submittedAt,
      Instant startedAt,
      Instant completedAt,
      Long duration,
      String status,
      int progress,
      String errorSummary,
      int retryCount,
      String queueKey,
      String targetType,
      String targetId,
      int priority,
      boolean cancelRequested,
      Map<String, String> details) {
  }

  public record QueueResponse(
      String queueKey,
      String queueName,
      String status,
      boolean paused,
      int depth,
      int runningItems,
      int failedItems,
      Instant updatedAt,
      String explanation) {
  }

  public record QueueItemResponse(
      String queueKey,
      String itemId,
      String itemType,
      String title,
      String status,
      int priority,
      Instant submittedAt,
      Instant startedAt,
      Instant completedAt,
      String errorSummary,
      boolean retrySupported,
      boolean cancelSupported,
      boolean prioritizeSupported,
      Map<String, String> details) {
  }

  public record SchedulerExecutionResponse(
      UUID executionId,
      Instant startedAt,
      Instant completedAt,
      String status,
      String triggerSource,
      String details) {
  }

  public record SchedulerResponse(
      String schedulerKey,
      String displayName,
      String description,
      boolean enabled,
      Instant nextExecution,
      Instant lastExecution,
      String lastStatus,
      List<SchedulerExecutionResponse> history) {
  }

  public record CacheResponse(
      String cacheKey,
      String displayName,
      int entryCount,
      String lastAction,
      Instant lastActionAt) {
  }

  public record HealthComponentResponse(
      String component,
      String status,
      String explanation) {
  }

  public record HealthResponse(
      String overallStatus,
      List<HealthComponentResponse> components) {
  }

  public record DiagnosticsResponse(
      Map<String, String> systemInformation,
      Map<String, Integer> activeWorkers,
      Map<String, Integer> queueDepth,
      int failedJobs,
      List<String> bottlenecks,
      List<String> configurationValidation,
      List<String> dependencyValidation,
      List<JobResponse> recentFailures,
      List<MetricResponse> metrics) {
  }

  public record AlertDefinitionResponse(
      String alertId,
      String severity,
      String category,
      String threshold,
      String suppressionWindow,
      String escalationLevel,
      String resolutionGuidance) {
  }

  public record MetricResponse(
      String metricGroup,
      String metricKey,
      String metricUnit,
      String value,
      Instant recordedAt) {
  }

  public record ReindexRequest(
      @NotBlank String scope,
      UUID customerId,
      UUID documentId) {
  }

  public record EmbeddingRebuildRequest(
      @NotBlank String scope,
      UUID customerId,
      UUID documentId) {
  }

  public record PrioritizeRequest(
      @Min(1) @Max(1000) int priority) {
  }
}
