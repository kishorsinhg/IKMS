package com.ikms.worker.intake;

import com.ikms.email.EmailAttachmentService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ImapIntakeWorker {

  private final EmailAttachmentService emailAttachmentService;

  public ImapIntakeWorker(EmailAttachmentService emailAttachmentService) {
    this.emailAttachmentService = emailAttachmentService;
  }

  public EmailAttachmentService.EmailIntakeResult processMessage(ImapMessage message) {
    return emailAttachmentService.ingestEmail(new EmailAttachmentService.EmailIntakeCommand(
        message.clientId(),
        message.actorUserId(),
        message.mailboxConfigId(),
        message.messageId(),
        message.threadId(),
        message.subject(),
        message.sender(),
        message.recipients(),
        message.cc(),
        message.bodyText(),
        message.receivedAt() == null ? Instant.now() : message.receivedAt(),
        message.attachments().stream()
            .map(attachment -> new EmailAttachmentService.AttachmentCommand(
                attachment.filename(),
                attachment.mimeType(),
                attachment.fileHash(),
                attachment.fileBytes()))
            .toList()));
  }

  public record ImapMessage(
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
      List<ImapAttachment> attachments) {
  }

  public record ImapAttachment(
      String filename,
      String mimeType,
      String fileHash,
      byte[] fileBytes) {
  }
}
