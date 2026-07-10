package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_interaction")
public class AiInteraction {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String question;

  @Column(columnDefinition = "TEXT")
  private String answer;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "cited_sources", columnDefinition = "TEXT")
  private String citedSources;

  @Column(name = "helpful_feedback")
  private Boolean helpfulFeedback;

  @Column(name = "feedback_comment", columnDefinition = "TEXT")
  private String feedbackComment;

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
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getQuestion() { return question; }
  public void setQuestion(String question) { this.question = question; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getCitedSources() { return citedSources; }
  public void setCitedSources(String citedSources) { this.citedSources = citedSources; }
  public Boolean getHelpfulFeedback() { return helpfulFeedback; }
  public void setHelpfulFeedback(Boolean helpfulFeedback) { this.helpfulFeedback = helpfulFeedback; }
  public String getFeedbackComment() { return feedbackComment; }
  public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
