package com.ikms.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.ai.AiConversationRepository;
import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.audit.AuditService;
import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentReviewStatus;
import com.ikms.document.DocumentSource;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.Email;
import com.ikms.email.EmailProcessingStatus;
import com.ikms.email.EmailRepository;
import com.ikms.email.EmailReviewStatus;
import com.ikms.note.NoteRepository;
import com.ikms.review.ReviewQueueRepository;
import com.ikms.security.PiiMaskingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ClientKnowledgeServiceTest {

  private ClientService clientService;
  private DocumentRepository documentRepository;
  private DocumentVersionRepository documentVersionRepository;
  private EmailRepository emailRepository;
  private NoteRepository noteRepository;
  private ReviewQueueRepository reviewQueueRepository;
  private MetadataValueRepository metadataValueRepository;
  private AiConversationRepository aiConversationRepository;
  private EmbeddingChunkRepository embeddingChunkRepository;
  private ClientKnowledgeService service;

  @BeforeEach
  void setUp() {
    clientService = mock(ClientService.class);
    documentRepository = mock(DocumentRepository.class);
    documentVersionRepository = mock(DocumentVersionRepository.class);
    emailRepository = mock(EmailRepository.class);
    noteRepository = mock(NoteRepository.class);
    reviewQueueRepository = mock(ReviewQueueRepository.class);
    metadataValueRepository = mock(MetadataValueRepository.class);
    aiConversationRepository = mock(AiConversationRepository.class);
    embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    AuditService auditService = mock(AuditService.class);

    service = new ClientKnowledgeService(
        clientService,
        documentRepository,
        documentVersionRepository,
        emailRepository,
        noteRepository,
        reviewQueueRepository,
        metadataValueRepository,
        aiConversationRepository,
        embeddingChunkRepository,
        jdbcTemplate,
        new PiiMaskingService(),
        auditService);

    when(reviewQueueRepository.findAll()).thenReturn(List.of());
    when(aiConversationRepository.findByClientIdOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    when(noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    when(embeddingChunkRepository.findByClientIdAndSourceTypeAndSourceIdOrderByChunkIndexAsc(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
  }

  @Test
  void timelineReturnsKnowledgeEventsOrderedByOccurrenceAndKeepsBusinessReferencesAsMetadata() {
    UUID clientId = UUID.randomUUID();
    UUID documentId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    UUID emailId = UUID.randomUUID();

    Client client = new Client();
    client.setId(clientId);
    when(clientService.requireClient(clientId)).thenReturn(client);

    Document document = new Document();
    document.setId(documentId);
    document.setClient(client);
    document.setTitle("Policy Schedule");
    document.setSource(DocumentSource.MANUAL_UPLOAD);
    document.setReviewStatus(DocumentReviewStatus.APPROVED);
    document.setCreatedAt(Instant.parse("2026-07-10T10:00:00Z"));

    DocumentVersion version = new DocumentVersion();
    version.setId(versionId);
    version.setDocument(document);
    version.setVersionNumber(1);
    version.setFileHash("hash-1");
    version.setCurrent(true);
    version.setCreatedAt(Instant.parse("2026-07-10T10:00:00Z"));

    Email email = new Email();
    email.setId(emailId);
    email.setClient(client);
    email.setSubject("Renewal reminder");
    email.setSender("carrier@example.com");
    email.setRecipients("broker@example.com");
    email.setProcessingStatus(EmailProcessingStatus.LINKED);
    email.setReviewStatus(EmailReviewStatus.APPROVED);
    email.setReceivedAt(Instant.parse("2026-07-11T09:00:00Z"));
    email.setCreatedAt(Instant.parse("2026-07-11T09:00:00Z"));

    when(documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)).thenReturn(List.of(document));
    when(documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)).thenReturn(Optional.of(version));
    when(documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(documentId)).thenReturn(List.of(version));
    when(emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId)).thenReturn(List.of(email));

    MetadataField field = new MetadataField();
    field.setFieldKey("policy_number");
    field.setLabel("Policy Number");
    MetadataValue value = new MetadataValue();
    value.setId(UUID.randomUUID());
    value.setField(field);
    value.setTextValue("POL-12345");
    value.setCreatedAt(Instant.parse("2026-07-10T10:05:00Z"));
    value.setUpdatedAt(Instant.parse("2026-07-10T10:05:00Z"));
    when(metadataValueRepository.findByOwnerTypeAndOwnerId("DOCUMENT", documentId)).thenReturn(List.of(value));
    when(metadataValueRepository.findByOwnerTypeAndOwnerId("EMAIL", emailId)).thenReturn(List.of());

    ClientContracts.CustomerKnowledgeTimelinePageResponse response = service.timeline(
        clientId,
        new ClientKnowledgeService.TimelineQuery(null, 20, null, null, null, null, null, null, null, "POL-12345", null, null, null),
        UUID.randomUUID(),
        Set.of());

    assertThat(response.events()).hasSize(2);
    assertThat(response.events())
        .extracting(ClientContracts.CustomerKnowledgeTimelineEventResponse::eventType)
        .containsExactly("BUSINESS_REFERENCE_EXTRACTED", "DOCUMENT_CREATED");
    assertThat(response.events().get(0).businessReferenceFields())
        .extracting(ClientContracts.BusinessReferenceFieldResponse::key)
        .containsExactly("policy_number");
    assertThat(response.events().get(1).title()).doesNotContain("Policy Record");
  }

  @Test
  void relatedKnowledgeReturnsDeterministicEmailAttachmentLinksWithoutIntroducingPolicyEntities() {
    UUID clientId = UUID.randomUUID();
    UUID documentId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    UUID emailId = UUID.randomUUID();

    Client client = new Client();
    client.setId(clientId);
    when(clientService.requireClient(clientId)).thenReturn(client);

    Email email = new Email();
    email.setId(emailId);
    email.setClient(client);
    email.setSubject("Renewal reminder");
    email.setReceivedAt(Instant.parse("2026-07-11T09:00:00Z"));
    email.setCreatedAt(Instant.parse("2026-07-11T09:00:00Z"));

    Document document = new Document();
    document.setId(documentId);
    document.setClient(client);
    document.setParentEmail(email);
    document.setTitle("Policy Schedule");
    document.setSource(DocumentSource.EMAIL_ATTACHMENT);
    document.setCreatedAt(Instant.parse("2026-07-11T09:05:00Z"));

    DocumentVersion version = new DocumentVersion();
    version.setId(versionId);
    version.setDocument(document);
    version.setVersionNumber(1);
    version.setFileHash("hash-attachment");
    version.setCurrent(true);
    version.setCreatedAt(Instant.parse("2026-07-11T09:05:00Z"));

    when(documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)).thenReturn(List.of(document));
    when(documentVersionRepository.findByDocument_IdAndCurrentTrue(documentId)).thenReturn(Optional.of(version));
    when(documentVersionRepository.findByDocument_IdOrderByVersionNumberDesc(documentId)).thenReturn(List.of(version));
    when(emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId)).thenReturn(List.of(email));
    when(metadataValueRepository.findByOwnerTypeAndOwnerId("DOCUMENT", documentId)).thenReturn(List.of());
    when(metadataValueRepository.findByOwnerTypeAndOwnerId("EMAIL", emailId)).thenReturn(List.of());

    ClientContracts.RelatedKnowledgeResponse response = service.relatedForClient(clientId, 10, UUID.randomUUID(), Set.of());

    assertThat(response.links()).hasSize(1);
    ClientContracts.RelatedKnowledgeLinkResponse link = response.links().get(0);
    assertThat(link.relationshipType()).isEqualTo("EMAIL_ATTACHMENT");
    assertThat(link.sourceType()).isEqualTo("EMAIL");
    assertThat(link.relatedSourceType()).isEqualTo("DOCUMENT");
    assertThat(link.explanation()).contains("related knowledge");
    assertThat(link.relationshipType()).doesNotContain("POLICY");
    assertThat(link.relationshipType()).doesNotContain("CLAIM");
  }
}
