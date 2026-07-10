package com.ikms.email;

import com.ikms.ai.ClassificationService;
import com.ikms.client.ClientService;
import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentUploadService;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.review.ReviewQueueReason;
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
  private final TextExtractionService textExtractionService;
  private final ClassificationService classificationService;
  private final ReviewQueueService reviewQueueService;
  private final ClientService clientService;

  public EmailAttachmentService(
      EmailRepository emailRepository,
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      DocumentUploadService documentUploadService,
      TextExtractionService textExtractionService,
      ClassificationService classificationService,
      ReviewQueueService reviewQueueService,
      ClientService clientService) {
    this.emailRepository = emailRepository;
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.documentUploadService = documentUploadService;
    this.textExtractionService = textExtractionService;
    this.classificationService = classificationService;
    this.reviewQueueService = reviewQueueService;
    this.clientService = clientService;
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

    List<DocumentUploadService.UploadResult> attachments = new ArrayList<>();
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
        var extraction = textExtractionService.extract(new TextExtractionService.ExtractionRequest(
            attachment.filename(),
            attachment.mimeType(),
            attachment.fileBytes(),
            "mistral-placeholder"));
        version.setExtractedText(extraction.extractedText());
        version.setLanguage(extraction.language());
        version.setOcrProvider(extraction.provider());
        documentVersionRepository.save(version);

        var classification = classificationService.classify(new ClassificationService.ClassificationRequest(
            command.clientId(),
            attachment.filename(),
            attachment.mimeType(),
            extraction.extractedText(),
            extraction.language()));
        document.setTitle(classification.suggestedTitle());
        document.setClientMatchConfidence(classification.clientMatchConfidence());
        document.setClassificationConfidence(classification.classificationConfidence());
        document.setExtractionConfidence(classification.extractionConfidence());
        documentRepository.save(document);

        if (command.clientId() == null) {
          reviewQueueService.createForDocument(document.getId(), ReviewQueueReason.UNLINKED);
        }
      }
    }

    if (command.clientId() == null) {
      reviewQueueService.createForEmail(savedEmail.getId(), ReviewQueueReason.UNLINKED);
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
