package com.ikms.search;

import com.ikms.ai.AiProviderClient;
import com.ikms.ai.AiProviderSettingsService;
import com.ikms.ai.EmbeddingChunk;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.security.ContentSensitivityService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class VectorSearchRetriever implements SearchRetriever {

  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;
  private final JdbcTemplate jdbcTemplate;
  private final ContentSensitivityService contentSensitivityService;

  VectorSearchRetriever(
      EmbeddingChunkRepository embeddingChunkRepository,
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient,
      JdbcTemplate jdbcTemplate,
      ContentSensitivityService contentSensitivityService) {
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
    this.jdbcTemplate = jdbcTemplate;
    this.contentSensitivityService = contentSensitivityService;
  }

  @Override
  public List<SearchEvidenceCandidate> retrieve(SearchQueryContext context) {
    return retrieveWithOutcome(context).candidates();
  }

  VectorRetrievalExecution retrieveWithOutcome(SearchQueryContext context) {
    if (context.browsing()) {
      return new VectorRetrievalExecution(List.of(), new VectorRetrievalOutcome(List.of(), "BROWSE", null));
    }
    VectorRetrievalOutcome outcome = findSimilarChunks(context);
    List<SearchEvidenceCandidate> candidates = new ArrayList<>();
    for (SimilarChunk chunk : outcome.chunks()) {
      if (!context.allowsSourceType(chunk.sourceType())
          || !context.matchesSourceId(chunk.sourceId())
          || !context.inDateRange(chunk.createdAt())
          || !matchesBusinessReferences(context, chunk)) {
        continue;
      }
      SearchEvidenceCandidate candidate = new SearchEvidenceCandidate(chunk.sourceType(), chunk.sourceId());
      candidate.setTitle(chunk.sourceTitle() == null || chunk.sourceTitle().isBlank() ? chunk.sourceType() : chunk.sourceTitle());
      candidate.setExcerpt(SearchSupport.excerpt(expandChunkContext(chunk), chunk.chunkText(), candidate.title(), context.normalizedQuery()));
      candidate.setFallbackText(chunk.chunkText());
      candidate.setCitation(chunk.sourceTitle() == null || chunk.sourceTitle().isBlank() ? chunk.sourceType() : chunk.sourceTitle());
      candidate.setPageNumber(chunk.pageNumber());
      candidate.setSourceSection(chunk.sourceSection());
      candidate.setOccurredAt(chunk.createdAt());
      candidate.setContainsPii(resolvePii(chunk.sourceType(), chunk.sourceId()));
      candidate.addVectorScore(Math.max(0d, 3d - chunk.distance()));
      candidate.retrievalSignals().add("VECTOR");
      candidates.add(candidate);
    }
    return new VectorRetrievalExecution(List.copyOf(candidates), outcome);
  }

  VectorRetrievalOutcome findSimilarChunks(SearchQueryContext context) {
    var providerSettings = aiProviderSettingsService.current();
    var queryEmbedding = aiProviderClient.embed(providerSettings, List.of(context.normalizedQuery()))
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
      String sql = context.clientId() == null
          ? """
              select source_type, source_id, chunk_text, chunk_index, page_number,
                     source_title, source_section, metadata_summary, created_at,
                     cast(embedding_vector as vector) <=> cast(? as vector) as distance
              from embedding_chunk
              where embedding_vector is not null
              order by cast(embedding_vector as vector) <=> cast(? as vector) asc
              limit 24
              """
          : """
              select source_type, source_id, chunk_text, chunk_index, page_number,
                     source_title, source_section, metadata_summary, created_at,
                     cast(embedding_vector as vector) <=> cast(? as vector) as distance
              from embedding_chunk
              where client_id = ?
                and embedding_vector is not null
              order by cast(embedding_vector as vector) <=> cast(? as vector) asc
              limit 24
              """;
      List<SimilarChunk> chunks = context.clientId() == null
          ? jdbcTemplate.query(
              sql,
              (resultSet, rowNum) -> new SimilarChunk(
                  resultSet.getString("source_type"),
                  UUID.fromString(resultSet.getString("source_id")),
                  resultSet.getString("chunk_text"),
                  resultSet.getInt("chunk_index"),
                  resultSet.getObject("page_number", Integer.class),
                  resultSet.getString("source_title"),
                  resultSet.getString("source_section"),
                  resultSet.getString("metadata_summary"),
                  resultSet.getTimestamp("created_at").toInstant(),
                  resultSet.getDouble("distance")),
              vectorLiteral,
              vectorLiteral)
          : jdbcTemplate.query(
              sql,
              (resultSet, rowNum) -> new SimilarChunk(
                  resultSet.getString("source_type"),
                  UUID.fromString(resultSet.getString("source_id")),
                  resultSet.getString("chunk_text"),
                  resultSet.getInt("chunk_index"),
                  resultSet.getObject("page_number", Integer.class),
                  resultSet.getString("source_title"),
                  resultSet.getString("source_section"),
                  resultSet.getString("metadata_summary"),
                  resultSet.getTimestamp("created_at").toInstant(),
                  resultSet.getDouble("distance")),
              vectorLiteral,
              context.clientId(),
              vectorLiteral);
      return new VectorRetrievalOutcome(chunks, "HYBRID_VECTOR", null);
    } catch (Exception ignored) {
      return new VectorRetrievalOutcome(
          List.of(),
          "KEYWORD_FALLBACK",
          "Vector retrieval was unavailable for this query; keyword and metadata fallback were used.");
    }
  }

  String expandChunkContext(SimilarChunk chunk) {
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

  private boolean resolvePii(String sourceType, UUID sourceId) {
    return switch (sourceType) {
      case "DOCUMENT" -> contentSensitivityService.documentContainsPii(sourceId);
      case "EMAIL" -> contentSensitivityService.emailContainsPii(sourceId);
      case "NOTE" -> contentSensitivityService.noteContainsPii(sourceId);
      default -> true;
    };
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

  record SimilarChunk(
      String sourceType,
      UUID sourceId,
      String chunkText,
      int chunkIndex,
      Integer pageNumber,
      String sourceTitle,
      String sourceSection,
      String metadataSummary,
      java.time.Instant createdAt,
      double distance) {
  }

  record VectorRetrievalOutcome(
      List<SimilarChunk> chunks,
      String retrievalMode,
      String warning) {
  }

  record VectorRetrievalExecution(
      List<SearchEvidenceCandidate> candidates,
      VectorRetrievalOutcome outcome) {
  }

  private static boolean matchesBusinessReferences(SearchQueryContext context, SimilarChunk chunk) {
    if (!context.hasBusinessReferenceFilters()) {
      return true;
    }
    String summary = SearchSupport.nullSafe(chunk.metadataSummary()).toLowerCase(Locale.ROOT);
    for (String token : referenceTerms(context)) {
      if (!summary.contains(token)) {
        return false;
      }
    }
    return true;
  }

  private static List<String> referenceTerms(SearchQueryContext context) {
    List<String> tokens = new ArrayList<>();
    addReferenceToken(tokens, context.businessReferenceFields().policyNumber());
    addReferenceToken(tokens, context.businessReferenceFields().claimNumber());
    addReferenceToken(tokens, context.businessReferenceFields().insurer());
    addReferenceToken(tokens, context.businessReferenceFields().policyType());
    addReferenceToken(tokens, context.businessReferenceFields().externalReference());
    return tokens;
  }

  private static void addReferenceToken(List<String> tokens, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    tokens.addAll(SearchSupport.tokenize(value).stream().map(token -> token.toLowerCase(Locale.ROOT)).toList());
  }
}
