package com.ikms.search;

import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
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
import com.ikms.security.ContentSensitivityService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class LexicalSearchRetriever implements SearchRetriever {

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final ContentSensitivityService contentSensitivityService;
  private final MetadataValueRepository metadataValueRepository;

  LexicalSearchRetriever(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      ContentSensitivityService contentSensitivityService,
      EmbeddingChunkRepository ignoredEmbeddingChunkRepository,
      MetadataValueRepository metadataValueRepository) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.contentSensitivityService = contentSensitivityService;
    this.metadataValueRepository = metadataValueRepository;
  }

  @Override
  public List<SearchEvidenceCandidate> retrieve(SearchQueryContext context) {
    List<SearchEvidenceCandidate> candidates = new ArrayList<>();
    addDocumentCandidates(candidates, context);
    addEmailCandidates(candidates, context);
    addNoteCandidates(candidates, context);

    return candidates;
  }

  private void addDocumentCandidates(List<SearchEvidenceCandidate> candidates, SearchQueryContext context) {
    if (!context.allowsSourceType("DOCUMENT") && !context.allowsSourceType("DOCUMENT_VERSION")) {
      return;
    }
    List<Document> documents = context.clientId() == null
        ? documentRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Document::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
            .toList()
        : documentRepository.findByClient_IdOrderByCreatedAtDesc(context.clientId());
    for (Document document : documents) {
      if (!context.matchesClient(document.getClient() == null ? null : document.getClient().getId())
          || !context.inDateRange(document.getCreatedAt())
          || !matchesBusinessReferences("DOCUMENT", document.getId(), context)) {
        continue;
      }
      DocumentVersion version = resolvePreferredVersion(document.getId(), context);
      if (!matchesDocumentSource(document, version, context)) {
        continue;
      }
      String text = document.getTitle() + " " + (version == null ? "" : SearchSupport.nullSafe(version.getExtractedText()));
      double score = context.browsing() ? 0d : SearchSupport.score(context.normalizedQuery(), context.queryTokens(), text);
      if (context.browsing() || score > 0d || context.hasBusinessReferenceFilters() || context.hasSourceIds()) {
        SearchEvidenceCandidate candidate = baseDocumentCandidate(document, version, context.normalizedQuery());
        candidate.addLexicalScore(score);
        candidate.retrievalSignals().add(context.browsing() ? "BROWSE" : "LEXICAL");
        candidates.add(candidate);
      }
    }
  }

  private void addEmailCandidates(List<SearchEvidenceCandidate> candidates, SearchQueryContext context) {
    if (!context.allowsSourceType("EMAIL")) {
      return;
    }
    List<Email> emails = context.clientId() == null
        ? emailRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Email::getReceivedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
            .toList()
        : emailRepository.findByClient_IdOrderByReceivedAtDesc(context.clientId());
    for (Email email : emails) {
      if (!context.matchesClient(email.getClient() == null ? null : email.getClient().getId())
          || !context.matchesSourceId(email.getId())
          || !context.inDateRange(email.getReceivedAt())
          || !matchesBusinessReferences("EMAIL", email.getId(), context)) {
        continue;
      }
      String text = email.getSubject() + " " + SearchSupport.nullSafe(email.getBodyText());
      double score = context.browsing() ? 0d : SearchSupport.score(context.normalizedQuery(), context.queryTokens(), text);
      if (context.browsing() || score > 0d || context.hasBusinessReferenceFilters() || context.hasSourceIds()) {
        SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("EMAIL", email.getId());
        candidate.setTitle(email.getSubject());
        candidate.setExcerpt(SearchSupport.excerpt(null, email.getBodyText(), email.getSubject(), context.normalizedQuery()));
        candidate.setFallbackText(email.getBodyText());
        candidate.setCitation("Email: " + email.getSubject());
        candidate.setSourceSection("email");
        candidate.setOccurredAt(email.getReceivedAt());
        candidate.setContainsPii(contentSensitivityService.emailContainsPii(email.getId()));
        candidate.addLexicalScore(score);
        candidate.retrievalSignals().add(context.browsing() ? "BROWSE" : "LEXICAL");
        candidates.add(candidate);
      }
    }
  }

  private void addNoteCandidates(List<SearchEvidenceCandidate> candidates, SearchQueryContext context) {
    if (!context.allowsSourceType("NOTE")) {
      return;
    }
    List<Note> notes = context.clientId() == null
        ? noteRepository.findAll().stream()
            .filter(note -> note.getStatus() == NoteStatus.ACTIVE)
            .sorted(java.util.Comparator.comparing(Note::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
            .toList()
        : noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(context.clientId(), NoteStatus.ACTIVE);
    for (Note note : notes) {
      if (!context.matchesClient(note.getClient() == null ? null : note.getClient().getId())
          || !context.matchesSourceId(note.getId())
          || !context.inDateRange(note.getCreatedAt())
          || !matchesBusinessReferences("NOTE", note.getId(), context)) {
        continue;
      }
      double score = context.browsing() ? 0d : SearchSupport.score(context.normalizedQuery(), context.queryTokens(), note.getNoteText());
      if (context.browsing() || score > 0d || context.hasBusinessReferenceFilters() || context.hasSourceIds()) {
        SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("NOTE", note.getId());
        candidate.setTitle("Broker note");
        candidate.setExcerpt(SearchSupport.excerpt(null, note.getNoteText(), "Broker note", context.normalizedQuery()));
        candidate.setFallbackText(note.getNoteText());
        candidate.setCitation("Note created " + note.getCreatedAt());
        candidate.setSourceSection("note");
        candidate.setOccurredAt(note.getCreatedAt());
        candidate.setContainsPii(contentSensitivityService.noteContainsPii(note.getId()));
        candidate.addLexicalScore(score);
        candidate.retrievalSignals().add(context.browsing() ? "BROWSE" : "LEXICAL");
        candidates.add(candidate);
      }
    }
  }

  private SearchEvidenceCandidate baseDocumentCandidate(Document document, DocumentVersion version, String normalizedQuery) {
    SearchEvidenceCandidate candidate = new SearchEvidenceCandidate("DOCUMENT", document.getId());
    candidate.setTitle(document.getTitle());
    candidate.setExcerpt(SearchSupport.excerpt(null, version == null ? null : version.getExtractedText(), document.getTitle(), normalizedQuery));
    candidate.setFallbackText(version == null ? document.getTitle() : version.getExtractedText());
    candidate.setCitation("Document: " + document.getTitle());
    candidate.setPageNumber(null);
    candidate.setSourceSection(version == null ? null : "document-version");
    candidate.setOccurredAt(document.getCreatedAt());
    candidate.setContainsPii(contentSensitivityService.documentContainsPii(document.getId()));
    return candidate;
  }

  private DocumentVersion resolvePreferredVersion(UUID documentId, SearchQueryContext context) {
    if (context.versionPreference() == EnterpriseAiContracts.VersionPreference.PREVIOUS_VERSION) {
      List<DocumentVersion> versions = documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(documentId);
      return versions.stream()
          .filter(version -> !version.isCurrent())
          .findFirst()
          .orElse(versions.stream().findFirst().orElse(null));
    }
    return documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)
        .orElseGet(() -> documentVersionRepository.findTopByDocument_IdOrderByVersionNumberDesc(documentId).orElse(null));
  }

  private boolean matchesDocumentSource(Document document, DocumentVersion version, SearchQueryContext context) {
    if (!context.hasSourceIds()) {
      return true;
    }
    return context.matchesSourceId(document.getId()) || (version != null && context.matchesSourceId(version.getId()));
  }

  private boolean matchesBusinessReferences(String ownerType, UUID ownerId, SearchQueryContext context) {
    if (!context.hasBusinessReferenceFilters()) {
      return true;
    }
    String available = metadataValueRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId).stream()
        .map(value -> value.getField().getLabel() + " " + value.getTextValue())
        .collect(Collectors.joining(" "))
        .toLowerCase(Locale.ROOT);
    for (String token : SearchSupport.tokenize(referenceSearchText(context)).stream().map(value -> value.toLowerCase(Locale.ROOT)).toList()) {
      if (!available.contains(token)) {
        return false;
      }
    }
    return true;
  }

  private String referenceSearchText(SearchQueryContext context) {
    return String.join(" ",
        SearchSupport.nullSafe(context.businessReferenceFields().policyNumber()),
        SearchSupport.nullSafe(context.businessReferenceFields().claimNumber()),
        SearchSupport.nullSafe(context.businessReferenceFields().insurer()),
        SearchSupport.nullSafe(context.businessReferenceFields().policyType()),
        SearchSupport.nullSafe(context.businessReferenceFields().effectiveDate()),
        SearchSupport.nullSafe(context.businessReferenceFields().expiryDate()),
        SearchSupport.nullSafe(context.businessReferenceFields().renewalDate()),
        SearchSupport.nullSafe(context.businessReferenceFields().brokerReference()),
        SearchSupport.nullSafe(context.businessReferenceFields().externalReference())).trim();
  }
}
