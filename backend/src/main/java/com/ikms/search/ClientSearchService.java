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
    return searchDetailed(clientId, query, permissions).results();
  }

  public SearchOutcome searchDetailed(UUID clientId, String query, Set<Permission> permissions) {
    String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<Document> documents = documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId);
    List<Email> emails = emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId);
    List<Note> notes = noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(clientId, NoteStatus.ACTIVE);
    Map<String, CandidateMatch> matches = new HashMap<>();
    List<String> warnings = new ArrayList<>();
    String retrievalMode = normalizedQuery.isBlank() ? "BROWSE" : "KEYWORD_FALLBACK";

    if (!normalizedQuery.isBlank()) {
      Set<String> queryTokens = tokenize(normalizedQuery);
      VectorRetrievalOutcome vectorOutcome = findSimilarChunks(clientId, normalizedQuery);
      retrievalMode = vectorOutcome.retrievalMode();
      if (vectorOutcome.warning() != null && !vectorOutcome.warning().isBlank()) {
        warnings.add(vectorOutcome.warning());
      }
      for (SimilarChunk chunk : vectorOutcome.chunks()) {
        mergeMatch(
            matches,
            chunk.sourceType(),
            chunk.sourceId(),
            Math.max(0d, 3d - chunk.distance()),
            expandChunkContext(chunk),
            chunk.pageNumber(),
            chunk.sourceSection(),
            "VECTOR_HYBRID");
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
          mergeMatch(matches, chunk.getSourceType(), chunk.getSourceId(), hybridScore + 1.2d, matchedText, chunk.getPageNumber(), chunk.getSourceSection(), "KEYWORD_CHUNK");
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
            metadataValue.getField().getLabel(),
            "METADATA");
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
        String retrievalPath = match == null
            ? (normalizedQuery.isBlank() ? "BROWSE" : haystack.contains(normalizedQuery) ? "SOURCE_TEXT" : "BROWSE")
            : match.retrievalPath();
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
                retrievalPath,
                citationQuality("DOCUMENT", match == null ? null : match.pageNumber(), match == null ? null : match.sourceSection()),
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
        String retrievalPath = match == null
            ? (normalizedQuery.isBlank() ? "BROWSE" : haystack.contains(normalizedQuery) ? "SOURCE_TEXT" : "BROWSE")
            : match.retrievalPath();
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
                retrievalPath,
                citationQuality("EMAIL", match == null ? null : match.pageNumber(), match == null ? null : match.sourceSection()),
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
        String retrievalPath = match == null
            ? (normalizedQuery.isBlank() ? "BROWSE" : haystack.contains(normalizedQuery) ? "SOURCE_TEXT" : "BROWSE")
            : match.retrievalPath();
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
                retrievalPath,
                citationQuality("NOTE", match == null ? null : match.pageNumber(), match == null ? null : match.sourceSection()),
                note.getCreatedAt()),
            score));
      }
    }

    List<SearchContracts.SearchResultResponse> results = candidates.stream()
        .sorted(Comparator
            .comparing(SearchResultCandidate::score, Comparator.reverseOrder())
            .thenComparing(candidate -> candidate.result().occurredAt(), Comparator.nullsLast(Comparator.reverseOrder())))
        .map(SearchResultCandidate::result)
        .limit(20)
        .toList();
    if (results.stream().anyMatch(result -> "LOW".equals(result.citationQuality()))) {
      warnings.add("Some retrieved evidence has limited location metadata and may produce weaker citations.");
    }
    return new SearchOutcome(results, retrievalMode, warnings);
  }

  private static void mergeMatch(
      Map<String, CandidateMatch> matches,
      String sourceType,
      UUID sourceId,
      double score,
      String matchedText,
      Integer pageNumber,
      String sourceSection,
      String retrievalPath) {
    matches.merge(
        key(sourceType, sourceId),
        new CandidateMatch(score, matchedText, pageNumber, sourceSection, retrievalPath),
        (left, right) -> left.score() >= right.score()
            ? new CandidateMatch(left.score() + right.score(), left.matchedText(), left.pageNumber(), left.sourceSection(), left.retrievalPath())
            : new CandidateMatch(left.score() + right.score(), right.matchedText(), right.pageNumber(), right.sourceSection(), right.retrievalPath()));
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

  private static String citationQuality(String sourceType, Integer pageNumber, String sourceSection) {
    boolean hasLocation = pageNumber != null || (sourceSection != null && !sourceSection.isBlank());
    if ("DOCUMENT".equals(sourceType)) {
      return hasLocation ? "HIGH" : "LOW";
    }
    return hasLocation ? "HIGH" : "MEDIUM";
  }

  private record CandidateMatch(double score, String matchedText, Integer pageNumber, String sourceSection, String retrievalPath) {
  }

  private record SearchResultCandidate(SearchContracts.SearchResultResponse result, double score) {
  }

  private VectorRetrievalOutcome findSimilarChunks(UUID clientId, String normalizedQuery) {
    var providerSettings = aiProviderSettingsService.current();
    var queryEmbedding = aiProviderClient.embed(providerSettings, List.of(normalizedQuery))
        .filter(result -> !result.isEmpty() && result.getFirst() != null && !result.getFirst().isEmpty())
        .map(result -> result.getFirst())
        .orElse(List.of());
    if (queryEmbedding.isEmpty()) {
      return new VectorRetrievalOutcome(
          List.of(),
          "KEYWORD_FALLBACK",
          "Embedding provider was unavailable for this query; keyword and metadata fallback were used.");
    }

    String vectorLiteral = toVectorLiteral(queryEmbedding);
    try {
      List<SimilarChunk> chunks = jdbcTemplate.query(
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
      return new VectorRetrievalOutcome(chunks, "HYBRID_VECTOR", null);
    } catch (Exception ignored) {
      return new VectorRetrievalOutcome(
          List.of(),
          "KEYWORD_FALLBACK",
          "Vector retrieval was unavailable for this query; keyword and metadata fallback were used.");
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

  public record SearchOutcome(
      List<SearchContracts.SearchResultResponse> results,
      String retrievalMode,
      List<String> warnings) {
  }

  private record VectorRetrievalOutcome(
      List<SimilarChunk> chunks,
      String retrievalMode,
      String warning) {
  }
}
