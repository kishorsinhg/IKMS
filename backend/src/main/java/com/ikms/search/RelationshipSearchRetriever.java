package com.ikms.search;

import com.ikms.client.Client;
import com.ikms.client.ClientRepository;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.security.ContentSensitivityService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class RelationshipSearchRetriever implements SearchRetriever {

  private final ClientRepository clientRepository;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final ContentSensitivityService contentSensitivityService;

  RelationshipSearchRetriever(
      ClientRepository clientRepository,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      MetadataValueRepository metadataValueRepository,
      ContentSensitivityService contentSensitivityService) {
    this.clientRepository = clientRepository;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.contentSensitivityService = contentSensitivityService;
  }

  @Override
  public List<SearchEvidenceCandidate> retrieve(SearchQueryContext context) {
    if (context.browsing() && !context.hasBusinessReferenceFilters()) {
      return List.of();
    }
    List<SearchEvidenceCandidate> candidates = new ArrayList<>();
    if (context.clientId() != null) {
      clientRepository.findById(context.clientId()).ifPresent(client -> addCustomerSignals(candidates, client, context));
    } else if (!context.browsing()) {
      clientRepository.searchByQuery(context.normalizedQuery()).forEach(client -> addCustomerSignals(candidates, client, context));
    }
    addMetadataRelationshipSignals(candidates, context);
    addVersionSignals(candidates, context);
    return candidates;
  }

  private void addCustomerSignals(List<SearchEvidenceCandidate> candidates, Client client, SearchQueryContext context) {
    if (!context.matchesClient(client.getId()) || !context.inDateRange(client.getUpdatedAt())) {
      return;
    }
    String customerText = client.getDisplayName() + " " + client.getClientId();
    double score = SearchSupport.score(context.normalizedQuery(), context.queryTokens(), customerText);
    if (score > 0d || context.hasBusinessReferenceFilters()) {
      SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("CUSTOMER", client.getId());
      candidate.setTitle(client.getDisplayName());
      candidate.setExcerpt("Customer " + client.getDisplayName() + " · " + client.getClientId());
      candidate.setFallbackText(customerText);
      candidate.setCitation("Customer: " + client.getDisplayName());
      candidate.setSourceSection("customer");
      candidate.setOccurredAt(client.getUpdatedAt());
      candidate.setContainsPii(true);
      candidate.addRelationshipScore(score + 1.25d);
      candidate.retrievalSignals().add("ENTITY");
      candidates.add(candidate);
    }
  }

  private void addMetadataRelationshipSignals(List<SearchEvidenceCandidate> candidates, SearchQueryContext context) {
    if (!context.allowsSourceType("DOCUMENT") && !context.allowsSourceType("DOCUMENT_VERSION")) {
      return;
    }
    for (String term : relationshipTerms(context)) {
      List<MetadataValue> values = metadataValueRepository.findByOwnerTypeAndTextValueContainingIgnoreCase("DOCUMENT", term);
      for (MetadataValue value : values) {
        String label = value.getField().getLabel();
        String normalizedLabel = label.toLowerCase(Locale.ROOT);
        if (!(normalizedLabel.contains("policy")
            || normalizedLabel.contains("claim")
            || normalizedLabel.contains("customer")
            || normalizedLabel.contains("version")
            || normalizedLabel.contains("document")
            || normalizedLabel.contains("insurer"))) {
          continue;
        }
        Document document = documentRepository.findById(value.getOwnerId()).orElse(null);
        if (document == null
            || !context.matchesClient(document.getClient() == null ? null : document.getClient().getId())
            || !context.matchesSourceId(document.getId())
            || !context.inDateRange(document.getCreatedAt())) {
          continue;
        }
        SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("DOCUMENT", document.getId());
        candidate.setTitle(document.getTitle());
        candidate.setExcerpt(label + ": " + value.getTextValue());
        candidate.setFallbackText(value.getTextValue());
        candidate.setCitation("Document: " + document.getTitle());
        candidate.setSourceSection(label);
        candidate.setOccurredAt(document.getCreatedAt());
        candidate.setContainsPii(contentSensitivityService.documentContainsPii(document.getId()));
        candidate.addRelationshipScore(SearchSupport.score(context.normalizedQuery(), context.queryTokens(), label + " " + value.getTextValue()) + 1.5d);
        candidate.retrievalSignals().add("RELATIONSHIP");
        candidates.add(candidate);
      }
    }
  }

  private void addVersionSignals(List<SearchEvidenceCandidate> candidates, SearchQueryContext context) {
    boolean versionIntent = context.normalizedQuery().contains("version") || context.normalizedQuery().contains("previous");
    List<Document> documents = context.clientId() == null
        ? documentRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Document::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
            .toList()
        : documentRepository.findByClient_IdOrderByCreatedAtDesc(context.clientId());
    for (Document document : documents) {
      if (!context.matchesClient(document.getClient() == null ? null : document.getClient().getId())
          || !context.inDateRange(document.getCreatedAt())) {
        continue;
      }
      List<DocumentVersion> versions = documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(document.getId());
      if (versions.size() <= 1 && !versionIntent) {
        continue;
      }
      for (DocumentVersion version : versions) {
        if (context.versionPreference() == com.ikms.ai.orchestration.EnterpriseAiContracts.VersionPreference.PREVIOUS_VERSION && version.isCurrent()) {
          continue;
        }
        if (context.hasSourceIds() && !context.matchesSourceId(document.getId()) && !context.matchesSourceId(version.getId())) {
          continue;
        }
        String versionText = "version " + version.getVersionNumber() + " " + version.getFileName() + " " + SearchSupport.nullSafe(version.getExtractedText());
        double score = SearchSupport.score(context.normalizedQuery(), context.queryTokens(), versionText);
        if (versionIntent) {
          score += version.isCurrent() ? 0.25d : 1d;
        }
        if (score <= 0d && !versionIntent) {
          continue;
        }
        SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("DOCUMENT", document.getId());
        candidate.setTitle(document.getTitle());
        candidate.setExcerpt("Version " + version.getVersionNumber() + " · " + SearchSupport.excerpt(null, version.getExtractedText(), version.getFileName(), context.normalizedQuery()));
        candidate.setFallbackText(version.getExtractedText());
        candidate.setCitation("Document: " + document.getTitle());
        candidate.setSourceSection("document-version-v" + version.getVersionNumber());
        candidate.setOccurredAt(version.getCreatedAt());
        candidate.setContainsPii(contentSensitivityService.documentContainsPii(document.getId()));
        candidate.addRelationshipScore(score + 0.75d);
        candidate.retrievalSignals().add("VERSION");
        candidates.add(candidate);
      }
    }
  }

  private static List<String> relationshipTerms(SearchQueryContext context) {
    List<String> terms = new ArrayList<>();
    if (!context.normalizedQuery().isBlank()) {
      terms.add(context.normalizedQuery());
    }
    if (context.businessReferenceFields() != null) {
      addIfPresent(terms, context.businessReferenceFields().policyNumber());
      addIfPresent(terms, context.businessReferenceFields().claimNumber());
      addIfPresent(terms, context.businessReferenceFields().insurer());
      addIfPresent(terms, context.businessReferenceFields().externalReference());
    }
    return terms;
  }

  private static void addIfPresent(List<String> values, String value) {
    if (value != null && !value.isBlank()) {
      values.add(value);
    }
  }
}
