package com.ikms.document;

import com.ikms.ai.EmbeddingIndexService;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentPublishingService {

  private final DocumentVersionRepository documentVersionRepository;
  private final EmbeddingIndexService embeddingIndexService;

  public DocumentPublishingService(
      DocumentVersionRepository documentVersionRepository,
      EmbeddingIndexService embeddingIndexService) {
    this.documentVersionRepository = documentVersionRepository;
    this.embeddingIndexService = embeddingIndexService;
  }

  public void publish(Document document) {
    if (document.getClient() == null) {
      return;
    }
    UUID clientId = document.getClient().getId();
    documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())
        .ifPresent(version -> embeddingIndexService.indexDocumentVersion(clientId, version));
  }
}
