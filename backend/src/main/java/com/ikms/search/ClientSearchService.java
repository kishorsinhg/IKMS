package com.ikms.search;

import com.ikms.ai.EmbeddingChunk;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.config.domain.MetadataValue;
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
import com.ikms.security.SecurityTrimService;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientSearchService {

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final MetadataValueRepository metadataValueRepository;
  private final ContentSensitivityService contentSensitivityService;
  private final SecurityTrimService securityTrimService;

  public ClientSearchService(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      EmbeddingChunkRepository embeddingChunkRepository,
      MetadataValueRepository metadataValueRepository,
      ContentSensitivityService contentSensitivityService,
      SecurityTrimService securityTrimService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.contentSensitivityService = contentSensitivityService;
    this.securityTrimService = securityTrimService;
  }

  public List<SearchContracts.SearchResultResponse> search(UUID clientId, String query, Set<Permission> permissions) {
    String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<Document> documents = documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId);
    List<Email> emails = emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId);
    List<Note> notes = noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(clientId, NoteStatus.ACTIVE);
    Map<String, CandidateMatch> matches = new HashMap<>();

    if (!normalizedQuery.isBlank()) {
      Set<String> queryTokens = tokenize(normalizedQuery);
      for (EmbeddingChunk chunk : embeddingChunkRepository.findByClientIdOrderByCreatedAtDesc(clientId)) {
        double score = score(normalizedQuery, queryTokens, chunk.getChunkText());
        if (score > 0) {
          mergeMatch(matches, chunk.getSourceType(), chunk.getSourceId(), score + 1.5d, chunk.getChunkText());
        }
      }

      for (MetadataValue metadataValue : metadataValueRepository.findByOwnerTypeAndTextValueContainingIgnoreCase("DOCUMENT", normalizedQuery)) {
        mergeMatch(
            matches,
            "DOCUMENT",
            metadataValue.getOwnerId(),
            2.5d,
            metadataValue.getField().getLabel() + ": " + metadataValue.getTextValue());
      }
    }

    List<SearchResultCandidate> candidates = new ArrayList<>();
    for (Document document : documents) {
      DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).orElse(null);
      String haystack = (document.getTitle() + " " + (version == null ? "" : nullSafe(version.getExtractedText())))
          .toLowerCase(Locale.ROOT);
      CandidateMatch match = matches.get(key("DOCUMENT", document.getId()));
      if (normalizedQuery.isBlank() || match != null || haystack.contains(normalizedQuery)) {
        double score = match == null ? 0d : match.score();
        if (!normalizedQuery.isBlank() && haystack.contains(normalizedQuery)) {
          score += 1d;
        }
        String excerpt = excerpt(match == null ? null : match.matchedText(), version == null ? null : version.getExtractedText(),
            document.getTitle(), normalizedQuery);
        boolean containsPii = contentSensitivityService.documentContainsPii(document.getId());
        candidates.add(new SearchResultCandidate(
            new SearchContracts.SearchResultResponse(
                "DOCUMENT",
                document.getId(),
                document.getTitle(),
                securityTrimService.trimSearchResult(permissions, excerpt, containsPii),
                "Document: " + document.getTitle(),
                document.getCreatedAt()),
            score));
      }
    }

    for (Email email : emails) {
      String haystack = (email.getSubject() + " " + nullSafe(email.getBodyText())).toLowerCase(Locale.ROOT);
      CandidateMatch match = matches.get(key("EMAIL", email.getId()));
      if (normalizedQuery.isBlank() || match != null || haystack.contains(normalizedQuery)) {
        double score = match == null ? 0d : match.score();
        if (!normalizedQuery.isBlank() && haystack.contains(normalizedQuery)) {
          score += 1d;
        }
        String excerpt = excerpt(match == null ? null : match.matchedText(), email.getBodyText(), email.getSubject(), normalizedQuery);
        boolean containsPii = contentSensitivityService.emailContainsPii(email.getId());
        candidates.add(new SearchResultCandidate(
            new SearchContracts.SearchResultResponse(
                "EMAIL",
                email.getId(),
                email.getSubject(),
                securityTrimService.trimSearchResult(permissions, excerpt, containsPii),
                "Email: " + email.getSubject(),
                email.getReceivedAt()),
            score));
      }
    }

    for (Note note : notes) {
      String haystack = note.getNoteText().toLowerCase(Locale.ROOT);
      CandidateMatch match = matches.get(key("NOTE", note.getId()));
      if (normalizedQuery.isBlank() || match != null || haystack.contains(normalizedQuery)) {
        double score = match == null ? 0d : match.score();
        if (!normalizedQuery.isBlank() && haystack.contains(normalizedQuery)) {
          score += 1d;
        }
        boolean containsPii = contentSensitivityService.noteContainsPii(note.getId());
        candidates.add(new SearchResultCandidate(
            new SearchContracts.SearchResultResponse(
                "NOTE",
                note.getId(),
                "Broker note",
                securityTrimService.trimSearchResult(
                    permissions,
                    excerpt(match == null ? null : match.matchedText(), note.getNoteText(), "Broker note", normalizedQuery),
                    containsPii),
                "Note created " + note.getCreatedAt(),
                note.getCreatedAt()),
            score));
      }
    }

    return candidates.stream()
        .sorted(Comparator
            .comparing(SearchResultCandidate::score, Comparator.reverseOrder())
            .thenComparing(candidate -> candidate.result().occurredAt(), Comparator.nullsLast(Comparator.reverseOrder())))
        .map(SearchResultCandidate::result)
        .limit(20)
        .toList();
  }

  private static void mergeMatch(
      Map<String, CandidateMatch> matches,
      String sourceType,
      UUID sourceId,
      double score,
      String matchedText) {
    matches.merge(
        key(sourceType, sourceId),
        new CandidateMatch(score, matchedText),
        (left, right) -> left.score() >= right.score() ? new CandidateMatch(left.score() + right.score(), left.matchedText()) : new CandidateMatch(left.score() + right.score(), right.matchedText()));
  }

  private static String key(String sourceType, UUID sourceId) {
    return sourceType + ":" + sourceId;
  }

  private static double score(String normalizedQuery, Set<String> queryTokens, String text) {
    String normalizedText = nullSafe(text).toLowerCase(Locale.ROOT);
    if (normalizedText.isBlank()) {
      return 0d;
    }
    double score = normalizedText.contains(normalizedQuery) ? 2d : 0d;
    Set<String> textTokens = tokenize(normalizedText);
    long overlap = queryTokens.stream().filter(textTokens::contains).count();
    return score + overlap;
  }

  private static Set<String> tokenize(String value) {
    return java.util.Arrays.stream(value.split("[^\\p{L}\\p{N}]+"))
        .map(String::trim)
        .filter(token -> !token.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String excerpt(String preferredText, String text, String fallback, String query) {
    String value = nullSafe(preferredText).trim();
    if (value.isBlank()) {
      value = nullSafe(text).trim();
    }
    if (value.isBlank()) {
      return fallback;
    }
    if (query == null || query.isBlank()) {
      return truncate(value);
    }
    int index = value.toLowerCase(Locale.ROOT).indexOf(query);
    if (index < 0) {
      return truncate(value);
    }
    int start = Math.max(0, index - 40);
    int end = Math.min(value.length(), index + query.length() + 80);
    return value.substring(start, end).trim();
  }

  private static String truncate(String value) {
    return value.length() <= 160 ? value : value.substring(0, 160).trim() + "...";
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private record CandidateMatch(double score, String matchedText) {
  }

  private record SearchResultCandidate(SearchContracts.SearchResultResponse result, double score) {
  }
}
