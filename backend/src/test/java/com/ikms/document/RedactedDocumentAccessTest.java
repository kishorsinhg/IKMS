package com.ikms.document;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ikms.audit.AuditService;
import com.ikms.client.Client;
import com.ikms.common.api.GlobalExceptionHandler;
import com.ikms.security.AppUserPrincipal;
import com.ikms.security.ContentSensitivityService;
import com.ikms.security.GovernanceAccessService;
import com.ikms.security.PiiMaskingService;
import com.ikms.security.SecurityTrimService;
import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import com.ikms.storage.FileStorageService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RedactedDocumentAccessTest {

  private DocumentRepository documentRepository;
  private DocumentVersionRepository documentVersionRepository;
  private DocumentRedactionService documentRedactionService;
  private FileStorageService fileStorageService;
  private AuditService auditService;
  private ContentSensitivityService contentSensitivityService;
  private MockMvc mockMvc;
  private Document document;
  private DocumentVersion version;

  @BeforeEach
  void setUp() {
    documentRepository = mock(DocumentRepository.class);
    documentVersionRepository = mock(DocumentVersionRepository.class);
    documentRedactionService = mock(DocumentRedactionService.class);
    fileStorageService = mock(FileStorageService.class);
    auditService = mock(AuditService.class);
    contentSensitivityService = mock(ContentSensitivityService.class);

    var controller = new DocumentAccessController(
        documentRepository,
        documentVersionRepository,
        documentRedactionService,
        fileStorageService,
        contentSensitivityService,
        new SecurityTrimService(new PiiMaskingService()),
        new GovernanceAccessService(),
        auditService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    Client client = new Client();
    client.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    document = new Document();
    document.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    document.setClient(client);

    version = new DocumentVersion();
    version.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    version.setDocument(document);
    version.setFileName("policy.pdf");
    version.setMimeType("application/pdf");
    version.setOriginalStoragePath("original/policy.pdf");
    version.setRedactedStoragePath("redacted/policy.txt");
    version.setRedactionStatus(RedactionStatus.AVAILABLE);
    version.setCurrent(true);

    when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
    when(documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId())).thenReturn(Optional.of(version));
    when(contentSensitivityService.documentContainsPii(document.getId())).thenReturn(true);
  }

  @Test
  void processorShouldReceiveRedactedPreview() throws Exception {
    when(fileStorageService.load("redacted/policy.txt"))
        .thenReturn(new ByteArrayResource("REDACTED".getBytes()));

    mockMvc.perform(get("/api/documents/{documentId}/preview", document.getId())
            .principal(processorAuthentication()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("policy-redacted.txt")))
        .andExpect(content().string("REDACTED"));
  }

  @Test
  void supervisorShouldReceiveOriginalDownload() throws Exception {
    when(fileStorageService.load("original/policy.pdf"))
        .thenReturn(new ByteArrayResource("ORIGINAL".getBytes()));

    mockMvc.perform(get("/api/documents/{documentId}/download", document.getId())
            .principal(supervisorAuthentication()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
        .andExpect(content().string("ORIGINAL"));
  }

  @Test
  void processorShouldBeDeniedWhenNoRedactedVariantExists() throws Exception {
    version.setRedactedStoragePath(null);
    version.setRedactionStatus(RedactionStatus.FAILED);

    when(documentRedactionService.ensureRedactedVariant(version, true)).thenReturn(version);

    mockMvc.perform(get("/api/documents/{documentId}/download", document.getId())
            .principal(processorAuthentication()))
        .andExpect(status().isForbidden());

    verify(auditService).write(org.mockito.ArgumentMatchers.argThat(event ->
        "DOCUMENT_DOWNLOAD_DENIED".equals(event.action())));
  }

  @Test
  void processorShouldReceiveOriginalWhenDocumentHasNoPiiMetadata() throws Exception {
    when(contentSensitivityService.documentContainsPii(document.getId())).thenReturn(false);
    when(fileStorageService.load("original/policy.pdf"))
        .thenReturn(new ByteArrayResource("ORIGINAL".getBytes()));

    mockMvc.perform(get("/api/documents/{documentId}/download", document.getId())
            .principal(processorAuthentication()))
        .andExpect(status().isOk())
        .andExpect(content().string("ORIGINAL"));
  }

  private Authentication processorAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        new AppUserPrincipal(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "processor",
            "password",
            "Processor User",
            "processor@example.com",
            UserStatus.ACTIVE,
            Set.of(UserRole.PROCESSOR),
            Set.of(Permission.VIEW_REDACTED_DOCUMENTS),
            Set.of()),
        null,
        Set.of());
  }

  private Authentication supervisorAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        new AppUserPrincipal(
            UUID.fromString("44444444-4444-4444-4444-444444444444"),
            "supervisor",
            "password",
            "Supervisor User",
            "supervisor@example.com",
            UserStatus.ACTIVE,
            Set.of(UserRole.SUPERVISOR),
            Set.of(Permission.VIEW_REDACTED_DOCUMENTS, Permission.VIEW_ORIGINAL_DOCUMENTS, Permission.VIEW_PII),
            Set.of()),
        null,
        Set.of());
  }
}
