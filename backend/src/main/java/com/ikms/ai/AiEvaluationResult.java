package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_evaluation_result")
public class AiEvaluationResult {

  @Id
  private UUID id;

  @Column(name = "interaction_id")
  private UUID interactionId;

  @Column(name = "evaluation_type", nullable = false, length = 64)
  private String evaluationType;

  @Column
  private Double score;

  @Column(length = 32)
  private String outcome;

  @Column(columnDefinition = "TEXT")
  private String notes;

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
  public UUID getInteractionId() { return interactionId; }
  public void setInteractionId(UUID interactionId) { this.interactionId = interactionId; }
  public String getEvaluationType() { return evaluationType; }
  public void setEvaluationType(String evaluationType) { this.evaluationType = evaluationType; }
  public Double getScore() { return score; }
  public void setScore(Double score) { this.score = score; }
  public String getOutcome() { return outcome; }
  public void setOutcome(String outcome) { this.outcome = outcome; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
