package com.ikms.email;

import com.ikms.client.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_item")
public class Email {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id")
  private Client client;

  @Column(name = "mailbox_config_id")
  private UUID mailboxConfigId;

  @Column(name = "message_id", nullable = false, unique = true, length = 255)
  private String messageId;

  @Column(name = "thread_id", length = 255)
  private String threadId;

  @Column(nullable = false, length = 255)
  private String subject;

  @Column(nullable = false, length = 255)
  private String sender;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String recipients;

  @Column(columnDefinition = "TEXT")
  private String cc;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "body_text", columnDefinition = "TEXT")
  private String bodyText;

  @Column(name = "body_html_storage_path", length = 512)
  private String bodyHtmlStoragePath;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 32)
  private EmailProcessingStatus processingStatus = EmailProcessingStatus.INTAKE_RECEIVED;

  @Enumerated(EnumType.STRING)
  @Column(name = "review_status", nullable = false, length = 32)
  private EmailReviewStatus reviewStatus = EmailReviewStatus.PENDING_REVIEW;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Client getClient() { return client; }
  public void setClient(Client client) { this.client = client; }
  public UUID getMailboxConfigId() { return mailboxConfigId; }
  public void setMailboxConfigId(UUID mailboxConfigId) { this.mailboxConfigId = mailboxConfigId; }
  public String getMessageId() { return messageId; }
  public void setMessageId(String messageId) { this.messageId = messageId; }
  public String getThreadId() { return threadId; }
  public void setThreadId(String threadId) { this.threadId = threadId; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getSender() { return sender; }
  public void setSender(String sender) { this.sender = sender; }
  public String getRecipients() { return recipients; }
  public void setRecipients(String recipients) { this.recipients = recipients; }
  public String getCc() { return cc; }
  public void setCc(String cc) { this.cc = cc; }
  public Instant getReceivedAt() { return receivedAt; }
  public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
  public String getBodyText() { return bodyText; }
  public void setBodyText(String bodyText) { this.bodyText = bodyText; }
  public String getBodyHtmlStoragePath() { return bodyHtmlStoragePath; }
  public void setBodyHtmlStoragePath(String bodyHtmlStoragePath) { this.bodyHtmlStoragePath = bodyHtmlStoragePath; }
  public EmailProcessingStatus getProcessingStatus() { return processingStatus; }
  public void setProcessingStatus(EmailProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
  public EmailReviewStatus getReviewStatus() { return reviewStatus; }
  public void setReviewStatus(EmailReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
