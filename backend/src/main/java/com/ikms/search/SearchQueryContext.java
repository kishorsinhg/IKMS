package com.ikms.search;

import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record SearchQueryContext(
    UUID clientId,
    String normalizedQuery,
    Set<String> queryTokens,
    Set<Permission> permissions,
    EnterpriseAiContracts.QueryScope scope,
    Set<String> sourceTypes,
    Set<String> documentTypes,
    EnterpriseAiContracts.QueryDateRange dateRange,
    EnterpriseAiContracts.BusinessReferenceFields businessReferenceFields,
    EnterpriseAiContracts.VersionPreference versionPreference,
    EnterpriseAiContracts.SortOrder sortOrder,
    List<UUID> sourceIds,
    int resultLimit,
    UUID conversationId,
    Map<String, String> actorAttributes) {

  public static SearchQueryContext simple(UUID clientId, String query, Set<Permission> permissions) {
    return simple(clientId, query, permissions, Map.of());
  }

  public static SearchQueryContext simple(UUID clientId, String query, Set<Permission> permissions, Map<String, String> actorAttributes) {
    String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    return new SearchQueryContext(
        clientId,
        normalizedQuery,
        SearchSupport.tokenize(normalizedQuery),
        permissions,
        EnterpriseAiContracts.QueryScope.CUSTOMER,
        Set.of(),
        Set.of(),
        null,
        new EnterpriseAiContracts.BusinessReferenceFields(null, null, null, null, null, null, null, null, null),
        EnterpriseAiContracts.VersionPreference.CURRENT_VERSION,
        EnterpriseAiContracts.SortOrder.RELEVANCE,
        List.of(),
        20,
        null,
        actorAttributes == null ? Map.of() : Map.copyOf(actorAttributes));
  }

  public static SearchQueryContext enterprise(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.QueryPlan plan) {
    return new SearchQueryContext(
        request.clientId(),
        plan.normalizedPrompt(),
        SearchSupport.tokenize(plan.normalizedPrompt()),
        request.permissions(),
        plan.scope(),
        plan.sourceTypes().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
        Set.copyOf(plan.documentTypes()),
        plan.dateRange(),
        plan.businessReferenceFields(),
        plan.versionPreference(),
        plan.sortOrder(),
        request.sourceIds() == null ? List.of() : List.copyOf(request.sourceIds()),
        plan.resultLimit(),
        request.conversationId(),
        com.ikms.security.ActorAttributeContext.fromParameters(request.parameters()).asMap());
  }

  public boolean browsing() {
    return normalizedQuery == null || normalizedQuery.isBlank();
  }

  public boolean allowsSourceType(String sourceType) {
    return sourceTypes == null || sourceTypes.isEmpty() || sourceTypes.contains(sourceType);
  }

  public boolean hasSourceIds() {
    return sourceIds != null && !sourceIds.isEmpty();
  }

  public boolean matchesSourceId(UUID sourceId) {
    return !hasSourceIds() || sourceIds.contains(sourceId);
  }

  public boolean hasBusinessReferenceFilters() {
    return businessReferenceFields != null && businessReferenceFields.hasValues();
  }

  public boolean inDateRange(Instant value) {
    if (dateRange == null || value == null) {
      return true;
    }
    Instant from = parseDateBoundary(dateRange.from(), false);
    Instant to = parseDateBoundary(dateRange.to(), true);
    if (from != null && value.isBefore(from)) {
      return false;
    }
    if (to != null && value.isAfter(to)) {
      return false;
    }
    return true;
  }

  public boolean matchesClient(UUID ownerClientId) {
    return clientId == null || clientId.equals(ownerClientId);
  }

  private static Instant parseDateBoundary(String value, boolean endOfDay) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return (endOfDay
            ? java.time.LocalDate.parse(value).plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).minusNanos(1)
            : java.time.LocalDate.parse(value).atStartOfDay(java.time.ZoneOffset.UTC))
        .toInstant();
  }
}
