package com.ikms.search;

import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.DocumentRepository;
import com.ikms.email.EmailRepository;
import com.ikms.note.NoteRepository;
import com.ikms.security.ContentSensitivityService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class MetadataSearchRetriever implements SearchRetriever {

  private final MetadataValueRepository metadataValueRepository;
  private final DocumentRepository documentRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final ContentSensitivityService contentSensitivityService;

  MetadataSearchRetriever(
      MetadataValueRepository metadataValueRepository,
      DocumentRepository documentRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      ContentSensitivityService contentSensitivityService) {
    this.metadataValueRepository = metadataValueRepository;
    this.documentRepository = documentRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.contentSensitivityService = contentSensitivityService;
  }

  @Override
  public List<SearchEvidenceCandidate> retrieve(SearchQueryContext context) {
    if (context.browsing() && !context.hasBusinessReferenceFilters()) {
      return List.of();
    }
    List<SearchEvidenceCandidate> candidates = new ArrayList<>();
    for (String term : searchTerms(context)) {
      if (context.allowsSourceType("DOCUMENT") || context.allowsSourceType("DOCUMENT_VERSION")) {
        candidates.addAll(resolve("DOCUMENT", metadataValueRepository.findByOwnerTypeAndTextValueContainingIgnoreCase("DOCUMENT", term), context));
      }
      if (context.allowsSourceType("EMAIL")) {
        candidates.addAll(resolve("EMAIL", metadataValueRepository.findByOwnerTypeAndTextValueContainingIgnoreCase("EMAIL", term), context));
      }
      if (context.allowsSourceType("NOTE")) {
        candidates.addAll(resolve("NOTE", metadataValueRepository.findByOwnerTypeAndTextValueContainingIgnoreCase("NOTE", term), context));
      }
    }
    return candidates;
  }

  private List<SearchEvidenceCandidate> resolve(
      String ownerType,
      List<MetadataValue> values,
      SearchQueryContext context) {
    List<SearchEvidenceCandidate> candidates = new ArrayList<>();
    for (MetadataValue value : values) {
      if (!matchesContext(ownerType, value, context)) {
        continue;
      }
      SearchEvidenceCandidate candidate = new SearchEvidenceCandidate(ownerType, value.getOwnerId());
      String fieldLabel = value.getField().getLabel();
      String textValue = value.getTextValue();
      candidate.setTitle(resolveTitle(ownerType, value.getOwnerId()));
      candidate.setExcerpt(fieldLabel + ": " + textValue);
      candidate.setFallbackText(textValue);
      candidate.setCitation(candidate.title());
      candidate.setSourceSection(fieldLabel);
      candidate.setContainsPii(resolvePii(ownerType, value.getOwnerId()));
      candidate.setOccurredAt(null);
      candidate.addMetadataScore(SearchSupport.score(context.normalizedQuery(), context.queryTokens(), fieldLabel + " " + textValue) + semanticBoost(fieldLabel));
      candidate.retrievalSignals().add("METADATA");
      candidates.add(candidate);
    }
    return candidates;
  }

  private String resolveTitle(String ownerType, UUID ownerId) {
    return switch (ownerType) {
      case "DOCUMENT" -> documentRepository.findById(ownerId).map(document -> "Document: " + document.getTitle()).orElse("Document metadata");
      case "EMAIL" -> emailRepository.findById(ownerId).map(email -> "Email: " + email.getSubject()).orElse("Email metadata");
      case "NOTE" -> noteRepository.findById(ownerId).map(note -> "Broker note").orElse("Note metadata");
      default -> "Metadata";
    };
  }

  private boolean resolvePii(String ownerType, UUID ownerId) {
    return switch (ownerType) {
      case "DOCUMENT" -> contentSensitivityService.documentContainsPii(ownerId);
      case "EMAIL" -> contentSensitivityService.emailContainsPii(ownerId);
      case "NOTE" -> contentSensitivityService.noteContainsPii(ownerId);
      default -> false;
    };
  }

  private static double semanticBoost(String fieldLabel) {
    String normalized = fieldLabel == null ? "" : fieldLabel.toLowerCase();
    if (normalized.contains("policy") || normalized.contains("claim") || normalized.contains("customer")) {
      return 1.5d;
    }
    if (normalized.contains("version") || normalized.contains("document")) {
      return 1d;
    }
    return 0.5d;
  }

  private boolean matchesContext(String ownerType, MetadataValue value, SearchQueryContext context) {
    if (!context.matchesSourceId(value.getOwnerId())) {
      return false;
    }
    if (!matchesOwnerClient(ownerType, value.getOwnerId(), context)) {
      return false;
    }
    if (!matchesOwnerDate(ownerType, value.getOwnerId(), context)) {
      return false;
    }
    return true;
  }

  private boolean matchesOwnerClient(String ownerType, UUID ownerId, SearchQueryContext context) {
    return switch (ownerType) {
      case "DOCUMENT" -> documentRepository.findById(ownerId)
          .map(document -> context.matchesClient(document.getClient() == null ? null : document.getClient().getId()))
          .orElse(false);
      case "EMAIL" -> emailRepository.findById(ownerId)
          .map(email -> context.matchesClient(email.getClient() == null ? null : email.getClient().getId()))
          .orElse(false);
      case "NOTE" -> noteRepository.findById(ownerId)
          .map(note -> context.matchesClient(note.getClient() == null ? null : note.getClient().getId()))
          .orElse(false);
      default -> false;
    };
  }

  private boolean matchesOwnerDate(String ownerType, UUID ownerId, SearchQueryContext context) {
    return switch (ownerType) {
      case "DOCUMENT" -> documentRepository.findById(ownerId)
          .map(document -> context.inDateRange(document.getCreatedAt()))
          .orElse(false);
      case "EMAIL" -> emailRepository.findById(ownerId)
          .map(email -> context.inDateRange(email.getReceivedAt()))
          .orElse(false);
      case "NOTE" -> noteRepository.findById(ownerId)
          .map(note -> context.inDateRange(note.getCreatedAt()))
          .orElse(false);
      default -> false;
    };
  }

  private static Set<String> searchTerms(SearchQueryContext context) {
    Set<String> terms = new LinkedHashSet<>();
    if (!context.normalizedQuery().isBlank()) {
      terms.add(context.normalizedQuery());
    }
    if (context.businessReferenceFields() != null) {
      addIfPresent(terms, context.businessReferenceFields().policyNumber());
      addIfPresent(terms, context.businessReferenceFields().claimNumber());
      addIfPresent(terms, context.businessReferenceFields().insurer());
      addIfPresent(terms, context.businessReferenceFields().policyType());
      addIfPresent(terms, context.businessReferenceFields().effectiveDate());
      addIfPresent(terms, context.businessReferenceFields().expiryDate());
      addIfPresent(terms, context.businessReferenceFields().renewalDate());
      addIfPresent(terms, context.businessReferenceFields().brokerReference());
      addIfPresent(terms, context.businessReferenceFields().externalReference());
    }
    return terms;
  }

  private static void addIfPresent(Set<String> terms, String value) {
    if (value != null && !value.isBlank()) {
      terms.add(value.trim().toLowerCase(Locale.ROOT));
    }
  }
}
