package com.ikms.operations;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperationsCacheService {

  private final Map<String, CacheState> caches = new LinkedHashMap<>();

  public OperationsCacheService() {
    register("retrieval-cache", "Retrieval cache");
    register("ai-cache", "AI cache");
    register("metadata-cache", "Metadata cache");
    register("configuration-cache", "Configuration cache");
  }

  public synchronized Map<String, CacheState> snapshots() {
    return Map.copyOf(caches);
  }

  public synchronized CacheState clear(String cacheKey) {
    CacheState state = require(cacheKey);
    state.lastAction = "CLEARED";
    state.lastActionAt = Instant.now();
    state.entryCount = 0;
    return copy(state);
  }

  public synchronized CacheState invalidate(String cacheKey) {
    CacheState state = require(cacheKey);
    state.lastAction = "INVALIDATED";
    state.lastActionAt = Instant.now();
    return copy(state);
  }

  public synchronized CacheState refresh(String cacheKey) {
    CacheState state = require(cacheKey);
    state.lastAction = "REFRESHED";
    state.lastActionAt = Instant.now();
    return copy(state);
  }

  private void register(String key, String label) {
    CacheState state = new CacheState();
    state.cacheKey = key;
    state.displayName = label;
    state.lastAction = "READY";
    state.lastActionAt = Instant.now();
    state.entryCount = 0;
    caches.put(key, state);
  }

  private CacheState require(String cacheKey) {
    CacheState state = caches.get(cacheKey);
    if (state == null) {
      throw new IllegalArgumentException("Unknown cache: " + cacheKey);
    }
    return state;
  }

  private static CacheState copy(CacheState input) {
    CacheState copy = new CacheState();
    copy.cacheKey = input.cacheKey;
    copy.displayName = input.displayName;
    copy.entryCount = input.entryCount;
    copy.lastAction = input.lastAction;
    copy.lastActionAt = input.lastActionAt;
    return copy;
  }

  public static final class CacheState {
    private String cacheKey;
    private String displayName;
    private int entryCount;
    private String lastAction;
    private Instant lastActionAt;

    public String getCacheKey() { return cacheKey; }
    public String getDisplayName() { return displayName; }
    public int getEntryCount() { return entryCount; }
    public String getLastAction() { return lastAction; }
    public Instant getLastActionAt() { return lastActionAt; }
  }
}

