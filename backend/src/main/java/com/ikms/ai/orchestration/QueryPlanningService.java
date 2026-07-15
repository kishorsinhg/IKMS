package com.ikms.ai.orchestration;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class QueryPlanningService {

  private final BusinessReferenceExtractionService businessReferenceExtractionService;

  public QueryPlanningService(BusinessReferenceExtractionService businessReferenceExtractionService) {
    this.businessReferenceExtractionService = businessReferenceExtractionService;
  }

  public EnterpriseAiContracts.QueryPlan plan(
      EnterpriseAiContracts.EnterpriseAiRequest request,
      EnterpriseAiContracts.DetectedIntent intent) {
    EnterpriseAiContracts.BusinessReferenceFields businessReferenceFields = businessReferenceExtractionService.extract(
        intent.normalizedPrompt(),
        request.parameters());
    List<String> retrievalModes = switch (request.operation()) {
      case SEARCH -> List.of("LEXICAL", "VECTOR", "METADATA", "ENTITY");
      case ASK -> List.of("LEXICAL", "VECTOR", "METADATA", "ENTITY", "RELATIONSHIP");
      case SUMMARIZE, EXPLAIN -> List.of("VECTOR", "METADATA", "ENTITY");
      case COMPARE -> List.of("VECTOR", "METADATA", "VERSION", "RELATIONSHIP");
      case EXTRACT, VALIDATE -> List.of("METADATA", "VECTOR", "DOCUMENT");
    };

    int tokenBudget = switch (request.operation()) {
      case SEARCH -> 1600;
      case ASK -> 2800;
      case SUMMARIZE, EXPLAIN -> 2200;
      case COMPARE -> 3200;
      case EXTRACT, VALIDATE -> 1800;
    };

    EnterpriseAiContracts.QueryScope scope = request.clientId() == null
        ? EnterpriseAiContracts.QueryScope.GLOBAL
        : request.operation() == EnterpriseAiOperation.COMPARE
            ? EnterpriseAiContracts.QueryScope.DOCUMENT_VERSION
            : EnterpriseAiContracts.QueryScope.CUSTOMER;

    List<EnterpriseAiContracts.SourceType> sourceTypes = determineSourceTypes(request.operation(), intent.normalizedPrompt());
    EnterpriseAiContracts.SortOrder sortOrder = determineSortOrder(intent.normalizedPrompt());
    EnterpriseAiContracts.VersionPreference versionPreference = determineVersionPreference(intent.normalizedPrompt());
    EnterpriseAiContracts.EvidenceGranularity evidenceGranularity = determineEvidenceGranularity(request.operation(), intent.normalizedPrompt());

    return new EnterpriseAiContracts.QueryPlan(
        request.operation(),
        intent.normalizedPrompt(),
        intent.reasoningMode(),
        scope,
        retrievalModes,
        sourceTypes,
        List.of(),
        new EnterpriseAiContracts.QueryDateRange(
            stringParameter(request.parameters(), "dateFrom"),
            stringParameter(request.parameters(), "dateTo")),
        businessReferenceFields,
        request.operation() == EnterpriseAiOperation.COMPARE ? 10 : 5,
        request.operation() == EnterpriseAiOperation.COMPARE ? 3 : 2,
        tokenBudget,
        request.operation() == EnterpriseAiOperation.COMPARE ? 10 : 20,
        sortOrder,
        versionPreference,
        evidenceGranularity,
        request.sourceIds() == null ? List.of() : List.copyOf(request.sourceIds()));
  }

  private static List<EnterpriseAiContracts.SourceType> determineSourceTypes(
      EnterpriseAiOperation operation,
      String prompt) {
    String lowered = prompt.toLowerCase(Locale.ROOT);
    if (operation == EnterpriseAiOperation.COMPARE) {
      return List.of(
          EnterpriseAiContracts.SourceType.DOCUMENT,
          EnterpriseAiContracts.SourceType.DOCUMENT_VERSION,
          EnterpriseAiContracts.SourceType.OCR_TEXT,
          EnterpriseAiContracts.SourceType.EXTRACTED_FIELD);
    }
    if (lowered.contains("correspondence") || lowered.contains("email")) {
      return List.of(EnterpriseAiContracts.SourceType.EMAIL, EnterpriseAiContracts.SourceType.DOCUMENT);
    }
    if (lowered.contains("review")) {
      return List.of(EnterpriseAiContracts.SourceType.REVIEW, EnterpriseAiContracts.SourceType.DOCUMENT);
    }
    if (lowered.contains("note")) {
      return List.of(EnterpriseAiContracts.SourceType.NOTE, EnterpriseAiContracts.SourceType.DOCUMENT);
    }
    return List.of(
        EnterpriseAiContracts.SourceType.DOCUMENT,
        EnterpriseAiContracts.SourceType.DOCUMENT_VERSION,
        EnterpriseAiContracts.SourceType.EMAIL,
        EnterpriseAiContracts.SourceType.NOTE,
        EnterpriseAiContracts.SourceType.EXTRACTED_FIELD);
  }

  private static EnterpriseAiContracts.SortOrder determineSortOrder(String prompt) {
    String lowered = prompt.toLowerCase(Locale.ROOT);
    if (lowered.contains("latest") || lowered.contains("newest") || lowered.contains("recent")) {
      return EnterpriseAiContracts.SortOrder.NEWEST_FIRST;
    }
    if (lowered.contains("oldest")) {
      return EnterpriseAiContracts.SortOrder.OLDEST_FIRST;
    }
    return EnterpriseAiContracts.SortOrder.RELEVANCE;
  }

  private static EnterpriseAiContracts.VersionPreference determineVersionPreference(String prompt) {
    String lowered = prompt.toLowerCase(Locale.ROOT);
    if (lowered.contains("previous version") || lowered.contains("older version")) {
      return EnterpriseAiContracts.VersionPreference.PREVIOUS_VERSION;
    }
    return EnterpriseAiContracts.VersionPreference.CURRENT_VERSION;
  }

  private static EnterpriseAiContracts.EvidenceGranularity determineEvidenceGranularity(
      EnterpriseAiOperation operation,
      String prompt) {
    String lowered = prompt.toLowerCase(Locale.ROOT);
    if (lowered.contains("page")) {
      return EnterpriseAiContracts.EvidenceGranularity.PAGE;
    }
    if (operation == EnterpriseAiOperation.EXTRACT || operation == EnterpriseAiOperation.VALIDATE || lowered.contains("field")) {
      return EnterpriseAiContracts.EvidenceGranularity.CHUNK;
    }
    return EnterpriseAiContracts.EvidenceGranularity.DOCUMENT;
  }

  private static String stringParameter(java.util.Map<String, Object> parameters, String key) {
    if (parameters == null) {
      return null;
    }
    Object value = parameters.get(key);
    return value instanceof String string && !string.isBlank() ? string.trim() : null;
  }
}
