package com.ikms.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentVersionServiceTest {

  private DocumentVersionRepository documentVersionRepository;
  private DocumentVersionService documentVersionService;

  @BeforeEach
  void setUp() {
    documentVersionRepository = mock(DocumentVersionRepository.class);
    documentVersionService = new DocumentVersionService(documentVersionRepository);
  }

  @Test
  void shouldIncrementVersionNumberAndRetireCurrentVersion() {
    UUID documentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    Document document = new Document();
    document.setId(documentId);

    DocumentVersion current = new DocumentVersion();
    current.setDocument(document);
    current.setVersionNumber(1);
    current.setCurrent(true);

    when(documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)).thenReturn(Optional.of(current));
    when(documentVersionRepository.findTopByDocument_IdOrderByVersionNumberDesc(documentId)).thenReturn(Optional.of(current));
    when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

    DocumentVersion created = documentVersionService.createNextVersion(
        document,
        "hash-2",
        "renewal-v2.pdf",
        "application/pdf",
        100L,
        "stored/file-2.pdf",
        UUID.randomUUID());

    assertThat(current.isCurrent()).isFalse();
    assertThat(created.getVersionNumber()).isEqualTo(2);
    assertThat(created.isCurrent()).isTrue();
    verify(documentVersionRepository).save(current);
  }
}
