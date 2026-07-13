package com.ikms.ai;

import com.ikms.document.DocumentVersion;
import com.ikms.email.Email;
import com.ikms.note.Note;
import com.ikms.worker.extract.TextExtractionService.PageSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmbeddingIndexService {

  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;

  public EmbeddingIndexService(
      EmbeddingChunkRepository embeddingChunkRepository,
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient) {
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
  }

  public List<EmbeddingChunk> indexDocumentVersion(UUID clientId, DocumentVersion version) {
    return indexDocumentVersion(clientId, version, List.of(new PageSegment(null, version.getExtractedText())));
  }

  public List<EmbeddingChunk> indexDocumentVersion(UUID clientId, DocumentVersion version, List<PageSegment> pageSegments) {
    version.setEmbeddingModel(aiProviderSettingsService.current().embeddingModelName());
    return saveChunks(
        clientId,
        version.getId(),
        "DOCUMENT",
        semanticChunks(pageSegments, version.getFileName(), version.getLanguage(), "document-version"));
  }

  public List<EmbeddingChunk> indexEmail(UUID clientId, Email email) {
    return saveChunks(
        clientId,
        email.getId(),
        "EMAIL",
        semanticChunks(List.of(new PageSegment(null, email.getBodyText())), email.getSubject(), inferLanguage(email.getBodyText()), "email"));
  }

  public List<EmbeddingChunk> indexNote(UUID clientId, Note note) {
    return saveChunks(
        clientId,
        note.getId(),
        "NOTE",
        semanticChunks(List.of(new PageSegment(null, note.getNoteText())), "Broker note", inferLanguage(note.getNoteText()), "note"));
  }

  public void deleteSource(String sourceType, UUID sourceId) {
    embeddingChunkRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
  }

  private List<EmbeddingChunk> saveChunks(UUID clientId, UUID sourceId, String sourceType, List<ChunkDescriptor> chunks) {
    var providerSettings = aiProviderSettingsService.current();
    var embeddings = aiProviderClient.embed(providerSettings, chunks.stream().map(ChunkDescriptor::text).toList()).orElse(List.of());
    embeddingChunkRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    List<EmbeddingChunk> persisted = new ArrayList<>();
    int index = 1;
    for (ChunkDescriptor descriptor : chunks) {
      EmbeddingChunk chunk = new EmbeddingChunk();
      chunk.setClientId(clientId);
      chunk.setSourceId(sourceId);
      chunk.setSourceType(sourceType);
      chunk.setChunkText(descriptor.text());
      chunk.setChunkIndex(descriptor.chunkIndex());
      chunk.setTokenCount(descriptor.tokenCount());
      chunk.setSourceTitle(descriptor.sourceTitle());
      chunk.setSourceSection(descriptor.sourceSection());
      chunk.setPageNumber(descriptor.pageNumber());
      chunk.setMetadataSummary(descriptor.metadataSummary());
      chunk.setLanguage(descriptor.language());
      chunk.setEmbeddingReference(providerSettings.providerName() + ":" + providerSettings.embeddingModelName() + ":" + index++);
      if (embeddings.size() >= index && embeddings.get(index - 1) != null && !embeddings.get(index - 1).isEmpty()) {
        chunk.setEmbeddingVector(toVectorLiteral(embeddings.get(index - 1)));
      }
      persisted.add(embeddingChunkRepository.save(chunk));
    }
    return persisted;
  }

  static List<ChunkDescriptor> semanticChunks(
      List<PageSegment> pageSegments,
      String sourceTitle,
      String language,
      String sourceSection) {
    List<ChunkDescriptor> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    String overlap = "";
    int index = 0;
    Integer currentPageNumber = null;

    List<PageSegment> safeSegments = pageSegments == null || pageSegments.isEmpty()
        ? List.of(new PageSegment(null, sourceTitle))
        : pageSegments;

    for (PageSegment segment : safeSegments) {
      String segmentText = segment.text() == null || segment.text().isBlank() ? sourceTitle : segment.text().trim();
      if (currentPageNumber == null) {
        currentPageNumber = segment.pageNumber();
      }
      for (String paragraph : splitParagraphs(segmentText)) {
        for (String sentence : splitSentences(paragraph)) {
        if (!current.isEmpty() && current.length() + sentence.length() + 1 > 900) {
          String chunkText = current.toString().trim();
          chunks.add(new ChunkDescriptor(
              chunkText,
              index++,
              approximateTokenCount(chunkText),
              sourceTitle,
              sourceSection,
              currentPageNumber,
              metadataSummary(sourceTitle, sourceSection, language, chunkText),
              language));
          overlap = trailingContext(chunkText);
          current = new StringBuilder(overlap);
          currentPageNumber = segment.pageNumber();
        }
        if (!current.isEmpty()) {
          current.append(' ');
        }
        current.append(sentence.trim());
      }
      }
    }
    if (!current.isEmpty()) {
      String chunkText = current.toString().trim();
      chunks.add(new ChunkDescriptor(
          chunkText,
          index,
          approximateTokenCount(chunkText),
          sourceTitle,
          sourceSection,
          currentPageNumber,
          metadataSummary(sourceTitle, sourceSection, language, chunkText),
          language));
    }
    if (chunks.isEmpty()) {
      chunks.add(new ChunkDescriptor(
          sourceTitle,
          0,
          approximateTokenCount(sourceTitle),
          sourceTitle,
          sourceSection,
          currentPageNumber,
          metadataSummary(sourceTitle, sourceSection, language, sourceTitle),
          language));
    }
    return chunks;
  }

  private static List<String> splitParagraphs(String value) {
    List<String> paragraphs = new ArrayList<>();
    for (String paragraph : value.split("\\R\\s*\\R")) {
      String trimmed = paragraph.trim();
      if (!trimmed.isBlank()) {
        paragraphs.add(trimmed);
      }
    }
    if (paragraphs.isEmpty()) {
      paragraphs.add(value.trim());
    }
    return paragraphs;
  }

  private static List<String> splitSentences(String paragraph) {
    List<String> sentences = new ArrayList<>();
    for (String sentence : paragraph.split("(?<=[.!?])\\s+")) {
      String trimmed = sentence.trim();
      if (!trimmed.isBlank()) {
        sentences.add(trimmed);
      }
    }
    if (sentences.isEmpty()) {
      sentences.add(paragraph.trim());
    }
    return sentences;
  }

  private static String trailingContext(String chunkText) {
    List<String> sentences = splitSentences(chunkText);
    if (sentences.isEmpty()) {
      return "";
    }
    return sentences.get(sentences.size() - 1);
  }

  private static int approximateTokenCount(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    return value.trim().split("\\s+").length;
  }

  private static String metadataSummary(String sourceTitle, String sourceSection, String language, String chunkText) {
    return "title=" + nullSafe(sourceTitle)
        + "; section=" + nullSafe(sourceSection)
        + "; language=" + nullSafe(language)
        + "; preview=" + (chunkText.length() <= 140 ? chunkText : chunkText.substring(0, 140).trim());
  }

  private static String inferLanguage(String text) {
    String normalized = nullSafe(text).toLowerCase(Locale.ROOT);
    return normalized.contains(" der ") || normalized.contains(" und ") ? "de" : "en";
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
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

  record ChunkDescriptor(
      String text,
      int chunkIndex,
      int tokenCount,
      String sourceTitle,
      String sourceSection,
      Integer pageNumber,
      String metadataSummary,
      String language) {
  }
}
