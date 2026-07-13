package com.ikms.email;

import com.ikms.ai.ClassificationService;
import com.ikms.ai.EmbeddingIndexService;
import com.ikms.ai.PromptInjectionDetectionService;
import com.ikms.client.ClientService;
import com.ikms.document.Document;
import com.ikms.document.DocumentIntakeProcessingService;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentUploadService;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.review.ReviewRoutingService;
import com.ikms.review.ReviewQueueService;
import com.ikms.worker.extract.TextExtractionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailAttachmentService {

  private final EmailRepository emailRepository;
  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final DocumentUploadService documentUploadService;
  private final DocumentIntakeProcessingService documentIntakeProcessingService;
  private final EmbeddingIndexService embeddingIndexService;
  private final ReviewRoutingService reviewRoutingService;
  private final ReviewQueueService reviewQueueService;
  private final ClientService clientService;
  private final PromptInjectionDetectionService promptInjectionDetectionService;

  public EmailAttachmentService(
      EmailRepository emailRepository,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentUploadService documentUploadService,
      DocumentIntakeProcessingService documentIntakeProcessingService,
      EmbeddingIndexService embeddingIndexService,
      ReviewRoutingService reviewRoutingService,
      ReviewQueueService reviewQueueService,
      ClientService clientService,
      PromptInjectionDetectionService promptInjectionDetectionService) {
    this.emailRepository = emailRepository;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentUploadService = documentUploadService;
    this.documentIntakeProcessingService = documentIntakeProcessingService;
    this.embeddingIndexService = embeddingIndexService;
    this.reviewRoutingService = reviewRoutingService;
    this.reviewQueueService = reviewQueueService;
    this.clientService = clientService;
    this.promptInjectionDetectionService = promptInjectionDetectionService;
  }

  public EmailIntakeResult ingestEmail(EmailIntakeCommand command) {
    Email email = new Email();
    email.setClient(command.clientId() == null ? null : clientService.requireClient(command.clientId()));
    email.setMailboxConfigId(command.mailboxConfigId());
    email.setMessageId(command.messageId());
    email.setThreadId(command.threadId());
    email.setSubject(command.subject());
    email.setSender(command.sender());
    email.setRecipients(String.join(", ", command.recipients() == null ? List.of() : command.recipients()));
    email.setCc(String.join(", ", command.cc() == null ? List.of() : command.cc()));
    email.setReceivedAt(command.receivedAt() == null ? Instant.now() : command.receivedAt());
    email.setBodyText(command.bodyText());
    email.setProcessingStatus(command.clientId() == null ? EmailProcessingStatus.PENDING_REVIEW : EmailProcessingStatus.LINKED);
    email.setReviewStatus(command.clientId() == null ? EmailReviewStatus.PENDING_REVIEW : EmailReviewStatus.NOT_REQUIRED);
    Email savedEmail = emailRepository.save(email);
    if (savedEmail.getClient() != null) {
      embeddingIndexService.indexEmail(savedEmail.getClient().getId(), savedEmail);
    }
    boolean emailPromptRisk = promptInjectionDetectionService.inspect(command.bodyText()).detected();
    if (emailPromptRisk) {
      savedEmail.setProcessingStatus(EmailProcessingStatus.PENDING_REVIEW);
      savedEmail.setReviewStatus(EmailReviewStatus.PENDING_REVIEW);
      emailRepository.save(savedEmail);
      reviewQueueService.createForEmail(savedEmail.getId(), com.ikms.review.ReviewQueueReason.PROMPT_INJECTION_RISK);
    }

    List<DocumentUploadService.UploadResult> attachments = new ArrayList<>();
    boolean attachmentNeedsReview = false;
    for (AttachmentCommand attachment : command.attachments()) {
      DocumentUploadService.UploadResult uploadResult = documentUploadService.upload(new DocumentUploadService.UploadCommand(
          command.clientId(),
          command.actorUserId(),
          attachment.filename(),
          attachment.mimeType(),
          attachment.fileHash(),
          attachment.fileBytes()));
      attachments.add(uploadResult);

      if (uploadResult.documentId() != null) {
        Document document = documentRepository.findById(uploadResult.documentId())
            .orElseThrow(() -> new IllegalStateException("Uploaded document not found: " + uploadResult.documentId()));
        document.setParentEmail(savedEmail);

        DocumentVersion version = documentVersionRepository.findById(uploadResult.versionId())
            .orElseThrow(() -> new IllegalStateException("Uploaded version not found: " + uploadResult.versionId()));
        documentIntakeProcessingService.process(document, version, command.clientId(), attachment.fileBytes());
        if (document.getReviewStatus() == com.ikms.document.DocumentReviewStatus.PENDING_REVIEW) {
          attachmentNeedsReview = true;
        }
      }
    }

    var emailDecision = reviewRoutingService.emailDecision(command.clientId(), savedEmail.getId(), attachmentNeedsReview);
    if (emailDecision.requiresReview()) {
      savedEmail.setProcessingStatus(EmailProcessingStatus.PENDING_REVIEW);
      savedEmail.setReviewStatus(EmailReviewStatus.PENDING_REVIEW);
      emailRepository.save(savedEmail);
      reviewQueueService.createForEmail(savedEmail.getId(), emailDecision.reason());
    }

    return new EmailIntakeResult(savedEmail.getId(), attachments);
  }

  public record EmailIntakeCommand(
      UUID clientId,
      UUID actorUserId,
      UUID mailboxConfigId,
      String messageId,
      String threadId,
      String subject,
      String sender,
      List<String> recipients,
      List<String> cc,
      String bodyText,
      Instant receivedAt,
      List<AttachmentCommand> attachments) {
  }

  public record AttachmentCommand(
      String filename,
      String mimeType,
      String fileHash,
      byte[] fileBytes) {
  }

  public record EmailIntakeResult(
      UUID emailId,
      List<DocumentUploadService.UploadResult> attachmentResults) {
  }
}
