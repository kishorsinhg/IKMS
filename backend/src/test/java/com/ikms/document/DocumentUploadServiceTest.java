package com.ikms.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikms.audit.AuditService;
import com.ikms.client.Client;
import com.ikms.client.ClientService;
import com.ikms.storage.FileStorageService;
import com.ikms.storage.FileStorageService.StoredFile;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentUploadServiceTest {

  private DocumentRepository documentRepository;
  private DuplicateDetectionService duplicateDetectionService;
  private DocumentVersionService documentVersionService;
  private ClientService clientService;
  private FileStorageService fileStorageService;
  private AuditService auditService;
  private DocumentUploadService documentUploadService;

  @BeforeEach
  void setUp() {
    documentRepository = mock(DocumentRepository.class);
    duplicateDetectionService = mock(DuplicateDetectionService.class);
    documentVersionService = mock(DocumentVersionService.class);
    clientService = mock(ClientService.class);
    fileStorageService = mock(FileStorageService.class);
    auditService = mock(AuditService.class);
    documentUploadService = new DocumentUploadService(
        documentRepository,
        duplicateDetectionService,
        documentVersionService,
        clientService,
        fileStorageService,
        auditService);
  }

  @Test
  void shouldReturnDuplicateWithoutStoringFile() {
    UUID documentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID versionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    when(duplicateDetectionService.findExactDuplicate("dup-hash"))
        .thenReturn(Optional.of(new DuplicateDetectionService.DuplicateMatch(documentId, versionId, "dup-hash", "stored")));

    var result = documentUploadService.upload(new DocumentUploadService.UploadCommand(
        null,
        UUID.randomUUID(),
        "renewal.pdf",
        "application/pdf",
        "dup-hash",
        new byte[] {1, 2, 3}));

    assertThat(result.outcome()).isEqualTo(DocumentUploadService.UploadOutcome.DUPLICATE);
    assertThat(result.duplicateOfDocumentId()).isEqualTo(documentId);
    verify(auditService).write(any());
  }

  @Test
  void shouldStoreOriginalAndCreateVersionForNewUpload() {
    UUID actorUserId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    Client client = new Client();
    client.setId(clientId);
    when(duplicateDetectionService.findExactDuplicate("new-hash")).thenReturn(Optional.empty());
    when(clientService.requireClient(clientId)).thenReturn(client);
    when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
      Document document = invocation.getArgument(0);
      if (document.getId() == null) {
        document.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
      }
      return document;
    });
    when(fileStorageService.store(any())).thenReturn(new StoredFile(
        UUID.randomUUID(),
        "stored/originals/renewal.pdf",
        "renewal.pdf",
        "application/pdf",
        FileStorageService.FileVariant.ORIGINAL,
        3L));
    when(documentVersionService.createInitialVersion(any(), any(), any(), any(), anyLong(), any(), any()))
        .thenAnswer(invocation -> {
          Document document = invocation.getArgument(0);
          DocumentVersion version = new DocumentVersion();
          version.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
          version.setDocument(document);
          version.setVersionNumber(1);
          return version;
        });

    var result = documentUploadService.upload(new DocumentUploadService.UploadCommand(
        clientId,
        actorUserId,
        "renewal.pdf",
        "application/pdf",
        "new-hash",
        new byte[] {1, 2, 3}));

    assertThat(result.outcome()).isEqualTo(DocumentUploadService.UploadOutcome.CREATED);
    assertThat(result.documentId()).isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    assertThat(result.versionId()).isEqualTo(UUID.fromString("44444444-4444-4444-4444-444444444444"));
    assertThat(result.reviewStatus()).isEqualTo(DocumentReviewStatus.NOT_REQUIRED);
    verify(fileStorageService).store(any());
    verify(auditService).write(any());
  }
}
