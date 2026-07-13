package com.ikms.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikms.ai.EmbeddingChunkRepository;
import com.ikms.audit.AuditService;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.Email;
import com.ikms.email.EmailRepository;
import com.ikms.note.Note;
import com.ikms.note.NoteRepository;
import com.ikms.retention.RetentionWorkflowService.RetentionAction;
import com.ikms.retention.RetentionWorkflowService.RetentionDecision;
import com.ikms.retention.RetentionWorkflowService.RetentionRequest;
import com.ikms.storage.FileStorageService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RetentionWorkflowTest {

  private AuditService auditService;
  private RetentionRecordRepository retentionRecordRepository;
  private DocumentRepository documentRepository;
  private DocumentVersionRepository documentVersionRepository;
  private EmailRepository emailRepository;
  private NoteRepository noteRepository;
  private MetadataValueRepository metadataValueRepository;
  private EmbeddingChunkRepository embeddingChunkRepository;
  private FileStorageService fileStorageService;
  private RetentionWorkflowService retentionWorkflowService;

  @BeforeEach
  void setUp() {
    auditService = mock(AuditService.class);
    retentionRecordRepository = mock(RetentionRecordRepository.class);
    documentRepository = mock(DocumentRepository.class);
    documentVersionRepository = mock(DocumentVersionRepository.class);
    emailRepository = mock(EmailRepository.class);
    noteRepository = mock(NoteRepository.class);
    metadataValueRepository = mock(MetadataValueRepository.class);
    embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
    fileStorageService = mock(FileStorageService.class);
    when(retentionRecordRepository.findByTargetTypeAndTargetId(any(), any())).thenReturn(Optional.empty());
    retentionWorkflowService = new RetentionWorkflowService(
        auditService,
        retentionRecordRepository,
        documentRepository,
        documentVersionRepository,
        emailRepository,
        noteRepository,
        metadataValueRepository,
        embeddingChunkRepository,
        fileStorageService);
  }

  @Test
  void deleteShouldBeDeniedWhenLegalHoldIsActive() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.DELETE,
        true,
        Instant.now().minus(2, ChronoUnit.DAYS)));

    assertThat(decision.approved()).isFalse();
    assertThat(decision.reason()).contains("Legal hold");
    verify(auditService).write(any());
  }

  @Test
  void anonymizeShouldBeDeniedBeforeMinimumRetentionHasElapsed() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.ANONYMIZE,
        false,
        Instant.now().plus(10, ChronoUnit.DAYS)));

    assertThat(decision.approved()).isFalse();
    assertThat(decision.reason()).contains("Minimum retention period");
    verify(auditService).write(any());
  }

  @Test
  void applyLegalHoldShouldBeAccepted() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.APPLY_LEGAL_HOLD,
        false,
        null));

    assertThat(decision.approved()).isTrue();
    verify(auditService).write(any());
    verify(retentionRecordRepository).save(any());
  }

  @Test
  void anonymizeShouldUpdateEmailContent() {
    UUID emailId = UUID.randomUUID();
    Email email = new Email();
    email.setId(emailId);
    email.setSubject("Client renewal");
    email.setSender("broker@example.com");
    email.setRecipients("client@example.com");
    email.setBodyText("Sensitive email body");
    when(emailRepository.findById(emailId)).thenReturn(Optional.of(email));

    RetentionDecision decision = retentionWorkflowService.evaluate(new RetentionRequest(
        "EMAIL",
        emailId.toString(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        RetentionAction.ANONYMIZE,
        false,
        Instant.now().minus(1, ChronoUnit.DAYS),
        "Customer request"));

    assertThat(decision.approved()).isTrue();
    verify(emailRepository).save(email);
    assertThat(email.getSubject()).isEqualTo("Anonymized email");
    assertThat(email.getBodyText()).contains("Anonymized");
  }

  @Test
  void deleteShouldRemoveNote() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note();
    note.setId(noteId);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

    RetentionDecision decision = retentionWorkflowService.evaluate(new RetentionRequest(
        "NOTE",
        noteId.toString(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        RetentionAction.DELETE,
        false,
        Instant.now().minus(1, ChronoUnit.DAYS),
        "Retention expiry"));

    assertThat(decision.approved()).isTrue();
    verify(noteRepository).delete(note);
    verify(fileStorageService, never()).delete(any());
  }

  @Test
  void releaseLegalHoldShouldBeDeniedWhenNoHoldRecordExists() {
    RetentionDecision decision = retentionWorkflowService.evaluate(request(
        RetentionAction.RELEASE_LEGAL_HOLD,
        false,
        null));

    assertThat(decision.approved()).isFalse();
    verify(retentionRecordRepository).save(any());
  }

  private RetentionRequest request(RetentionAction action, boolean legalHold, Instant minimumRetentionUntil) {
    return new RetentionRequest(
        "Document",
        "doc-123",
        UUID.randomUUID(),
        UUID.randomUUID(),
        action,
        legalHold,
        minimumRetentionUntil,
        "Retention workflow test");
  }
}
