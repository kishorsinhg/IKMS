package com.ikms.search;

import com.ikms.ai.EmbeddingChunk;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.ai.AiProviderClient;
import com.ikms.ai.AiProviderSettingsService;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;
  private final JdbcTemplate jdbcTemplate;
  private final ContentSensitivityService contentSensitivityService;
  private final SecurityTrimService securityTrimService;

  public ClientSearchService(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      EmbeddingChunkRepository embeddingChunkRepository,
      MetadataValueRepository metadataValueRepository,
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient,
      JdbcTemplate jdbcTemplate,
      ContentSensitivityService contentSensitivityService,
      SecurityTrimService securityTrimService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.metadataValueRepository = metadataValueRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
    this.jdbcTemplate = jdbcTemplate;
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
      List<SimilarChunk> similarChunks = findSimilarChunks(clientId, normalizedQuery);
      for (SimilarChunk chunk : similarChunks) {
        mergeMatch(
            matches,
            chunk.sourceType(),
            chunk.sourceId(),
            Math.max(0d, 3d - chunk.distance()),
            expandChunkContext(chunk),
            chunk.pageNumber(),
            chunk.sourceSection());
      }

      for (EmbeddingChunk chunk : embeddingChunkRepository.findByClientIdOrderByCreatedAtDesc(clientId)) {
        double keywordScore = score(normalizedQuery, queryTokens, chunk.getChunkText());
        double metadataScore = score(normalizedQuery, queryTokens, chunk.getMetadataSummary()) * 0.8d
            + score(normalizedQuery, queryTokens, chunk.getSourceTitle()) * 1.2d
            + score(normalizedQuery, queryTokens, chunk.getSourceSection()) * 0.5d;
        double hybridScore = keywordScore + metadataScore;
        if (hybridScore > 0) {
          String matchedText = keywordScore >= metadataScore
              ? chunk.getChunkText()
              : nullSafe(chunk.getMetadataSummary()) + " " + chunk.getChunkText();
          mergeMatch(matches, chunk.getSourceType(), chunk.getSourceId(), hybridScore + 1.2d, matchedText, chunk.getPageNumber(), chunk.getSourceSection());
        }
      }

      for (MetadataValue metadataValue : metadataValueRepository.findByOwnerTypeAndTextValueContainingIgnoreCase("DOCUMENT", normalizedQuery)) {
        mergeMatch(
            matches,
            "DOCUMENT",
            metadataValue.getOwnerId(),
            2.5d,
            metadataValue.getField().getLabel() + ": " + metadataValue.getTextValue(),
            null,
            metadataValue.getField().getLabel());
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
                match == null ? null : match.pageNumber(),
                match == null ? null : match.sourceSection(),
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
                match == null ? null : match.pageNumber(),
                match == null ? null : match.sourceSection(),
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
                match == null ? null : match.pageNumber(),
                match == null ? null : match.sourceSection(),
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
      String matchedText,
      Integer pageNumber,
      String sourceSection) {
    matches.merge(
        key(sourceType, sourceId),
        new CandidateMatch(score, matchedText, pageNumber, sourceSection),
        (left, right) -> left.score() >= right.score()
            ? new CandidateMatch(left.score() + right.score(), left.matchedText(), left.pageNumber(), left.sourceSection())
            : new CandidateMatch(left.score() + right.score(), right.matchedText(), right.pageNumber(), right.sourceSection()));
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

  private record CandidateMatch(double score, String matchedText, Integer pageNumber, String sourceSection) {
  }

  private record SearchResultCandidate(SearchContracts.SearchResultResponse result, double score) {
  }

  private List<SimilarChunk> findSimilarChunks(UUID clientId, String normalizedQuery) {
    var providerSettings = aiProviderSettingsService.current();
    var queryEmbedding = aiProviderClient.embed(providerSettings, List.of(normalizedQuery))
        .filter(result -> !result.isEmpty() && result.getFirst() != null && !result.getFirst().isEmpty())
        .map(result -> result.getFirst())
        .orElse(List.of());
    if (queryEmbedding.isEmpty()) {
      return List.of();
    }

    String vectorLiteral = toVectorLiteral(queryEmbedding);
    try {
      return jdbcTemplate.query(
          """
              select source_type, source_id, chunk_text, chunk_index, page_number,
                     source_title, source_section, metadata_summary,
                     cast(embedding_vector as vector) <=> cast(? as vector) as distance
              from embedding_chunk
              where client_id = ?
                and embedding_vector is not null
              order by cast(embedding_vector as vector) <=> cast(? as vector) asc
              limit 12
              """,
          (resultSet, rowNum) -> new SimilarChunk(
              resultSet.getString("source_type"),
              UUID.fromString(resultSet.getString("source_id")),
              resultSet.getString("chunk_text"),
              resultSet.getInt("chunk_index"),
              resultSet.getObject("page_number", Integer.class),
              resultSet.getString("source_title"),
              resultSet.getString("source_section"),
              resultSet.getString("metadata_summary"),
              resultSet.getDouble("distance")),
          vectorLiteral,
          clientId,
          vectorLiteral);
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private static String toVectorLiteral(List<Double> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append(values.get(index));
    }
    builder.append(']');
    return builder.toString();
  }

  private String expandChunkContext(SimilarChunk chunk) {
    List<EmbeddingChunk> chunks = embeddingChunkRepository.findBySourceTypeAndSourceIdOrderByChunkIndexAsc(chunk.sourceType(), chunk.sourceId());
    if (chunks.isEmpty()) {
      return chunk.chunkText();
    }
    int start = Math.max(0, chunk.chunkIndex() - 1);
    int end = Math.min(chunks.size() - 1, chunk.chunkIndex() + 1);
    StringBuilder builder = new StringBuilder();
    for (int index = start; index <= end; index++) {
      if (builder.length() > 0) {
        builder.append(' ');
      }
      builder.append(chunks.get(index).getChunkText());
    }
    return builder.toString().trim();
  }

  private record SimilarChunk(
      String sourceType,
      UUID sourceId,
      String chunkText,
      int chunkIndex,
      Integer pageNumber,
      String sourceTitle,
      String sourceSection,
      String metadataSummary,
      double distance) {
  }
}
