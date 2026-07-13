package com.ikms.ai;

import com.ikms.document.DocumentVersion;
import com.ikms.email.Email;
import com.ikms.note.Note;
import java.util.ArrayList;
import java.util.List;
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
    version.setEmbeddingModel(aiProviderSettingsService.current().modelName());
    return saveChunks(clientId, version.getId(), "DOCUMENT", chunk(version.getExtractedText(), version.getFileName()));
  }

  public List<EmbeddingChunk> indexEmail(UUID clientId, Email email) {
    return saveChunks(clientId, email.getId(), "EMAIL", chunk(email.getBodyText(), email.getSubject()));
  }

  public List<EmbeddingChunk> indexNote(UUID clientId, Note note) {
    return saveChunks(clientId, note.getId(), "NOTE", chunk(note.getNoteText(), "Broker note"));
  }

  public void deleteSource(String sourceType, UUID sourceId) {
    embeddingChunkRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
  }

  private List<EmbeddingChunk> saveChunks(UUID clientId, UUID sourceId, String sourceType, List<String> chunks) {
    var providerSettings = aiProviderSettingsService.current();
    var embeddings = aiProviderClient.embed(providerSettings, chunks).orElse(List.of());
    embeddingChunkRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    List<EmbeddingChunk> persisted = new ArrayList<>();
    int index = 1;
    for (String chunkText : chunks) {
      EmbeddingChunk chunk = new EmbeddingChunk();
      chunk.setClientId(clientId);
      chunk.setSourceId(sourceId);
      chunk.setSourceType(sourceType);
      chunk.setChunkText(chunkText);
      chunk.setEmbeddingReference(providerSettings.providerName() + ":" + providerSettings.modelName() + ":" + index++);
      if (embeddings.size() >= index && embeddings.get(index - 1) != null && !embeddings.get(index - 1).isEmpty()) {
        chunk.setEmbeddingVector(toVectorLiteral(embeddings.get(index - 1)));
      }
      persisted.add(embeddingChunkRepository.save(chunk));
    }
    return persisted;
  }

  private static List<String> chunk(String text, String fallback) {
    String value = text == null || text.isBlank() ? fallback : text.trim();
    List<String> chunks = new ArrayList<>();
    int step = 240;
    for (int start = 0; start < value.length(); start += step) {
      int end = Math.min(value.length(), start + step);
      chunks.add(value.substring(start, end));
    }
    if (chunks.isEmpty()) {
      chunks.add(fallback);
    }
    return chunks;
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
}
