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

  public EmbeddingIndexService(
      EmbeddingChunkRepository embeddingChunkRepository,
      AiProviderSettingsService aiProviderSettingsService) {
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
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

  private List<EmbeddingChunk> saveChunks(UUID clientId, UUID sourceId, String sourceType, List<String> chunks) {
    var providerSettings = aiProviderSettingsService.current();
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
}
