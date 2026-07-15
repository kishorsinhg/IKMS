package com.ikms.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

public final class RequestContextHolder {

  public static final String REQUEST_ID = "requestId";
  public static final String CORRELATION_ID = "correlationId";
  public static final String BACKGROUND_JOB_ID = "backgroundJobId";
  public static final String AI_INTERACTION_ID = "aiInteractionId";
  public static final String PROCESSING_JOB_ID = "processingJobId";
  public static final String REVIEW_ID = "reviewId";
  public static final String RETRIEVAL_ID = "retrievalId";
  public static final String TIMELINE_REQUEST_ID = "timelineRequestId";
  public static final String SEARCH_REQUEST_ID = "searchRequestId";

  private static final ThreadLocal<LinkedHashMap<String, String>> CONTEXT =
      ThreadLocal.withInitial(LinkedHashMap::new);

  private RequestContextHolder() {
  }

  public static Scope openRequest(String requestId, String correlationId) {
    LinkedHashMap<String, String> previous = snapshotInternal();
    LinkedHashMap<String, String> next = new LinkedHashMap<>();
    next.put(REQUEST_ID, requestId);
    next.put(CORRELATION_ID, correlationId);
    restore(next);
    return new Scope(previous);
  }

  public static Scope with(String key, String value) {
    LinkedHashMap<String, String> previous = snapshotInternal();
    LinkedHashMap<String, String> next = snapshotInternal();
    if (value == null || value.isBlank()) {
      next.remove(key);
    } else {
      next.put(key, value);
    }
    restore(next);
    return new Scope(previous);
  }

  public static Scope withGenerated(String key) {
    return with(key, UUID.randomUUID().toString());
  }

  public static String get(String key) {
    return CONTEXT.get().get(key);
  }

  public static String requestId() {
    return get(REQUEST_ID);
  }

  public static String correlationId() {
    return get(CORRELATION_ID);
  }

  public static Map<String, String> snapshot() {
    return Map.copyOf(snapshotInternal());
  }

  public static Map<String, String> traceIdentifiers() {
    LinkedHashMap<String, String> identifiers = new LinkedHashMap<>();
    putIfPresent(identifiers, REQUEST_ID);
    putIfPresent(identifiers, CORRELATION_ID);
    putIfPresent(identifiers, BACKGROUND_JOB_ID);
    putIfPresent(identifiers, AI_INTERACTION_ID);
    putIfPresent(identifiers, PROCESSING_JOB_ID);
    putIfPresent(identifiers, REVIEW_ID);
    putIfPresent(identifiers, RETRIEVAL_ID);
    putIfPresent(identifiers, TIMELINE_REQUEST_ID);
    putIfPresent(identifiers, SEARCH_REQUEST_ID);
    return Map.copyOf(identifiers);
  }

  public static Scope restoreSnapshot(Map<String, String> snapshot) {
    LinkedHashMap<String, String> previous = snapshotInternal();
    restore(snapshot == null ? Map.of() : snapshot);
    return new Scope(previous);
  }

  public static void clear() {
    CONTEXT.remove();
    MDC.clear();
  }

  private static void putIfPresent(Map<String, String> target, String key) {
    String value = get(key);
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }

  private static LinkedHashMap<String, String> snapshotInternal() {
    return new LinkedHashMap<>(CONTEXT.get());
  }

  private static void restore(Map<String, String> values) {
    LinkedHashMap<String, String> next = new LinkedHashMap<>();
    if (values != null) {
      values.forEach((key, value) -> {
        if (value != null && !value.isBlank()) {
          next.put(key, value);
        }
      });
    }
    CONTEXT.set(next);
    syncMdc(next);
  }

  private static void syncMdc(Map<String, String> context) {
    MDC.clear();
    context.forEach(MDC::put);
  }

  public static final class Scope implements AutoCloseable {

    private final LinkedHashMap<String, String> previous;

    private Scope(LinkedHashMap<String, String> previous) {
      this.previous = previous;
    }

    @Override
    public void close() {
      restore(previous);
    }
  }
}
