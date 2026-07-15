package com.ikms.search;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class SearchEvidenceCandidate {

  private final String sourceType;
  private final UUID sourceId;
  private String title;
  private String excerpt;
  private String fallbackText;
  private String citation;
  private Integer pageNumber;
  private String sourceSection;
  private Instant occurredAt;
  private boolean containsPii;
  private double lexicalScore;
  private double vectorScore;
  private double metadataScore;
  private double relationshipScore;
  private final Set<String> retrievalSignals = new LinkedHashSet<>();

  public SearchEvidenceCandidate(String sourceType, UUID sourceId) {
    this.sourceType = sourceType;
    this.sourceId = sourceId;
  }

  public String key() {
    return sourceType + ":" + sourceId;
  }

  public double finalScore() {
    double base = lexicalScore + (vectorScore * 1.25d) + (metadataScore * 0.95d) + (relationshipScore * 0.85d);
    if (occurredAt != null) {
      base += 0.000000001d * occurredAt.toEpochMilli();
    }
    return base;
  }

  public String retrievalPath() {
    if (vectorScore > 0 && (lexicalScore > 0 || metadataScore > 0 || relationshipScore > 0)) {
      return "HYBRID_VECTOR";
    }
    if (vectorScore > 0) {
      return "VECTOR_HYBRID";
    }
    if (metadataScore > 0 && relationshipScore > 0) {
      return "RELATIONSHIP_METADATA";
    }
    if (metadataScore > 0) {
      return "METADATA";
    }
    if (relationshipScore > 0) {
      return "RELATIONSHIP";
    }
    return lexicalScore > 0 ? "KEYWORD_FALLBACK" : "BROWSE";
  }

  public String sourceType() { return sourceType; }
  public UUID sourceId() { return sourceId; }
  public String title() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String excerpt() { return excerpt; }
  public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
  public String fallbackText() { return fallbackText; }
  public void setFallbackText(String fallbackText) { this.fallbackText = fallbackText; }
  public String citation() { return citation; }
  public void setCitation(String citation) { this.citation = citation; }
  public Integer pageNumber() { return pageNumber; }
  public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
  public String sourceSection() { return sourceSection; }
  public void setSourceSection(String sourceSection) { this.sourceSection = sourceSection; }
  public Instant occurredAt() { return occurredAt; }
  public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
  public boolean containsPii() { return containsPii; }
  public void setContainsPii(boolean containsPii) { this.containsPii = containsPii; }
  public double lexicalScore() { return lexicalScore; }
  public void addLexicalScore(double lexicalScore) { this.lexicalScore += lexicalScore; }
  public double vectorScore() { return vectorScore; }
  public void addVectorScore(double vectorScore) { this.vectorScore += vectorScore; }
  public double metadataScore() { return metadataScore; }
  public void addMetadataScore(double metadataScore) { this.metadataScore += metadataScore; }
  public double relationshipScore() { return relationshipScore; }
  public void addRelationshipScore(double relationshipScore) { this.relationshipScore += relationshipScore; }
  public Set<String> retrievalSignals() { return retrievalSignals; }
}
