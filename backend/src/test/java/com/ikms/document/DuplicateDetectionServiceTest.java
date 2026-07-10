package com.ikms.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DuplicateDetectionServiceTest {

  private DocumentVersionRepository documentVersionRepository;
  private DuplicateDetectionService duplicateDetectionService;

  @BeforeEach
  void setUp() {
    documentVersionRepository = mock(DocumentVersionRepository.class);
    duplicateDetectionService = new DuplicateDetectionService(documentVersionRepository);
  }

  @Test
  void shouldReturnDuplicateMatchWhenHashAlreadyExists() {
    Document document = new Document();
    document.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    DocumentVersion version = new DocumentVersion();
    version.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    version.setDocument(document);
    version.setFileHash("dup-hash");
    version.setOriginalStoragePath("stored/original.pdf");

    when(documentVersionRepository.findByFileHash("dup-hash")).thenReturn(Optional.of(version));

    var match = duplicateDetectionService.findExactDuplicate("dup-hash");

    assertThat(match).isPresent();
    assertThat(match.get().documentId()).isEqualTo(document.getId());
    assertThat(match.get().versionId()).isEqualTo(version.getId());
  }
}
